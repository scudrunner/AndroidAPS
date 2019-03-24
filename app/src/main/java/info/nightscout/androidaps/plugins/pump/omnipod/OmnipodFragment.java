package info.nightscout.androidaps.plugins.pump.omnipod;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import java.util.Objects;

import info.nightscout.androidaps.R;
import info.nightscout.androidaps.plugins.common.SubscriberFragment;
import info.nightscout.androidaps.plugins.pump.omnipod.events.EventOmnipodUpdateGui;

public class OmnipodFragment extends SubscriberFragment {

    public OmnipodFragment() {
        // Required empty public constructor
    }

    public static OmnipodFragment newInstance() {
        return new OmnipodFragment();
    }

    private OmnipodPdm _pdm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        Intent intent = Objects.requireNonNull(getActivity()).getIntent();
//        if (intent != null) {
//
//            Bundle extras = intent.getExtras();
//            if (extras != null) {
//                String address = extras.getString("omnipy_address");
//                if (address != null) {
//                    //
//                }
//            }
//        }

        OmnipodPlugin op = OmnipodPlugin.getPlugin();
        _pdm = op.getPdm();
    }

    @Subscribe
    public void onStatusEvent(final EventOmnipodUpdateGui ev) {
        Activity activity = getActivity();
        if (activity != null)
            activity.runOnUiThread(() -> updateGUI() );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_omnipod, container, false);

//        View.OnClickListener onc = new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//        }

//        ((Button)view.findViewById(R.id.omnipy_btn_check_connection)).setOnClickListener(this.onClick(view));

        return view;
    }

    @Override
    protected void updateGUI() {
        View vw = this.getView();

        TextView tv = vw.findViewById(R.id.omnipy_txt_connection_status);
        tv.setText(_pdm.getConnectionStatusText());

        tv = vw.findViewById(R.id.omnipy_txt_pod_status);
        tv.setText(_pdm.getPodStatusText());
    }
}
