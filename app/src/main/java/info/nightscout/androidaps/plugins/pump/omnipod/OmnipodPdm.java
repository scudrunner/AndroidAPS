package info.nightscout.androidaps.plugins.pump.omnipod;

import android.content.Context;

import com.squareup.otto.Subscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.data.PumpEnactResult;
import info.nightscout.androidaps.events.EventPumpStatusChanged;
import info.nightscout.androidaps.plugins.general.overview.events.EventDismissNotification;
import info.nightscout.androidaps.plugins.general.overview.events.EventNewNotification;
import info.nightscout.androidaps.plugins.general.overview.notifications.Notification;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyCallback;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipyApiResult;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.pump.omnipod.api.OmnipyRestApi;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipodUpdateGui;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipyConfigurationComplete;
import info.nightscout.androidaps.utils.SP;
import info.nightscout.androidaps.plugins.pump.omnipod.api.rest.OmnipyResult;

public class OmnipodPdm {

    private final Context _context;

    //private Profile _profile;

    private OmnipyRestApi _restApi;
    private OmnipyResult _lastResult;
    private OmnipodStatus _lastStatus;

    private boolean _initialized;
    private boolean _suspended;

    private final Timer _pingTimer;

    private final Logger _log;

    public OmnipodPdm(Context context)
    {
        _context = context;
        _log =  LoggerFactory.getLogger(L.PUMP);
        _pingTimer = new Timer(true);
    }

    public void OnStart() {
        _restApi = new OmnipyRestApi(_context);
        MainApp.bus().register(this);
        MainApp.bus().register(_restApi);
        //_profile = ProfileFunctions.getInstance().getProfile();
        _lastStatus = OmnipodStatus.fromJson(SP.getString(R.string.key_omnipod_status, null));
        _initialized = false;
        _restApi.StartConfiguring();
    }

    public void OnStop() {
        MainApp.bus().unregister(_restApi);
        MainApp.bus().unregister(this);
        _restApi.StopConfiguring();
        _restApi = null;
    }

    private long _lastPing = 0;
    private void pingOmnipy()
    {
        long t0 = System.currentTimeMillis();
        if (t0 - _lastPing > 5000) {
            _lastPing = t0;
            _restApi.Ping(result -> {
                if (!result.canceled && !result.success) {
                    handleDisconnect();
                }
            });
        }
    }

    private void handleDisconnect()
    {
        _initialized = false;
        _pingTimer.cancel();
        MainApp.bus().post(new Notification(Notification.PUMP_UNREACHABLE, "Omnipy disconnected", Notification.NORMAL));
        MainApp.bus().post(new EventPumpStatusChanged(EventPumpStatusChanged.DISCONNECTING));
        MainApp.bus().post(new EventPumpStatusChanged(EventPumpStatusChanged.DISCONNECTED));
        _restApi.StartConfiguring();
    }

    public boolean IsInitialized() { return _initialized; }

    public boolean IsSuspended() {
        return _suspended;
    }

    public boolean IsBusy() {
        _lastResult = _restApi.IsBusy();

        if (_lastResult.success)
        {
            return _lastResult.response.get("busy").getAsBoolean();
        }
        else
            return true;
    }

    public boolean IsConnected() {
        if (_restApi.isConfigured() && _restApi.isConnectable()) {
            pingOmnipy();
            return true;
        }
        return false;
    }

    public void Connect() {
        if (_restApi.isConfigured())
        {
            if (_restApi.isConnectable())
            {
                pingOmnipy();
            }
        }
        else {
            _restApi.StartConfiguring();
        }
    }

    public boolean IsConnecting() {
        return _restApi.isConfiguring();
    }

    public void StopConnecting() {
        _restApi.StopConfiguring();
    }

    public void FinishHandshaking() { }

    public boolean IsHandshakeInProgress() {
        return false;
    }

    public void Disconnect() {}


    @Subscribe
    public synchronized void onResultReceived(final EventOmnipyApiResult or) {
        OmnipyResult result = or.getResult();
        if (!result.canceled) {
            _lastResult = result;
            if (_lastResult.status != null) {
                _lastStatus = result.status;
                SP.putString(R.string.key_omnipod_status, result.status.asJson());
                MainApp.bus().post(new EventOmnipodUpdateGui());
            }
        }
    }

    @Subscribe
    public void onConfigurationComplete(final EventOmnipyConfigurationComplete confResult) {
        MainApp.bus().post(new EventOmnipodUpdateGui());
        String errorMessage;
        if (!confResult.isConnectable)
        {
            _initialized = false;
            if (confResult.isDiscovered)
            {
                errorMessage = "Omnipy located at network address " + confResult.hostName +
                        " but connection cannot be established";
            }
            else
            {
                errorMessage = "Omnipy connection cannot be established at the configured address: " + confResult.hostName
                            + " Please verify the address in configuration.";
            }
            Notification notification = new Notification(Notification.PUMP_UNREACHABLE, errorMessage, Notification.NORMAL);
            MainApp.bus().post(new EventNewNotification(notification));
            MainApp.bus().post(new EventPumpStatusChanged(EventPumpStatusChanged.DISCONNECTING));
            MainApp.bus().post(new EventPumpStatusChanged(EventPumpStatusChanged.DISCONNECTED));
        }
        else if (!confResult.isAuthenticated)
        {
            _initialized = false;
            errorMessage = "Omnipy connection established at address " + confResult.hostName +
                    " but authentication failed. Please verify your password in settings.";
            Notification notification = new Notification(Notification.PUMP_UNREACHABLE, errorMessage, Notification.NORMAL);
            MainApp.bus().post(new EventNewNotification(notification));
            MainApp.bus().post(new EventPumpStatusChanged(EventPumpStatusChanged.DISCONNECTING));
            MainApp.bus().post(new EventPumpStatusChanged(EventPumpStatusChanged.DISCONNECTED));

        }
        else
        {
            _initialized = true;

            MainApp.bus().post(new EventDismissNotification(Notification.PUMP_UNREACHABLE));
            Notification notification = new Notification(100, "Connected to omnipy running at " + confResult.hostName, Notification.INFO, 1);
            MainApp.bus().post(new EventNewNotification(notification));
            MainApp.bus().post(new EventPumpStatusChanged(
                    EventPumpStatusChanged.CONNECTED));

            UpdateStatus();

            _pingTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    pingOmnipy();
                }
            }, 30000, 30000);
        }

    }

    private long _lastStatusRequest = 0;
    public void UpdateStatus() {
        long t0 = System.currentTimeMillis();
        if (t0 - _lastStatusRequest > 10000) {
            _lastStatusRequest = t0;
            _restApi.UpdateStatus(null);
        }
    }

    public long GetLastUpdated() {
        if (_lastStatus != null)
            return  (long)_lastStatus.state_last_updated * 1000;
        else
            return 0;
    }

    public String getPodStatusText()
    {
        if (_lastStatus == null)
            return "Status unknown";

        StringBuilder sb = new StringBuilder();
        sb.append("Lot: ").append(_lastStatus.id_lot).append(" TID: ").append(_lastStatus.id_t)
                .append(" Radio address: ")
                .append(String.format("%08X", _lastStatus.radio_address));

        sb.append("\n\nStatus: ");
        if (_lastStatus.state_progress < 8)
        {
            sb.append("Not yet running");
        }
        else if (_lastStatus.state_progress == 8)
        {
            sb.append("Running");
        }
        else if (_lastStatus.state_progress == 9)
        {
            sb.append("Running with low insulin (<50U)");
        }
        else
        {
            sb.append("Inactive");
        }

        if (_lastStatus.state_faulted)
        {
            sb.append("\nPOD FAULTED");
        }

        int minutes = _lastStatus.state_active_minutes;
        int days = minutes / 60 / 24;
        minutes -= days * 60 * 24;
        int hours = minutes / 60;
        minutes -= hours * 60;

        sb.append("\n\nTotal insulin delivered: ").append(String.format("%3.2f",
                _lastStatus.insulin_delivered)).append("U");
        sb.append("\nReservoir: ");
        if (_lastStatus.insulin_reservoir > 50)
            sb.append("more than 50U");
        else
            sb.append(String.format("%2.2f", _lastStatus.insulin_reservoir)).append("U");

        sb.append("\n\nPod age: ").append(String.format("%dd%dh%dm", days, hours, minutes));

        Date date = new Date((long)_lastStatus.state_last_updated * 1000);
        DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(_context);
        sb.append("\n\nStatus updated: ").append(dateFormat.format(date));

        return sb.toString();
    }

    public String getConnectionStatusText()
    {
        StringBuilder sb = new StringBuilder();
        if (!_restApi.isConfigured())
        {
            sb.append("Address configuration in progress..");
        }
        else
        {
            if (_restApi.isDiscovered())
                sb.append("Discovered Address: ").append(_restApi.getHost());
            else
                sb.append("Configured Address: ").append(_restApi.getHost());

            if (!_restApi.isConnectable())
                sb.append("\nConnectable: No");
            else
            {
                sb.append("\nConnectable: Yes");
                if (_restApi.isAuthenticated())
                    sb.append("\nAuthentication: Verified");
                else
                    sb.append("\nAuthentication: Failed");
            }
        }

        sb.append("\n\nLast successful connection: ");
        long tx = _restApi.getLastSuccessfulConnection();
        if (tx == 0)
        {
            sb.append("N/A");
        }
        else
        {
            Date date = new Date(tx);
            DateFormat dateFormat = android.text.format.DateFormat.getTimeFormat(_context);
            sb.append(dateFormat.format(date));
        }

        return sb.toString();
    }

    public PumpEnactResult SetNewBasalProfile(Profile profile) {
        PumpEnactResult r = new PumpEnactResult();
        r.enacted = false;
        r.success = false;
        if (_initialized) {
            BigDecimal[] basalSchedule = getBasalScheduleFromProfile(profile);
            OmnipyResult result = _restApi.setBasalSchedule(basalSchedule, null).waitForResult();
            r.enacted = result.success;
            r.success = result.success;
            if (result.success) {
                Notification notification = new Notification(Notification.PROFILE_SET_OK, MainApp.gs(R.string.profile_set_ok), Notification.INFO, 60);
                MainApp.bus().post(new EventNewNotification(notification));
            } else {
                Notification notification = new Notification(Notification.PROFILE_SET_FAILED, MainApp.gs(R.string.profile_set_ok), Notification.NORMAL, 60);
                MainApp.bus().post(new EventNewNotification(notification));
            }
        }
        return r;
    }

    public boolean IsProfileSet(Profile profile) {
        if (!_initialized) {
            return false;
        }

        if (_lastStatus == null)
        {
            OmnipyResult result = _restApi.UpdateStatus(null).waitForResult();
            if (result.success && result.status.var_basal_schedule != null
                    && result.status.var_basal_schedule.length != 0)
            {
                return verifySchedule(result.status.var_basal_schedule, profile);
            }

            return false;
        }

        if (_lastStatus.var_basal_schedule == null || _lastStatus.var_basal_schedule.length == 0)
        {
            return false;
        }

        return verifySchedule(_lastStatus.var_basal_schedule, profile);
    }

    private boolean verifySchedule(BigDecimal[] podSchedule, Profile profile)
    {
        BigDecimal[] scheduleToVerify = getBasalScheduleFromProfile(profile);
        for(int i=0; i<48; i++)
            if (!_lastStatus.var_basal_schedule[i].equals(scheduleToVerify[i]))
                return false;
        return true;
    }

    public double GetBaseBasalRate() {
        if (!_initialized || _lastStatus == null || _lastStatus.var_basal_schedule == null
                || _lastStatus.var_basal_schedule.length == 0)
            return -1d;

        long t = System.currentTimeMillis();
        t+= _lastStatus.var_utc_offset;
        Date dt = new Date(t);
        int h = dt.getHours();
        int m = dt.getMinutes();

        int index = h * 2;
        if (m >= 30)
            index++;

        return _lastStatus.var_basal_schedule[index].doubleValue();
    }

    public PumpEnactResult Bolus(DetailedBolusInfo detailedBolusInfo) {
        BigDecimal iuBolus = GetExactInsulinUnits(detailedBolusInfo.insulin);
        OmnipyResult result = _restApi.Bolus(iuBolus, null).waitForResult();
        PumpEnactResult r = new PumpEnactResult();
        r.enacted = result.success;
        r.success = result.success;
        if (result.success)
        {
            r.bolusDelivered = iuBolus.doubleValue();
        }
        return r;
    }

    public void CancelBolus(OmnipyCallback cb) {
        _restApi.CancelBolus(cb);
    }

    public PumpEnactResult SetTempBasal(Double absoluteRate, Integer durationInMinutes, Profile profile, boolean enforceNew) {
        BigDecimal iuRate = GetExactInsulinUnits(absoluteRate);
        BigDecimal durationHours = GetExactHourUnits(durationInMinutes);

        OmnipyResult result = _restApi.SetTempBasal(iuRate, durationHours, null).waitForResult();
        PumpEnactResult r = new PumpEnactResult();
        r.enacted = result.success;
        r.success = result.success;
        if (result.success)
        {
            r.absolute = iuRate.doubleValue();
            r.duration = durationInMinutes;
            r.isPercent = false;
        }
        return r;
    }

    public PumpEnactResult CancelTempBasal(boolean enforceNew) {

        PumpEnactResult r = new PumpEnactResult();

        OmnipyResult result = _restApi.CancelTempBasal(null).waitForResult();
        r.enacted = result.success;
        r.success = result.success;
        if (result.success)
        {
            r.isTempCancel = true;
        }
        return r;
    }

    public String GetPodId() {
        return String.format("L%dT%d", _lastStatus.id_lot, _lastStatus.id_t);
    }

    public String GetStatusShort() {
        if (_lastStatus.state_faulted)
            return "FAULT";
        if (_lastStatus.state_progress == 9)
            return "IU<50";
        if (_lastStatus.state_progress == 8)
            return "OK";
        return "UNKNOWN";
    }

    private BigDecimal GetExactInsulinUnits(double iu)
    {
        BigDecimal big20 = new BigDecimal("20");
        // round to 0.05's complements
        return new BigDecimal(iu).multiply(big20).setScale(0, RoundingMode.HALF_UP).setScale(2).divide(big20);
    }

    private BigDecimal GetExactHourUnits(int minutes)
    {
        BigDecimal big30 = new BigDecimal("30");
        return new BigDecimal(minutes).divide(big30).setScale(0, RoundingMode.HALF_UP).setScale(1).divide(new BigDecimal(2));
    }

    private BigDecimal[] getBasalScheduleFromProfile(Profile profile)
    {
        BigDecimal[] basalSchedule = new BigDecimal[48];

        long secondsSinceMidnight = 0;
        for(int i=0; i<48; i++)
        {
            basalSchedule[i] = GetExactInsulinUnits(profile.getBasal(secondsSinceMidnight));
            secondsSinceMidnight += 60*30;
        }
        return basalSchedule;
    }

    public double GetReservoirLevel() {
        if (_lastStatus != null)
            return _lastStatus.insulin_reservoir;
        else
            return -1;
    }
}
