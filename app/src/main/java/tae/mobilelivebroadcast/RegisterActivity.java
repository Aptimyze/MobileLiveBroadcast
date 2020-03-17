package tae.mobilelivebroadcast;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.dd.processbutton.iml.ActionProcessButton;
import com.dd.processbutton.iml.SubmitProcessButton;

import java.util.HashMap;
import java.util.Map;

import tae.mobilelivebroadcast.util.Config;
import tae.mobilelivebroadcast.util.ProgressGenerator;
import tae.mobilelivebroadcast.util.RegisterUserClass;

/**
 * Created by Tae on 2016-04-25.
 */
public class RegisterActivity extends Activity implements ProgressGenerator.OnCompleteListener{

    ActionProcessButton btnSignIn;
    ProgressGenerator progressGenerator;
    SubmitProcessButton btnCheckId;
    EditText editEmail;
    EditText editPassword;
    EditText editPassCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_layout);

        editEmail = (EditText)findViewById(R.id.edit_register_email);
        editPassword = (EditText)findViewById(R.id.edit_register_password);
        editPassCheck = (EditText)findViewById(R.id.edit_register_passCheck);

        //ID중복확인 버튼
        progressGenerator = new ProgressGenerator(this);
        btnCheckId = (SubmitProcessButton)findViewById(R.id.idCheckBtn);
        btnCheckId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressGenerator.start(btnCheckId);
                btnCheckId.setEnabled(false);
                emailCheck();
            }
        });

        //회원정보 제출 버튼
        btnSignIn = (ActionProcessButton) findViewById(R.id.btnSignIn);
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                btnSignIn.setMode(ActionProcessButton.Mode.PROGRESS);
// no progress
//                button.setProgress(0);
// progressDrawable cover 50% of button width, progressText is shown
//                button.setProgress(50);
// progressDrawable cover 75% of button width, progressText is shown
//                button.setProgress(75);
// completeColor & completeText is shown
//                button.setProgress(100);
                btnSignIn.setMode(ActionProcessButton.Mode.ENDLESS);
                btnSignIn.setProgress(1);
                registerUser();
            }
        });
    }

    private void emailCheck(){
        //Getting values from edit texts
        final String emailCheck = editEmail.getText().toString().trim();

        //Creating a string request
        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.EMAILCHECK_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //If we are getting success from server
                        if(response.equalsIgnoreCase("notAvailable")){
                            Toast.makeText(RegisterActivity.this, "E-mail is already used", Toast.LENGTH_LONG).show();
                        }else{
                            //If the server response is not success
                            //Displaying an error message on toast
                            Toast.makeText(RegisterActivity.this, "You can use this email", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //You can handle error here if you want
                    }
                }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<>();
                //Adding parameters to request
                params.put(Config.KEY_EMAIL, emailCheck);

                //returning parameter
                return params;
            }
        };

        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    private void registerUser() {
        String email = editEmail.getText().toString().trim().toLowerCase();
        String password = editPassword.getText().toString().trim().toLowerCase();
        String passCheck = editPassCheck.getText().toString().trim().toLowerCase();

        if(password.equals(passCheck)){

        register(email,password);

        }else {
            Toast.makeText(this,"password is not matched",Toast.LENGTH_SHORT).show();
            btnSignIn.setProgress(0);
        }
    }

    private void register(String email, String password) {
        class RegisterUser extends AsyncTask<String, Void, String> {
            ProgressDialog loading;
            RegisterUserClass ruc = new RegisterUserClass();


            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(RegisterActivity.this, "Please Wait",null, true, true);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();
                Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                startActivity(intent);
            }

            @Override
            protected String doInBackground(String... params) {

                HashMap<String, String> data = new HashMap<String,String>();
                data.put("email",params[0]);
                data.put("password",params[1]);

                String result = ruc.sendPostRequest(Config.REGISTER_URL,data);

                return  result;
            }
        }

        RegisterUser ru = new RegisterUser();
        ru.execute(email,password);
    }

    @Override
    public void onComplete() {
        Toast.makeText(this,"Compelete", Toast.LENGTH_SHORT).show();
    }
}
