package tae.mobilelivebroadcast;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.jpardogo.android.flabbylistview.lib.FlabbyListView;
import com.mxn.soul.flowingdrawer_core.FlowingView;
import com.mxn.soul.flowingdrawer_core.LeftDrawerLayout;

import java.util.ArrayList;

import jp.co.recruit_lifestyle.android.widget.WaveSwipeRefreshLayout;
import tae.mobilelivebroadcast.util.Config;
import tae.mobilelivebroadcast.util.ListRoomAdapter;
import tae.mobilelivebroadcast.util.MyMenuFragment;
import tae.mobilelivebroadcast.util.insertRedis;

/**
 * Created by Tae on 2016-04-25.
 */
public class ListRoomActivity extends AppCompatActivity {

    private ListRoomAdapter mAdapter;
    private WaveSwipeRefreshLayout mWaveSwipeRefreshLayout;
    private LeftDrawerLayout mLeftDrawerLayout;
    MyMenuFragment mMenuFragment;
    insertRedis insertRedis;

    //e-mail for broadcasting stream
    public static String mEmail;

    //list 정보를 위한 변수
    public static String response = null;
    ArrayList<String> roomMaster;
    ArrayList<String> roomTitle;
    ArrayList<String> roomMemberCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_layout);

        insertRedis = new insertRedis();
        roomMaster = new ArrayList<String>();
        roomTitle = new ArrayList<String>();
        roomMemberCount = new ArrayList<String>();


        SharedPreferences sharedPreferences = getSharedPreferences(Config.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        String email = sharedPreferences.getString(Config.EMAIL_SHARED_PREF, "Not Available");
        mEmail = email;
        Log.e("isEmailEmpty",""+mEmail);

        //리프레쉬 객체선언 및 리스너 실행
        mWaveSwipeRefreshLayout = (WaveSwipeRefreshLayout) findViewById(R.id.refresh_list_swipe);
        mWaveSwipeRefreshLayout.setOnRefreshListener(new WaveSwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Do work to refresh the list here.
                new refreshTask().execute();
            }
        });

        //네비게이션 바 프래그먼트
        mLeftDrawerLayout = (LeftDrawerLayout) findViewById(R.id.id_drawerlayout);
        FragmentManager fm = getSupportFragmentManager();
        mMenuFragment = (MyMenuFragment) fm.findFragmentById(R.id.id_container_menu);
        FlowingView mFlowingView = (FlowingView) findViewById(R.id.sv);
        if (mMenuFragment == null) {
            fm.beginTransaction().add(R.id.id_container_menu, mMenuFragment = new MyMenuFragment()).commit();
        }
        mLeftDrawerLayout.setFluidView(mFlowingView);
        mLeftDrawerLayout.setMenuFragment(mMenuFragment);

    }

    @Override
    protected void onResume() {
        super.onResume();
        mWaveSwipeRefreshLayout.setRefreshing(true);
        new refreshTask().execute();
    }

    @Override
    protected void onStop() {
        super.onStop();
        clearList();
    }

    //서버에서 리스트 정보 한번에 긁어와서 분리저장
    private void initListInfo(){
        if(response != null) {
            if(response.length()>10) {
                Log.e("Check3","check?");
                //response로 문자열 쭉 받아서 ,기준으로 한번 배열로 정리하고 아이템들을 --기준으로 아이디와 방제를 배열하고
                //그다음 방제에 "/'가 있으면 걸러내고(redis의 오류로 밸류도 키값이 됨)
                //남은것들은 방제와 타이틀을 arraylist에 넣어줌.
                String[] arrayRoomList = response.split(",");
                for (int i = 0; i < arrayRoomList.length; i++) {
                    String[] arrayRoomMaster = arrayRoomList[i].split("--");
                    if (!arrayRoomMaster[0].matches(".*/.*")) {
                        roomMaster.add(arrayRoomMaster[0]);
                        String[] arrayRoomTitle = arrayRoomMaster[1].split("/");
                        roomTitle.add(arrayRoomTitle[0]);
                    }
                }
            }
        }
    }

    //리스트 어뎁터 관리
    private void initListAdapter(){
        mAdapter = new ListRoomAdapter(this,roomMaster, roomTitle, roomMemberCount);
        FlabbyListView flabbyListView = (FlabbyListView)findViewById(R.id.list);
        flabbyListView.setAdapter(mAdapter);
        flabbyListView.setSelection(0);
        flabbyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(), PLVideoViewActivity.class);
                String broadcastStreamUrl = Config.BROADCAST+roomMaster.get(position);
                insertRedis.init(getApplicationContext(),Config.INSERT_REDIS_URL,Config.RKEY_ROOM_MEMBER,roomMaster.get(position));
                i.putExtra("StreamPath", broadcastStreamUrl);
                i.putExtra("MasterName", roomMaster.get(position));
                startActivity(i);
            }
        });
    }

    //리스트를 위한 자료구조를 자주 청소 해줍시다.
    private void clearList(){
        if(roomMaster!=null&&roomTitle!=null&&roomMemberCount!=null){
            roomMaster.clear();
            roomTitle.clear();
            roomMemberCount.clear();
        }
    }

    //1차 리프레쉬 클래스(여기서는 정보만 긁어옴)
    private class refreshTask extends AsyncTask<Void, Void, String[]> {
        @Override
        protected String[] doInBackground(Void... params) {
            clearList();
            insertRedis.init(getApplicationContext(), Config.GETLIST_URL, "", "");
            return new String[0];
        }

        @Override
        protected void onPostExecute(String[] result) {
            // Call setRefreshing(false) when the list has been refreshed.
            new refreshTask2().execute();
            super.onPostExecute(result);
        }
    }

    //2차 리프레쉬 클래스(여기선 정보 분리저장 및 방 참가인원정보, 리스트 어뎁터 실행 등 수행)
    private class refreshTask2 extends AsyncTask<Void, Void, String[]> {
        @Override
        protected String[] doInBackground(Void... params) {
            try {
                Thread.sleep(500);
                initListInfo();
                if(roomMaster!=null){
                    for(int i=0;i<roomMaster.size();i++){
                        insertRedis.init(getApplicationContext(),Config.ROOM_COUNT_URL,Config.RKEY_ROOM_MEMBER,roomMaster.get(i));
                        Thread.sleep(500);
                        roomMemberCount.add(response);
                        Log.e("check1", "Check");
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return new String[0];
        }

        @Override
        protected void onPostExecute(String[] result) {
            // Call setRefreshing(false) when the list has been refreshed.
            initListAdapter();
            mWaveSwipeRefreshLayout.setRefreshing(false);
            super.onPostExecute(result);
        }
    }

}
