package tae.mobilelivebroadcast.util;

/**
 * Created by Tae on 2016-05-07.
 */
public class Config {
    //URL to our login.php file
    public static final String LOGIN_URL = "http://52.79.179.155/mobileBroadcast/login.php";
    public static final String REGISTER_URL = "http://52.79.179.155/mobileBroadcast/register.php";
    public static final String INSERT_REDIS_URL = "http://52.79.179.155/mobileBroadcast/redis.php";
    public static final String EMAILCHECK_URL = "http://52.79.179.155/mobileBroadcast/idCheck.php";
    public static final String GETLIST_URL = "http://52.79.179.155/mobileBroadcast/testRedis.php";
    public static final String QUIT_BROAD_URL = "http://52.79.179.155/mobileBroadcast/quit_broad.php";
    public static final String ROOM_COUNT_URL = "http://52.79.179.155/mobileBroadcast/readCount.php";

    //URL to broadcast
    public static final String BROADCAST = "rtmp://taeheeid.cafe24.com/myapp/";

    //Redis Key List
    public static final String RKEY_LIST = "list";
    public static final String RKEY_ROOM_MEMBER ="room_member";

    //MySQL Key List
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";

    //If server response is equal to this that means login is successful
    public static final String LOGIN_SUCCESS = "success";

    //Keys for Sharedpreferences
    //This would be the name of our shared preferences
    public static final String SHARED_PREF_NAME = "myloginapp";

    //This would be used to store the email of current logged in user
    public static final String EMAIL_SHARED_PREF = "email";
    public static final String ROOM_TITLE_SHARED_PREF = "room_title";

    //We will use this to store the boolean in sharedpreference to track user is loggedin or not
    public static final String LOGGEDIN_SHARED_PREF = "loggedin";
}