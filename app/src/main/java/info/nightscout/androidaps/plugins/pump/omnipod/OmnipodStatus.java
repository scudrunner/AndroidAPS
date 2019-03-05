package info.nightscout.androidaps.plugins.pump.omnipod;

import org.json.JSONException;
import org.json.JSONObject;

public class OmnipodStatus {

    //    {
//        "result": {
//        "address": 521046513,
//                "alert_states": 0,
//                "basalSchedule": [],
//        "basalState": 1,
//                "bolusState": 0,
//                "canceledInsulin": 0.0,
//                "extendedBolus": [],
//        "fault_event": 28,
//                "fault_event_rel_time": 4800,
//                "fault_immediate_bolus_in_progress": 0,
//                "fault_insulin_state_table_corruption": 0,
//                "fault_internal_variables": 0,
//                "fault_progress_before": 9,
//                "fault_progress_before_2": 9,
//                "fault_table_access": 0,
//                "faulted": false,
//                "information_type2_last_word": 22,
//                "lastNonce": 880159024,
//                "lastUpdated": 1551003635.0432065,
//                "last_enacted_bolus_amount": 0.7,
//                "last_enacted_bolus_start": 1551003200.5788243,
//                "last_enacted_temp_basal_amount": 0.0,
//                "last_enacted_temp_basal_duration": 1.0,
//                "last_enacted_temp_basal_start": 1551003339.9190547,
//                "log_file_path": "data/pod.log",
//                "lot": 44223,
//                "maximumBolus": 15,
//                "maximumTempBasal": 15,
//                "minutes_since_activation": 1998,
//                "msgSequence": 6,
//                "nonceSeed": 11,
//                "packetSequence": 22,
//                "path": "data/pod.json",
//                "progress": 8,
//                "radio_low_gain": 0,
//                "radio_rssi": 23,
//                "reservoir": 51.150000000000006,
//                "tempBasal": [],
//        "tid": 580437,
//                "totalInsulin": 70.5,
//                "utcOffset": 0
//    },
//        "success": true
//    }

    int _lot;
    int _tid;
    int _address;

    long _statusDate;
    int _activeMinutes;
    double _insulinDelivered;
    double _insulinCanceled;
    int _podProgress;
    int _bolusState;
    int _basalState;
    boolean _faulted;
    Double _reservoir;

    public OmnipodStatus(String jsonString) {
        this.Update(jsonString);
    }

    public boolean Update(String jss)
    {
        try {
            if (jss == null)
                return false;
            JSONObject jo = new JSONObject(jss);
            if (!jo.getBoolean("success"))
                return false;
            JSONObject jr = jo.getJSONObject("result");
            return Update(jr);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean Update(JSONObject jr)
    {
        try {
            if (jr == null)
                return false;
            _lot = jr.getInt("lot");
            _tid = jr.getInt("tid");
            _address = jr.getInt("address");
            _statusDate = (long) (jr.getDouble("lastUpdated") * 1000);
            _activeMinutes = jr.getInt("minutes_since_activation");
            _insulinDelivered = jr.getDouble("totalInsulin");
            _insulinCanceled = jr.getDouble("canceledInsulin");
            _podProgress = jr.getInt("progress");
            _bolusState = jr.getInt("bolusState");
            _basalState = jr.getInt("basalState");
            _faulted = jr.getBoolean("faulted");
            _reservoir = jr.getDouble("reservoir");
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
}
