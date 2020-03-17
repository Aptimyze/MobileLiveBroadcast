package tae.mobilelivebroadcast.util;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.mxn.soul.flowingdrawer_core.MenuFragment;

import tae.mobilelivebroadcast.BroadcastActivity;
import tae.mobilelivebroadcast.MainActivity;
import tae.mobilelivebroadcast.R;

/**
 * Created by Tae on 2016-04-26.
 */
public class MyMenuFragment extends MenuFragment {
    insertRedis insertRedis;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container,
                false);

        LinearLayout broadcastLinear = (LinearLayout)view.findViewById(R.id.broadcastStart);
        broadcastLinear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentBroadcastActivity = new Intent(getContext(), BroadcastActivity.class);
                startActivity(intentBroadcastActivity);
            }
        });
        Button btnLogout = (Button)view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intentMainActivity = new Intent(getContext(), MainActivity.class);
                startActivity(intentMainActivity);
            }
        });

        return  setupReveal(view) ;
    }

    public void onOpenMenu(){
//        Toast.makeText(getActivity(),"onOpenMenu",Toast.LENGTH_SHORT).show();
    }
    public void onCloseMenu(){
//        Toast.makeText(getActivity(), "onCloseMenu", Toast.LENGTH_SHORT).show();
    }

}
