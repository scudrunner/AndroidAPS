package info.nightscout.androidaps.plugins.pump.omnipod.api.rest;

import java.util.Hashtable;
import java.util.Map;

public class OmnipyConstants {

    private static final Map<OmnipyRequestType,String> typePathMap;

    static {
        typePathMap = new Hashtable<OmnipyRequestType,String>();

        typePathMap.put(OmnipyRequestType.Ping, "/omnipy/ping");
        typePathMap.put(OmnipyRequestType.Shutdown, "/omnipy/shutdown");
        typePathMap.put(OmnipyRequestType.Restart, "/omnipy/restart");
        typePathMap.put(OmnipyRequestType.Token, "/omnipy/token");
        typePathMap.put(OmnipyRequestType.CheckPassword, "/omnipy/pwcheck");
        typePathMap.put(OmnipyRequestType.NewPod, "/omnipy/newpod");
        typePathMap.put(OmnipyRequestType.SetPodParameters, "/omnipy/parameters");
        typePathMap.put(OmnipyRequestType.ReadPdmAddress, "/omnipy/pdmspy");
        typePathMap.put(OmnipyRequestType.RLInfo, "/rl/info");
        typePathMap.put(OmnipyRequestType.ActivatePod, "/pdm/activate");
        typePathMap.put(OmnipyRequestType.StartPod, "/pdm/start");
        typePathMap.put(OmnipyRequestType.Status, "/pdm/status");
        typePathMap.put(OmnipyRequestType.IsBusy, "/pdm/isbusy");
        typePathMap.put(OmnipyRequestType.AckAlerts, "/pdm/ack");
        typePathMap.put(OmnipyRequestType.DeactivatePod, "/pdm/deactivate");
        typePathMap.put(OmnipyRequestType.Bolus, "/pdm/bolus");
        typePathMap.put(OmnipyRequestType.CancelBolus, "/pdm/cancelbolus");
        typePathMap.put(OmnipyRequestType.TempBasal, "/pdm/settempbasal");
        typePathMap.put(OmnipyRequestType.CancelTempBasal, "/pdm/canceltempbasal");
        typePathMap.put(OmnipyRequestType.SetBasalSchedule, "/pdm/setbasalschedule");
    }

    public static final String OMNIPY_PARAM_AUTH = "auth";
    public static final String OMNIPY_PARAM_IV = "i";

    public static final String OMNIPY_PARAM_PDM_ADDRESS_TIMEOUT = "timeout";

    public static final String OMNIPY_PARAM_NEW_POD_ADDRESS = "radio_address";
    public static final String OMNIPY_PARAM_NEW_POD_TID = "id_t";
    public static final String OMNIPY_PARAM_NEW_POD_LOT = "id_lot";

    public static final String OMNIPY_PARAM_SET_POD_ADDRESS = "radio_address";
    public static final String OMNIPY_PARAM_SET_POD_UTC_OFFSET = "var_utc_offset";


    public static final String OMNIPY_PARAM_SET_POD_MAX_BOLUS = "var_maximum_bolus";
    public static final String OMNIPY_PARAM_SET_POD_MAX_TEMPBASAL ="var_maximum_temp_basal_rate";
    public static final String OMNIPY_PARAM_SET_POD_ALERT_LOW_RESERVOIR = "var_alert_low_reservoir";
    public static final String OMNIPY_PARAM_SET_POD_ALERT_REPLACE_POD = "var_alert_replace_pod";
    public static final String OMNIPY_PARAM_SET_POD_NOTIFY_BOLUS = "var_notify_bolus_start";
    public static final String OMNIPY_PARAM_SET_POD_NOTIFY_BOLUS_CANCEL = "var_notify_bolus_cancel";
    public static final String OMNIPY_PARAM_SET_POD_NOTIFY_TEMPBASAL = "var_notify_temp_basal_set";
    public static final String OMNIPY_PARAM_SET_POD_NOTIFY_TEMPBASAL_CANCEL = "var_notify_temp_basal_cancel";
    public static final String OMNIPY_PARAM_SET_POD_NOTIFY_BASAL_SCHEDULE_CHANGE ="var_notify_basal_schedule_change";

    public static final String OMNIPY_PARAM_STATUS_TYPE = "type";

    public static final String OMNIPY_PARAM_BOLUS_AMOUNT = "amount";

    public static final String OMNIPY_PARAM_TEMPBASAL_RATE = "amount";
    public static final String OMNIPY_PARAM_TEMPBASAL_HOURS = "hours";

    public static final String OMNIPY_PARAM_ACK_ALERT_MASK = "alertmask";

    public static String getPath(OmnipyRequestType type)
    {
        return typePathMap.get(type);
    }
}
