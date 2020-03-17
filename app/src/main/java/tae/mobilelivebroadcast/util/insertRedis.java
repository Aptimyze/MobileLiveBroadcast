package tae.mobilelivebroadcast.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

import tae.mobilelivebroadcast.ListRoomActivity;

/**
 * Created by Tae on 2016-05-22.
 */
public class insertRedis {

    private Context mContext;
    private String mUrl;
    private String mKey;
    private String mValue;


    public void init(Context context,String url, String key, String value) {

        mContext = context;
        mUrl = url;
        mKey = key;
        mValue = value;


        //Creating a string request
        StringRequest stringRequest = new StringRequest(Request.Method.POST, mUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
//                        Toast.makeText(mContext.getApplicationContext(), response, Toast.LENGTH_LONG).show();
                        Log.e("redis",response);
                        ListRoomActivity.response = response;
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //You can handle error here if you want
                        Toast.makeText(mContext.getApplicationContext(), error.toString(), Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                //Adding parameters to request
                params.put(mKey, mValue);

                //returning parameter
                return params;
            }
        };

        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(mContext.getApplicationContext());
        requestQueue.add(stringRequest);
    }
}
