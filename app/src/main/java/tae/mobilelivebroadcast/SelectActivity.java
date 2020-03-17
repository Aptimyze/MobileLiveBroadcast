package tae.mobilelivebroadcast;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import info.hoang8f.widget.FButton;

/**
 * Created by Tae on 2016-04-28.
 */
public class SelectActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_layout);

        FButton fBtnBroadcating = (FButton)findViewById(R.id.fBtn_host);
        FButton fBtnPlayBroadcast = (FButton)findViewById(R.id.fBtn_guest);

        fBtnBroadcating.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentBroadcastActivity = new Intent(SelectActivity.this, BroadcastActivity.class);
                startActivity(intentBroadcastActivity);
            }
        });

        fBtnPlayBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentPlayActivity = new Intent(SelectActivity.this, PLVideoViewActivity.class);
                startActivity(intentPlayActivity);
            }
        });

        Button btnPlaylist = (Button) findViewById(R.id.play_list_btn);
        btnPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentListActivity = new Intent(SelectActivity.this, ListRoomActivity.class);
                startActivity(intentListActivity);
            }
        });

    }
}
