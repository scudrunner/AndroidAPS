package info.nightscout.androidaps.plugins.PumpOmnipod;

import android.content.Context;
import android.os.SystemClock;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.R2;
import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.data.PumpEnactResult;
import info.nightscout.androidaps.db.Source;
import info.nightscout.androidaps.db.TemporaryBasal;
import info.nightscout.androidaps.events.EventPumpStatusChanged;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.ConfigBuilder.ProfileFunctions;
import info.nightscout.androidaps.plugins.PumpVirtual.events.EventVirtualPumpUpdateGui;
import info.nightscout.androidaps.plugins.Treatments.TreatmentsPlugin;
import info.nightscout.utils.SP;

public class OmnipodPdm {

    private Context _context;

    private Profile _profile;

    private long _lastConnected = 0;
    private boolean _lastCommandSucceeded = false;

    private OmnipyNetworkDiscovery _omnipyNetworkDiscovery;
    private OmnipyRestApi _omnipyRestApiCached;
    private OmnipyApiSecret _omnipyApiSecretCached;
    private OmnipodStatus _podStatus;
    private Logger _log;

    public OmnipodPdm(Context context)
    {
        _context = context;
        _log =  LoggerFactory.getLogger(L.PUMP);
        _omnipyNetworkDiscovery = new OmnipyNetworkDiscovery(_context);
        _podStatus = new OmnipodStatus(SP.getString(R.string.key_omnipod_status, null));
    }

    public void UpdateStatus() {
        OmnipyRestApi rest = getRestApi();
        if (rest != null)
        {
            String response = rest.Status();
            _lastCommandSucceeded = parseStatusResponse(response);
        }
    }

    public void OnStart() {
    }

    public void InvalidateApiSecret()
    {
        _omnipyApiSecretCached = null;
        _omnipyRestApiCached = null;
    }

    public void InvalidateOmnipyHost()
    {
        _omnipyNetworkDiscovery.ClearKnownAddress();
        _omnipyRestApiCached = null;
    }

    private OmnipyRestApi getRestApi()
    {
        if (_omnipyRestApiCached == null) {
            String hostName = getOmnipyHost();
            OmnipyApiSecret apiSecret = getApiSecret();
            if (hostName != null && apiSecret != null) {
                _omnipyRestApiCached = new OmnipyRestApi("http://" + hostName + ":4444",
                        apiSecret);
            }
        }
        return _omnipyRestApiCached;
    }

    private String getOmnipyHost()
    {
        String omnipyHost;
        if (SP.getBoolean(R.string.key_omnipy_autodetect_host, true)) {
            omnipyHost = _omnipyNetworkDiscovery.GetLastKnownAddress();
            if (omnipyHost == null)
                _omnipyNetworkDiscovery.RunDiscovery();
        }
        else
        {
            omnipyHost = SP.getString(R.string.key_omnipy_host, null);
            if (omnipyHost != null && omnipyHost.length() == 0)
                return null;
        }

        return omnipyHost;
    }

    private OmnipyApiSecret getApiSecret()
    {
        if (_omnipyApiSecretCached == null)
        {
            String secret = SP.getString(R.string.key_omnipy_password, "");
            if (secret == null || secret.length() == 0)
                return null;

            _omnipyApiSecretCached = OmnipyApiSecret.fromPassphrase(
                    SP.getString(R.string.key_omnipy_password, ""));
        }
        return _omnipyApiSecretCached;
    }

    private boolean parseStatusResponse(String response)
    {
        try {
            if (response == null)
                return false;
            JSONObject jo = new JSONObject(response);
            if (!jo.getBoolean("success"))
                return false;
            JSONObject result = jo.getJSONObject("result");
            boolean ret = _podStatus.Update(result);
            if (ret) {
                SP.putString(R.string.key_omnipod_status, result.toString());
            }
            return ret;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public PumpEnactResult SetProfile(Profile profile) {

        _profile = profile;
        return CreateResult(true);
    }

    public boolean VerifyProfile(Profile profile) {
        _profile = profile;
        return true;
    }

    public PumpEnactResult Bolus(DetailedBolusInfo detailedBolusInfo) {
        BigDecimal iuBolus = GetExactInsulinUnits(detailedBolusInfo.insulin);

        OmnipyRestApi rest = getRestApi();
        if (rest != null)
        {
            _lastCommandSucceeded = parseStatusResponse(rest.Bolus(iuBolus));
        }
        else
        {
            _lastCommandSucceeded = false;
        }

        PumpEnactResult r = new PumpEnactResult();
        r.enacted = _lastCommandSucceeded;
        r.success = _lastCommandSucceeded;
        if (r.enacted)
        {
            r.success = true;
            r.bolusDelivered = Double.parseDouble(iuBolus.toString());
            //_busyUntil = SystemClock.elapsedRealtime() + (long)(r.bolusDelivered * 40000d);
        }
        return r;
    }

    public double CancelBolus() {
        OmnipyRestApi rest = getRestApi();
        if (rest != null)
        {
            _lastCommandSucceeded = parseStatusResponse(rest.CancelBolus());
        }
        else
        {
            _lastCommandSucceeded = false;
        }

        if (_lastCommandSucceeded) {
            //_busyUntil = 0;
            return _podStatus._insulinCanceled;
        }
        else
        {
            return -1d;
        }
    }

    public double GetBasalRate() {
        if (_profile == null)
        {
            _profile = ProfileFunctions.getInstance().getProfile();
        }

        if (_profile == null)
            return 0d;
        else
            return _profile.getBasal();
    }

    public long GetLastUpdated() {
        return  _podStatus._statusDate;
    }

    public PumpEnactResult SetTempBasal(Double absoluteRate, Integer durationInMinutes, Profile profile, boolean enforceNew) {
        BigDecimal iuRate = GetExactInsulinUnits(absoluteRate);
        BigDecimal durationHours = GetExactHourUnits(durationInMinutes);

        OmnipyRestApi rest = getRestApi();
        if (rest != null)
        {
            _lastCommandSucceeded = parseStatusResponse(rest.SetTempBasal(iuRate, durationHours));
        }
        else
        {
            _lastCommandSucceeded = false;
        }

        PumpEnactResult r = new PumpEnactResult();
        r.enacted = _lastCommandSucceeded;
        r.success = _lastCommandSucceeded;
        if (r.enacted)
        {
            r.duration = durationInMinutes;
            r.absolute = Double.parseDouble(iuRate.toString());
        }
        return r;
    }

    public PumpEnactResult CancelTempBasal(boolean enforceNew) {

        OmnipyRestApi rest = getRestApi();
        if (rest != null)
        {
            _lastCommandSucceeded = parseStatusResponse(rest.CancelTempBasal());
        }
        else
        {
            _lastCommandSucceeded = false;
        }

        PumpEnactResult r = new PumpEnactResult();
        r.enacted = _lastCommandSucceeded;
        r.success = _lastCommandSucceeded;
        if (r.enacted)
        {
            r.isTempCancel = true;
        }
        return r;
    }

    public String GetPodId() {
        return String.format("L%dT%d", _podStatus._lot, _podStatus._tid);
    }

    public String GetStatusShort() {
        if (_podStatus._faulted)
            return "FAULT";
        if (_podStatus._podProgress == 9)
            return "INSULIN <50";
        if (_podStatus._podProgress == 8)
            return "OK";
        return "UNKNOWN";
    }

    private PumpEnactResult CreateResult(boolean enacted)
    {
        PumpEnactResult r = new PumpEnactResult();
        r.enacted = enacted;
        return r;
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

    public boolean IsInitialized() {
        OmnipyRestApi rest = getRestApi();
        return rest != null;
    }

    public boolean IsSuspended() {
        return false;
    }

    public boolean AcceptsCommands() {
        OmnipyRestApi rest = getRestApi();
        if (rest == null)
            return false;
        String response = rest.IsBusy();
        try {
            JSONObject jo = new JSONObject(response);
            if (!jo.getBoolean("success"))
                return false;
            JSONObject result = jo.getJSONObject("result");
            return !result.getBoolean("busy");
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean IsConnected() {
        OmnipyRestApi rest = getRestApi();
        if (rest == null)
            return false;

        if ( SystemClock.elapsedRealtime() - _lastConnected < 30000)
            return true;

        String response = rest.CheckAuthentication();
        try {
            JSONObject jo = new JSONObject(response);
            if (!jo.getBoolean("success"))
                return false;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        _lastConnected = SystemClock.elapsedRealtime();
        return true;
    }

    public boolean IsConnecting() {
        return false;
    }

    public boolean IsHandshakeInProgress() {
        return false;
    }

    public void FinishHandshaking() {
    }

    public void Connect() {
    }

    public void StopConnecting()
    {
    }
    public void Disconnect() {
    }
}
