package info.nightscout.androidaps.plugins.pump.omnipod;

import android.content.Context;
import android.os.SystemClock;

import com.squareup.otto.Subscribe;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.BuildConfig;
import info.nightscout.androidaps.Config;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.data.PumpEnactResult;
import info.nightscout.androidaps.db.ExtendedBolus;
import info.nightscout.androidaps.db.Source;
import info.nightscout.androidaps.db.TemporaryBasal;
import info.nightscout.androidaps.events.EventNetworkChange;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.interfaces.PluginDescription;
import info.nightscout.androidaps.interfaces.PluginType;
import info.nightscout.androidaps.interfaces.PumpDescription;
import info.nightscout.androidaps.interfaces.PumpInterface;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.general.overview.events.EventOverviewBolusProgress;
import info.nightscout.androidaps.plugins.pump.common.defs.PumpType;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipodUpdateGui;
import info.nightscout.androidaps.plugins.treatments.TreatmentsPlugin;
import info.nightscout.androidaps.utils.DateUtil;
import info.nightscout.androidaps.utils.SP;


import org.json.JSONException;


public class OmnipodPlugin extends PluginBase implements PumpInterface {
    private final Logger log = LoggerFactory.getLogger(L.PUMP);


    private static OmnipodPlugin instance = null;
    private final PumpDescription pumpDescription = new PumpDescription();
    private final OmnipodPdm _pdm;
    private DetailedBolusInfo _runningBolusInfo;

    public static OmnipodPlugin getPlugin() {
        if (instance == null)
            instance = new OmnipodPlugin();
        return instance;
    }
    public OmnipodPlugin() {
        super(new PluginDescription()
                .mainType(PluginType.PUMP)
                .fragmentClass(OmnipodFragment.class.getName())
                .pluginName(R.string.omnipod)
                .shortName(R.string.omnipod_shortname)
                .preferencesId(R.xml.pref_omnipod)
                .neverVisible(Config.NSCLIENT)
                .description(R.string.omnipod_description)
        );

        // explicitly disable aaps interfering with bluetooth connections
        // as it assumes all pumps connect via bluetooth
        SP.putBoolean(R.string.key_btwatchdog, false);

        pumpDescription.setPumpDescription(PumpType.Omnipy_Omnipod);
        log.debug("omnipod plugin initialized");
        Context context = MainApp.instance().getApplicationContext();
        _pdm = new OmnipodPdm(context);
    }

    @Override
    protected void onStart() {
        super.onStart();
        MainApp.bus().register(this);
        log.debug("onstart");
        _pdm.OnStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        _pdm.OnStop();
        MainApp.bus().unregister(this);
    }

    @Subscribe
    public void onStatusEvent(final EventNetworkChange enc) {
        MainApp.bus().post(new EventOmnipodUpdateGui());
    }

    public OmnipodPdm getPdm()
    {
        return _pdm;
    }

    @Override
    public boolean isInitialized() {
        return _pdm.IsInitialized();
    }

    @Override
    public boolean isSuspended() {
        return _pdm.IsSuspended();
    }

    @Override
    public boolean isBusy() { return _pdm.IsBusy(); }

    @Override
    public boolean isConnected() { return _pdm.IsConnected(); }

    @Override
    public boolean isConnecting() { return _pdm.IsConnecting(); }

    @Override
    public boolean isHandshakeInProgress() { return _pdm.IsHandshakeInProgress();  }

    @Override
    public void finishHandshaking() { _pdm.FinishHandshaking(); }

    @Override
    public void connect(String reason) {
        _pdm.Connect();
        MainApp.bus().post(new EventOmnipodUpdateGui());
    }

    @Override
    public void disconnect(String reason) {
        _pdm.Disconnect();
    }

    @Override
    public void stopConnecting() {
        _pdm.StopConnecting();
    }

    @Override
    public void getPumpStatus() {
        _pdm.UpdateStatus();
    }

    @Override
    public PumpEnactResult setNewBasalProfile(Profile profile) {
        PumpEnactResult ret = _pdm.SetNewBasalProfile(profile);
        return ret;
    }

    @Override
    public boolean isThisProfileSet(Profile profile) {
        log.debug("omnipod plugin isThisProfileSet()");
        return _pdm.IsProfileSet(profile);
    }

    @Override
    public long lastDataTime()
    {
        return _pdm.GetLastUpdated();
    }

    @Override
    public double getBaseBasalRate() {
        log.debug("omnipod plugin GetBaseBasalRate()");
        return _pdm.GetBaseBasalRate();
    }

    @Override
    public double getReservoirLevel() {
        return _pdm.GetReservoirLevel();
    }

    @Override
    public int getBatteryLevel() {
        // TODO: return from api
        return 50;
    }

    @Override
    public PumpEnactResult deliverTreatment(DetailedBolusInfo detailedBolusInfo) {

        log.debug("omnipod plugin DeliverTreatment()");
        PumpEnactResult result = _pdm.Bolus(detailedBolusInfo);

        if (!result.success) {
            result.bolusDelivered = 0;
            return result;
        }

        detailedBolusInfo.deliverAt = _pdm.GetLastUpdated();
        TreatmentsPlugin.getPlugin().addToHistoryTreatment(detailedBolusInfo, false);

        result.carbsDelivered = detailedBolusInfo.carbs;
        result.comment = "";

        _runningBolusInfo = detailedBolusInfo;
        Double delivering = 0.05d;

        while (delivering < detailedBolusInfo.insulin) {
            EventOverviewBolusProgress bolusingEvent = EventOverviewBolusProgress.getInstance();
            bolusingEvent.status = String.format(MainApp.gs(R.string.bolusdelivering), delivering);
            bolusingEvent.percent = Math.min((int) (delivering / detailedBolusInfo.insulin * 100), 100);
            MainApp.bus().post(bolusingEvent);
            delivering += 0.05d;
            SystemClock.sleep(2000);
        }
        SystemClock.sleep(200);
        EventOverviewBolusProgress bolusingEvent = EventOverviewBolusProgress.getInstance();
        bolusingEvent.status = String.format(MainApp.gs(R.string.bolusdelivered), detailedBolusInfo.insulin);
        bolusingEvent.percent = 100;
        MainApp.bus().post(bolusingEvent);
        SystemClock.sleep(1000);
        if (L.isEnabled(L.PUMPCOMM))
            log.debug("Delivering treatment insulin: " + detailedBolusInfo.insulin + "U carbs: " + detailedBolusInfo.carbs + "g " + result);
        MainApp.bus().post(new EventOmnipodUpdateGui());
        return result;
    }

    @Override
    public void stopBolusDelivering() {
        log.debug("omnipod plugin StopBolusDelivering()");
        if (_runningBolusInfo != null) {
            _pdm.CancelBolus(result ->
            {
                double canceled = -1d;
                if (result.success) {
                    canceled = result.status.insulin_canceled;
                }

                EventOverviewBolusProgress bolusingEvent = EventOverviewBolusProgress.getInstance();

                Double supposedToDeliver = _runningBolusInfo.insulin;
                if (canceled <= 0d) {
                    if (bolusingEvent != null)
                        bolusingEvent.status = String.format("Couldn't stop bolus in time, delivered: %f.2u", supposedToDeliver);
                } else {
                    if (bolusingEvent != null)
                        bolusingEvent.status = MainApp.gs(R.string.bolusstopped);
                    _runningBolusInfo.insulin = supposedToDeliver - canceled;
                }
                if (bolusingEvent != null) {
                    MainApp.bus().post(bolusingEvent);
                    MainApp.bus().post(new EventOmnipodUpdateGui());
                }
                SystemClock.sleep(100);
                if (canceled > 0d)
                    _runningBolusInfo.notes = String.format("Delivery stopped at %f.2u. Original bolus request was: %f.2u", supposedToDeliver - canceled, supposedToDeliver);
                TreatmentsPlugin.getPlugin().addToHistoryTreatment(_runningBolusInfo, true);
            });

            EventOverviewBolusProgress bolusingEvent = EventOverviewBolusProgress.getInstance();
            if (bolusingEvent != null) {
                bolusingEvent.status = MainApp.gs(R.string.bolusstopping);
                MainApp.bus().post(bolusingEvent);
                MainApp.bus().post(new EventOmnipodUpdateGui());
            }
        }
    }

    @Override
    public PumpEnactResult setTempBasalAbsolute(Double absoluteRate, Integer durationInMinutes, Profile profile, boolean enforceNew) {
        log.debug("omnipod plugin SetTempBasalAbsolute()");
        PumpEnactResult result = _pdm.SetTempBasal(absoluteRate, durationInMinutes, profile, enforceNew);
        if (result.success) {
            result.absolute = absoluteRate;
            result.duration = durationInMinutes;
            result.comment = "";

            TemporaryBasal tempBasal = new TemporaryBasal()
                    .date(_pdm.GetLastUpdated())
                    .absolute(result.absolute)
                    .duration(result.duration)
                    .source(Source.USER);
            TreatmentsPlugin.getPlugin().addToHistoryTempBasal(tempBasal);
            if (L.isEnabled(L.PUMPCOMM))
                log.debug("Setting temp basal absolute: " + result);
        }
        MainApp.bus().post(new EventOmnipodUpdateGui());
        return result;
    }

    @Override
    public PumpEnactResult setTempBasalPercent(Integer percent, Integer durationInMinutes, Profile profile, boolean enforceNew) {
        log.debug("omnipod plugin SetTempBasalPercent()");
        PumpEnactResult per = new PumpEnactResult();
        per.enacted = false;
        per.success = false;
        return per;
    }

    @Override
    public PumpEnactResult setExtendedBolus(Double insulin, Integer durationInMinutes) {
        log.debug("omnipod plugin SetExtendedBolus()");
        PumpEnactResult per = new PumpEnactResult();
        per.enacted = false;
        per.success = false;
        return per;
    }

    @Override
    public PumpEnactResult cancelTempBasal(boolean enforceNew) {
        log.debug("CancelTempBasal()");
        PumpEnactResult result = _pdm.CancelTempBasal(enforceNew);
        if (result.success) {
            if (TreatmentsPlugin.getPlugin().isTempBasalInProgress()) {
                TemporaryBasal tempStop = new TemporaryBasal().date(_pdm.GetLastUpdated()).source(Source.USER);
                TreatmentsPlugin.getPlugin().addToHistoryTempBasal(tempStop);
                MainApp.bus().post(new EventOmnipodUpdateGui());
            }
        }
        return result;
    }

    @Override
    public PumpEnactResult cancelExtendedBolus() {
        log.debug("CancelExtendedBolus()");
        PumpEnactResult per = new PumpEnactResult();
        per.enacted = false;
        per.success = false;
        return per;
    }

    @Override
    public JSONObject getJSONStatus(Profile profile, String profileName) {
        log.debug("GetJSONStatus()");
        long now = System.currentTimeMillis();

        JSONObject pump = new JSONObject();
        JSONObject battery = new JSONObject();
        JSONObject status = new JSONObject();
        JSONObject extended = new JSONObject();
        try {
            battery.put("percent", getBatteryLevel());
            status.put("status", _pdm.getPodStatusText());
            extended.put("Version", BuildConfig.VERSION_NAME + "-" + BuildConfig.BUILDVERSION);
            try {
                extended.put("ActiveProfile", profileName);
            } catch (Exception ignored) {
            }
            TemporaryBasal tb = TreatmentsPlugin.getPlugin().getTempBasalFromHistory(now);
            if (tb != null) {
                extended.put("TempBasalAbsoluteRate", tb.tempBasalConvertedToAbsolute(now, profile));
                extended.put("TempBasalStart", DateUtil.dateAndTimeString(tb.date));
                extended.put("TempBasalRemaining", tb.getPlannedRemainingMinutes());
            }
            ExtendedBolus eb = TreatmentsPlugin.getPlugin().getExtendedBolusFromHistory(now);
            if (eb != null) {
                extended.put("ExtendedBolusAbsoluteRate", eb.absoluteRate());
                extended.put("ExtendedBolusStart", DateUtil.dateAndTimeString(eb.date));
                extended.put("ExtendedBolusRemaining", eb.getPlannedRemainingMinutes());
            }
            status.put("timestamp", DateUtil.toISOString(now));

            pump.put("battery", battery);
            pump.put("status", status);
            pump.put("extended", extended);
            pump.put("reservoir", getReservoirLevel());
            pump.put("clock", DateUtil.toISOString(now));
        } catch (JSONException e) {
            log.error("Unhandled exception", e);
        }
        return pump;
    }

    @Override
    public String deviceID() {
        log.debug("DeviceID()");
        return _pdm.GetPodId();
    }

    @Override
    public PumpDescription getPumpDescription() {
        //log.debug("getPumpDescription()");
        return pumpDescription;
    }

    @Override
    public String shortStatus(boolean veryShort) {
        log.debug("shortStatus()");
        return _pdm.GetStatusShort();
    }

    @Override
    public boolean isFakingTempsByExtendedBoluses() {
        //log.debug("isFaking()");
        return false;
    }

    @Override
    public PumpEnactResult loadTDDs() {
        log.debug("loadTDDs()");
        PumpEnactResult per = new PumpEnactResult();
        per.enacted = false;
        per.success = false;
        return per;
    }
}
