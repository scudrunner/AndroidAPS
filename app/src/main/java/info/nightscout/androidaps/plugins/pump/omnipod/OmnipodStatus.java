package info.nightscout.androidaps.plugins.pump.omnipod;

import com.google.gson.Gson;

import java.math.BigDecimal;

public class OmnipodStatus {


    // API V1.0
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


// API V1.1
//    {
//        "result": {
//        "fault_event": null,
//                "fault_event_rel_time": null,
//                "fault_immediate_bolus_in_progress": null,
//                "fault_information_type2_last_word": null,
//                "fault_insulin_state_table_corruption": null,
//                "fault_internal_variables": null,
//                "fault_progress_before": null,
//                "fault_progress_before_2": null,
//                "fault_table_access": null,
//                "id_lot": 44223,
//                "id_t": 571807,
//                "id_version_pi": null,
//                "id_version_pm": null,
//                "id_version_unknown_7_bytes": null,
//                "id_version_unknown_byte": null,
//                "insulin_canceled": 0.0,
//                "insulin_delivered": 24.150000000000002,
//                "insulin_reservoir": 51.150000000000006,
//                "last_enacted_bolus_amount": 0.4,
//                "last_enacted_bolus_start": 1552084788.4410026,
//                "last_enacted_temp_basal_amount": 0.0,
//                "last_enacted_temp_basal_duration": 2.0,
//                "last_enacted_temp_basal_start": 1552097314.6736631,
//                "log_file_path": "data/pod.log",
//                "nonce_last": 162125019,
//                "nonce_seed": 5,
//                "path": "data/pod.json",
//                "radio_address": 521206857,
//                "radio_address_candidate": null,
//                "radio_low_gain": null,
//                "radio_message_sequence": 10,
//                "radio_packet_sequence": 6,
//                "radio_rssi": null,
//                "state_active_minutes": 307,
//                "state_alert": 0,
//                "state_basal": 1,
//                "state_bolus": 0,
//                "state_faulted": false,
//                "state_last_updated": 1552097314.6601844,
//                "state_progress": 8,
//                "var_alert_low_reservoir": null,
//                "var_alert_replace_pod": null,
//                "var_basal_schedule": null,
//                "var_maximum_bolus": null,
//                "var_maximum_temp_basal_rate": null,
//                "var_notify_basal_schedule_change": null,
//                "var_notify_bolus_cancel": null,
//                "var_notify_bolus_start": null,
//                "var_notify_temp_basal_cancel": null,
//                "var_notify_temp_basal_set": null,
//                "var_utc_offset": null
//    },
//        "success": true
//    }


    public int id_lot;
    public int id_t;
    public int radio_address;

    public int state_active_minutes;
    public int state_alert;
    public int state_basal;
    public int state_bolus;
    public boolean state_faulted;
    public double state_last_updated;
    public int state_progress;

    public double insulin_canceled;
    public double insulin_delivered;
    public double insulin_reservoir;

    public BigDecimal[] var_basal_schedule;
    public int var_utc_offset;


    public static OmnipodStatus fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, OmnipodStatus.class);
    }

    public String asJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

//    public OmnipodStatus(String jsonString) {
//        this.Update(jsonString);
//    }
//
//    public boolean Update(String jss)
//    {
//        Gson gson = new Gson();
//        OmnipodStatus status =
//        try {
//            if (jss == null)
//                return false;
//            JSONObject jo = new JSONObject(jss);
//            if (!jo.getBoolean("success"))
//                return false;
//            JSONObject jr = jo.getJSONObject("result");
//            return Update(jr);
//        } catch (JSONException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    public boolean Update(JSONObject jr)
//    {
//        try {
//            if (jr == null)
//                return false;
//            _lot = jr.getInt("id_lot");
//            _tid = jr.getInt("id_t");
//            _address = jr.getInt("address");
//            _statusDate = (long) (jr.getDouble("lastUpdated") * 1000);
//            _activeMinutes = jr.getInt("minutes_since_activation");
//            _insulinDelivered = jr.getDouble("totalInsulin");
//            _insulinCanceled = jr.getDouble("canceledInsulin");
//            _podProgress = jr.getInt("progress");
//            _bolusState = jr.getInt("bolusState");
//            _basalState = jr.getInt("basalState");
//            _faulted = jr.getBoolean("faulted");
//            _reservoir = jr.getDouble("reservoir");
//            return true;
//        } catch (JSONException e) {
//            e.printStackTrace();
//            return false;
//        }
//    }
}
