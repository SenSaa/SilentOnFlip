package com.example.yusuf.silentonflip;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PathDashPathEffect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompatBase;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Button;

import java.io.IOException;

/** Service for silencing device on flipping, even when the activity stops (runs in the background). */

public class FlipService extends Service implements SensorEventListener {


    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float xAccel;
    private float yAccel;
    private float zAccel;

    AudioManager audioManager;

    public String flipStatus = "";

    boolean isFlipped;

    private Camera camera;
    private boolean isFlashOn;
    private Camera.Parameters params;
    private boolean isVibrateOn;
    private Handler vibrationHandler;
    private Handler flashHandler;
    private Handler flipHandler;

    NotificationManager notificationManager;


    Intent intent;
    public static final String BROADCAST_ACTION = "com.example.yusuf.silentonflip";


    @Override
    public void onCreate() {
        super.onCreate();

        // --------------------------------------------------------------------------- \\
        // ___ (1) create/instantiate intent. ___ \\
        intent = new Intent(BROADCAST_ACTION);
        // ___________________________________________________________________________ \\


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        showNotification();

        // --------------------------------------------------------------------------- \\
        // ___ (2.1) Instantiate SensorManager + Sensors + Register Sensors. ___ \\
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, 0);
        // ___________________________________________________________________________ \\

        GetCameraClass getCameraClassObj = new GetCameraClass();
        getCameraClassObj.start();

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        vibrationHandler = new Handler();
        flashHandler = new Handler();
        flipHandler = new Handler();


        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        turnOffFlash();

        if (camera != null) {
            camera.release();
            camera = null;
        }

        sensorManager.unregisterListener(this);

        dismissNotification();
    }


    // --------------------------------------------------------------------------- \\
    // ___ (2.2) Sensor Events. ___ \\
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            xAccel = event.values[0];
            yAccel = event.values[1];
            zAccel = event.values[2];

            onFlip();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    // ___________________________________________________________________________ \\


    // --------------------------------------------------------------------------- \\
    // ___ (3) Silence device on flip. ___ \\
    private void onFlip() {
        if (zAccel < -8) {

            Log.v("Z-axis","Flip");

            flipStatus = "Flipped!";
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);

            Handler duplicateSilence = new Handler();
            duplicateSilence.postDelayed(new Runnable() {
                @Override
                public void run() {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                }
            },100);

            // --- Limit Flash to a specific period of time. ---
            // Initially, while "isFlipped" flag is false, turn on the flash. Then after a specified delay, change the flag state whereby the flash method will stop being called.
            if (!isFlipped) {
                // When flipped - turn the flash on.
                try {

                    Log.v("TurnOnFlash","Called");

                    turnOnFlash();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // After a delay - change "isFlipped" flag state (which will flash method from being invoked).
                flipHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        Log.v("isFlipped Flag","Changed state");

                        isFlipped = true;
                    }
                }, 100);
            }
        }

        else {

            Log.v("Z-axis","UnFliped");

            flipStatus = "Unflipped";
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);

            // Reset "isFlipped" flag state.
            isFlipped = false;
        }

        broadcastFlippedState();

    }
    // ___________________________________________________________________________ \\


    // --------------------------------------------------------------------------- \\
    // ___ (3.1) Flash. ___ \\
    // Turning On flash
    private void turnOnFlash() throws IOException {
        if (!isFlashOn) {

            if (camera == null || params == null) {
                return;
            }

            try {
                camera.setPreviewTexture(new SurfaceTexture(0));

                Log.v("Camera","Preview Set");

            } catch (IOException e) {
                e.printStackTrace();
            }

            params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            camera.setParameters(params);
            camera.startPreview();
            isFlashOn = true;

            Log.v("Flash","Handled");

            flashHandler = new Handler();
            flashHandler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    Log.v("TurnOffFlash","Called");

                    turnOffFlash();
                }
            }, 200);


            turnOnVibration();
        }
    }

    // Turning Off flash
    private void turnOffFlash() {
        if (isFlashOn) {
            if (camera == null || params == null) {
                return;
            }

            params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
            camera.stopPreview();
            isFlashOn = false;
        }
    }
    // ___________________________________________________________________________ \\

    // --------------------------------------------------------------------------- \\
    // ___ (3.2) Vibration. ___ \\
    // Turning On flash
    // Controlling the Vibrator state (On/OFF).
    private void turnOnVibration() {
        if (!isVibrateOn) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(200);
            isVibrateOn = true;

            vibrationHandler = new Handler();
            vibrationHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    turnOffVibration();
                }
            }, 200);
        }
    }
    private void turnOffVibration() {
        if (isVibrateOn) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.cancel();
            isVibrateOn = false;
        }
    }
    // ___________________________________________________________________________ \\


    // --------------------------------------------------------------------------- \\
    // ___ (4) Get Camera. ___ \\
    // * This function is executed in another Thread due to its resource heavy-nature.
    public class GetCameraClass extends Thread {
        public GetCameraClass() {
        }
        @Override
        public void run() {
            getCamera();
        }
        // Get the camera
        private void getCamera() {
            if (camera == null) {
                try {
                    camera = Camera.open();
                    ///camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                    params = camera.getParameters();

                    Log.v("Camera","Open");

                } catch (RuntimeException e) {
                    Log.e("Camera Error. Failed to Open. Error: ", e.getMessage());
                }
            }
        }
    }
    // ______________________________________________________________________________________ \\


    // --------------------------------------------------------------------------- \\
    // _ Manage notification. _

    public void showNotification() {
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setContentTitle("SilentOnFlip");
        notificationBuilder.setContentText("To silence device, flip it over.");
        notificationBuilder.setSmallIcon(R.mipmap.nexus);
        notificationBuilder.setColor(Color.parseColor("#c80815"));
        int colorLED = Color.argb(255, 255, 0, 0);
        notificationBuilder.setLights(colorLED, 500, 500);
        // To  make sure that the Notification LED is triggered.
        notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
        notificationBuilder.setOngoing(true);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this,0,new Intent(),0);
        notificationBuilder.setContentIntent(resultPendingIntent);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(0,notificationBuilder.build());

    }

    public void dismissNotification() {
        notificationManager.cancel(0);
    }

    // ______________________________________________________________________________________ \\


    // --------------------------------------------------------------------------- \\
    // ___ (5) add  data to intent ___ \\
    private void broadcastFlippedState() {
        // add "flipStatus" flag to intent.
        /////intent.putExtra("Counted_Step", flipStatus);
        intent.putExtra("flip_state", String.valueOf(flipStatus));
        // call sendBroadcast with that intent  - which sends a message to whoever is registered to receive it.
        sendBroadcast(intent);
    }
    // ___________________________________________________________________________ \\

}

