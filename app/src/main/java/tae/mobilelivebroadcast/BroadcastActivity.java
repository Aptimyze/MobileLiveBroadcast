package tae.mobilelivebroadcast;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.oguzdev.circularfloatingactionmenu.library.FloatingActionButton;
import com.oguzdev.circularfloatingactionmenu.library.FloatingActionMenu;
import com.oguzdev.circularfloatingactionmenu.library.SubActionButton;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import tae.mobilelivebroadcast.util.Config;
import tae.mobilelivebroadcast.util.insertRedis;

public class BroadcastActivity extends Activity {

    private final String CLASS_LABEL = "BroadcastActivity";
    private final String LOG_TAG = CLASS_LABEL;

    /* Recording target URL */
//    private String ffmpeg_link = "/mnt/sdcard/stream.mp4";
    private String ffmpeg_link = "rtmp://taeheeid.cafe24.com:1935/myapp/"+ListRoomActivity.mEmail;

    //Camera Facing Control
    private int mCameraFacing;

    long startTime = 0;

    //state of camera and other options
    public boolean isRecording = false;
    private boolean isPreviewOn = false;
    boolean isFlashOn = false;
    boolean isMicOn = true;


    private FFmpegFrameRecorder recorder;
    insertRedis insertRedis;

    //camera quality setting
    private int sampleAudioRateInHz = 44100;
    private int imageWidth = 1280;
    private int imageHeight = 720;
    private int frameRate = 30;

    /* audio data getting thread */
    private AudioRecord audioRecord;
    private AudioRecordRunnable audioRecordRunnable;
    private Thread audioThread;
    volatile boolean runAudioThread = true;
    AudioManager audioManager;

    /* video data getting thread */
    private Camera cameraDevice;
    private CameraView cameraView;

    private Frame yuvImage = null;

    /* layout setting */
//    private final int bg_screen_bx = 232;
//    private final int bg_screen_by = 128;
//    private final int bg_screen_width = 700;
//    private final int bg_screen_height = 500;
//    private final int bg_width = 1123;
//    private final int bg_height = 715;
//    private final int live_width = 640;
//    private final int live_height = 480;
    private int screenWidth, screenHeight;
    private RelativeLayout topLayout;

    /* The number of seconds in the continuous record loop (or 0 to disable loop). */
    final int RECORD_LENGTH = 0;
    Frame[] images;
    long[] timestamps;
    ShortBuffer[] samples;
    int imagesIndex, samplesIndex;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.broadcast_layout);

        //First, camera facing back
        mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
        //for control audio mic
        audioManager= (AudioManager)getSystemService(AUDIO_SERVICE);

        insertRedis = new insertRedis();

        initLayout();
        initButtons();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        isRecording = false;
        audioManager.setMicrophoneMute(false);

        if (cameraView != null) {
            cameraView.stopPreview();
        }

        if(cameraDevice != null) {
            cameraDevice.stopPreview();
            cameraDevice.release();
            cameraDevice = null;
        }
    }


    private void initLayout() {
        /* get size of screen */
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        screenWidth = display.getWidth();
        screenHeight = display.getHeight();

        /* camera view size */
//        int display_width_d = (int) (1.0 * bg_screen_width * screenWidth / bg_width);
//        int display_height_d = (int) (1.0 * bg_screen_height * screenHeight / bg_height);
//        int prev_rw, prev_rh;
//        if (1.0 * display_width_d / display_height_d > 1.0 * live_width / live_height) {
//            prev_rh = display_height_d;
//            prev_rw = (int) (1.0 * display_height_d * live_width / live_height);
//        } else {
//            prev_rw = display_width_d;
//            prev_rh = (int) (1.0 * display_width_d * live_height / live_width);
//        }
//        RelativeLayout.LayoutParams layoutParam = new RelativeLayout.LayoutParams(prev_rw, prev_rh);
//        layoutParam.topMargin = (int) (1.0 * bg_screen_by * screenHeight / bg_height);
//        layoutParam.leftMargin = (int) (1.0 * bg_screen_bx * screenWidth / bg_width);

        //main layout set
        topLayout = (RelativeLayout)findViewById(R.id.record_layout);

        //start Camera
        cameraDevice = Camera.open(mCameraFacing);
        Log.i(LOG_TAG, "camera open");
        cameraView = new CameraView(this, cameraDevice, mCameraFacing);

        //camera view set
        RelativeLayout.LayoutParams layoutParam = new RelativeLayout.LayoutParams(screenWidth, screenHeight);
        topLayout.addView(cameraView, layoutParam);
        Log.i(LOG_TAG, "cameara preview start: OK");

        //recording control imageview
        final ImageView recordImage = new ImageView(this);
        recordImage.setImageResource(R.drawable.record_bf);
        recordImage.setMaxWidth(30);
        recordImage.setMaxHeight(30);
        recordImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    startRecording();
                    Log.w(LOG_TAG, "Start Button Pushed");
                    recordImage.setImageResource(R.drawable.record_af);

                    //리스트에 방송방 보이기 시작
                    long now = System.currentTimeMillis();
                    Date date = new Date(now);
                    SimpleDateFormat curDateFormat = new SimpleDateFormat("yyyyMMddmm");
                    String strCurrent = curDateFormat.format(date);
                    insertRedis.init(getApplicationContext(), Config.INSERT_REDIS_URL, Config.RKEY_LIST, ListRoomActivity.mEmail + "/" + ListRoomActivity.mEmail + "'s Room/" + strCurrent);

                } else {
                    // This will trigger the audio recording loop to stop and then set isRecorderStart = false;
                    stopRecording();
                    Log.w(LOG_TAG, "Stop Button Pushed");
                    recordImage.setImageResource(R.drawable.record_bf);

                    //리스트에 방송방 보이기 삭제
                    insertRedis.init(getApplicationContext(), Config.QUIT_BROAD_URL, Config.RKEY_LIST, ListRoomActivity.mEmail);
                }
            }
        });

        /* add recording imageview to main layout */
        RelativeLayout.LayoutParams layoutParam2 = new RelativeLayout.LayoutParams(150,150);
        layoutParam2.addRule(RelativeLayout.CENTER_VERTICAL, 1);
        topLayout.addView(recordImage, layoutParam2);

        //resize cameraview
//        topLayout.addView(cameraView, layoutParam);

    }

    private void initButtons(){
        //Sub Buttons
        SubActionButton.Builder itemBuilder = new SubActionButton.Builder(this);
        ImageView iconLight = new ImageView(this);
        iconLight.setImageResource(R.drawable.lightbulb);
        iconLight.setMaxWidth(25);
        iconLight.setMaxHeight(25);
        SubActionButton lightBtn = itemBuilder.setContentView(iconLight).build();

        final ImageView iconMute = new ImageView(this);
        iconMute.setImageResource(R.drawable.sound);
        iconMute.setMaxWidth(25);
        iconMute.setMaxHeight(25);
        SubActionButton muteBtn = itemBuilder.setContentView(iconMute).build();

        final ImageView iconFaceChange = new ImageView(this);
        iconFaceChange.setImageResource(R.drawable.changer);
        iconFaceChange.setMaxWidth(25);
        iconFaceChange.setMaxHeight(25);
        SubActionButton faceChangeBtn = itemBuilder.setContentView(iconFaceChange).build();

        //Floating menu button
        final ImageView iconMain = new ImageView(this);
        iconMain.setImageResource(R.drawable.plus_symbol);
        iconMain.setMaxWidth(30);
        iconMain.setMaxHeight(30);
        final FloatingActionButton actionButton = new FloatingActionButton.Builder(this)
                .setContentView(iconMain).build();

        final Camera.Parameters mCameraParameter;
        mCameraParameter = cameraDevice.getParameters();

        lightBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isFlashOn){
                    mCameraParameter.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    cameraDevice.setParameters(mCameraParameter);
                    isFlashOn = true;
                }else if(isFlashOn){
                    mCameraParameter.setFlashMode("off");
                    cameraDevice.setParameters(mCameraParameter);
                    isFlashOn = false;
                }
            }
        });

        muteBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isMicOn){
                    audioManager.setMicrophoneMute(true);
                    isMicOn=false;
                    iconMute.setImageResource(R.drawable.lightbulb);
                }else if(!isMicOn) {
                    audioManager.setMicrophoneMute(false);
                    isMicOn=true;
                    iconMute.setImageResource(R.drawable.sound);
                }

            }
        });

        faceChangeBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mCameraFacing==Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mCameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
                    isPreviewOn = false;
                    if (cameraView != null) {
                        cameraView.stopPreview();
                    }
                    if(cameraDevice != null) {
                        cameraDevice.stopPreview();
                        cameraDevice.release();
                        cameraDevice = null;
                    }
                    topLayout.removeView(cameraView);
                    initLayout();

                } else {
                    mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
                    isPreviewOn = false;
                    if (cameraView != null) {
                        cameraView.stopPreview();
                    }
                    if(cameraDevice != null) {
                        cameraDevice.stopPreview();
                        cameraDevice.release();
                        cameraDevice = null;
                    }
                    topLayout.removeView(cameraView);
                    initLayout();
                }

            }
        });


        FloatingActionMenu actionMenu = new FloatingActionMenu.Builder(this)
                .addSubActionView(lightBtn).addSubActionView(muteBtn).addSubActionView(faceChangeBtn)
                .attachTo(actionButton).build();

        // Listen menu open and close events to animate the button content view
        actionMenu.setStateChangeListener(new FloatingActionMenu.MenuStateChangeListener() {
            @Override
            public void onMenuOpened(FloatingActionMenu menu) {
                // Rotate the icon of rightLowerButton 45 degrees clockwise
                actionButton.setRotation(0);
                PropertyValuesHolder pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, 45);
                ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(actionButton, pvhR);
                animation.start();
            }

            @Override
            public void onMenuClosed(FloatingActionMenu menu) {
                // Rotate the icon of rightLowerButton 45 degrees counter-clockwise
                actionButton.setRotation(45);
                PropertyValuesHolder pvhR = PropertyValuesHolder.ofFloat(View.ROTATION, 0);
                ObjectAnimator animation = ObjectAnimator.ofPropertyValuesHolder(actionButton, pvhR);
                animation.start();
            }
        });
    }

    //---------------------------------------
    // initialize ffmpeg_recorder
    //---------------------------------------
    private void initRecorder() {

        Log.w(LOG_TAG, "init recorder");

        if(yuvImage == null){
            yuvImage = new Frame(imageWidth, imageHeight, Frame.DEPTH_UBYTE, 2);
            Log.i(LOG_TAG, "create yuvImage");
        }

        Log.i(LOG_TAG, "ffmpeg_url: " + ffmpeg_link);
        recorder = new FFmpegFrameRecorder(ffmpeg_link, imageWidth, imageHeight, 1);
        recorder.setFormat("flv");
        recorder.setSampleRate(sampleAudioRateInHz);
        // Set in the surface changed method
        recorder.setFrameRate(frameRate);

        Log.i(LOG_TAG, "recorder initialize success");

        audioRecordRunnable = new AudioRecordRunnable();
        runAudioThread = true;
        audioThread = new Thread(audioRecordRunnable);

    }

    public void startRecording() {

        initRecorder();

        try {
            recorder.start();
            startTime = System.currentTimeMillis();
            isRecording = true;
            audioThread.start();

        } catch (FFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
        }
    }


    public void stopRecording() {

        runAudioThread = false;
        try {
            audioThread.join();
        } catch (InterruptedException e) {
            // reset interrupt to be nice
            Thread.currentThread().interrupt();
            return;
        }
        audioRecordRunnable = null;
        audioThread = null;

        if (recorder != null && isRecording) {

            isRecording = false;
            Log.v(LOG_TAG,"Finishing recording, calling stop and release on recorder");
            try {
                recorder.stop();
                recorder.release();
            } catch (FFmpegFrameRecorder.Exception e) {
                e.printStackTrace();
            }
            recorder = null;

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isRecording) {
                stopRecording();
            }

            finish();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }


    //---------------------------------------------
    // audio thread, gets and encodes audio data
    //---------------------------------------------
    class AudioRecordRunnable implements Runnable {

        @Override
        public void run() {

            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            // Audio
            int bufferSize;
            ShortBuffer audioData;
            int bufferReadResult;

            bufferSize = AudioRecord.getMinBufferSize(sampleAudioRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleAudioRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            audioData = ShortBuffer.allocate(bufferSize);

            Log.d(LOG_TAG, "audioRecord.startRecording()");
            audioRecord.startRecording();

            /* ffmpeg_audio encoding loop */
            while (runAudioThread) {

                //Log.v(LOG_TAG,"recording? " + recording);
                bufferReadResult = audioRecord.read(audioData.array(), 0, audioData.capacity());
                audioData.limit(bufferReadResult);
                if (bufferReadResult > 0) {
//                    Log.v(LOG_TAG,"bufferReadResult: " + bufferReadResult);
                    // If "recording" isn't true when start this thread, it never get's set according to this if statement...!!!
                    // Why?  Good question...
                    if (isRecording) {
//                        if (RECORD_LENGTH <= 0)
                        try {
                            recorder.recordSamples(audioData);
                            //Log.v(LOG_TAG,"recording " + 1024*i + " to " + 1024*i+1024);
                        } catch (FFmpegFrameRecorder.Exception e) {
                            Log.v(LOG_TAG,e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
            Log.v(LOG_TAG,"AudioThread Finished, release audioRecord");

            /* encoding finish, release recorder */
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                Log.v(LOG_TAG,"audioRecord released");
            }
        }
    }

    //---------------------------------------------
    // camera thread, gets and encodes video data
    //---------------------------------------------
    class CameraView extends SurfaceView implements SurfaceHolder.Callback, PreviewCallback {

        private SurfaceHolder mHolder;
        private Camera mCamera;

        public CameraView(Context context, Camera camera, int cameraFacing) {
            super(context);
            Log.w("camera", "camera view");
            mCameraFacing = cameraFacing;
            mCamera = camera;
            mHolder = getHolder();
            mHolder.addCallback(CameraView.this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            mCamera.setPreviewCallback(CameraView.this);
            Log.w("camera", "camera view2");
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                stopPreview();
                mCamera.setPreviewDisplay(holder);
            } catch (IOException exception) {
                mCamera.release();
                mCamera = null;
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            stopPreview();

            Camera.Parameters camParams = mCamera.getParameters();
            List<Camera.Size> sizes = camParams.getSupportedPreviewSizes();
            // Sort the list in ascending order
            Collections.sort(sizes, new Comparator<Camera.Size>() {

                public int compare(final Camera.Size a, final Camera.Size b) {
                    return a.width * a.height - b.width * b.height;
                }
            });

            // Pick the first preview size that is equal or bigger, or pick the last (biggest) option if we cannot
            // reach the initial settings of imageWidth/imageHeight.
            for (int i = 0; i < sizes.size(); i++) {
                if ((sizes.get(i).width >= imageWidth && sizes.get(i).height >= imageHeight) || i == sizes.size() - 1) {
                    imageWidth = sizes.get(i).width;
                    imageHeight = sizes.get(i).height;
                    Log.v(LOG_TAG, "Changed to supported resolution: " + imageWidth + "x" + imageHeight);
                    break;
                }
            }
            camParams.setPreviewSize(imageWidth, imageHeight);

            Log.v(LOG_TAG,"Setting imageWidth: " + imageWidth + " imageHeight: " + imageHeight + " frameRate: " + frameRate);

            camParams.setPreviewFrameRate(frameRate);
            Log.v(LOG_TAG,"Preview Framerate: " + camParams.getPreviewFrameRate());

            mCamera.setParameters(camParams);

            // Set the holder (which might have changed) again
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.setPreviewCallback(CameraView.this);
                startPreview();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Could not set preview display in surfaceChanged");
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            try {
                mHolder.addCallback(null);
                mCamera.setPreviewCallback(null);
            } catch (RuntimeException e) {
                // The camera has probably just been released, ignore.
            }
        }

        public void startPreview() {
            if (!isPreviewOn && mCamera != null) {
                isPreviewOn = true;
                mCamera.startPreview();
            }
        }

        public void stopPreview() {
            if (isPreviewOn && mCamera != null) {
                isPreviewOn = false;
                mCamera.stopPreview();
            }
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (audioRecord == null || audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                startTime = System.currentTimeMillis();
                return;
            }
            /* get video data */
            if (yuvImage != null && isRecording) {
                ((ByteBuffer)yuvImage.image[0].position(0)).put(data);

                try {
//                    Log.v(LOG_TAG,"Writing Frame");
                    long t = 1000 * (System.currentTimeMillis() - startTime);
                    if (t > recorder.getTimestamp()) {
                        recorder.setTimestamp(t);
                    }
                    recorder.record(yuvImage);
                } catch (FFmpegFrameRecorder.Exception e) {
                    Log.v(LOG_TAG,e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}