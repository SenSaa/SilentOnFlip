package com.example.yusuf.silentonflip;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends Activity {

    private Button silentBtn;
    private TextView flipTxV;

    String flipStatus = "";

    private Button stopButton;

    boolean isServiceStopped;


    private static final String TAG = "FlipState";
    private Intent intent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        silentBtn = (Button)findViewById(R.id.silentBtn);
        silentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // start Service.
                startService(intent);
                // register our BroadcastReceiver by passing in an IntentFilter. * identifying the message that is broadcasted by using static string "BROADCAST_ACTION".
                registerReceiver(broadcastReceiver, new IntentFilter(FlipService.BROADCAST_ACTION));
                isServiceStopped = false;
            }
        });

        flipTxV = (TextView)findViewById(R.id.flipTxV);
        flipTxV.setText(flipStatus);

        isServiceStopped = true;

        stopButton = (Button)findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Utilise a boolean variable to - Prevent "IllegalArgumentException" <- which is caused by unregistering the receiver when it is not registered yet (pressing stop service button twice).
                if (!isServiceStopped) {
                    // call unregisterReceiver - to stop listening for broadcasts.
                    unregisterReceiver(broadcastReceiver);
                    // stop Service.
                    stopService(intent);
                    isServiceStopped = true;
                }
            }
        });

        // --------------------------------------------------------------------------- \\
        // ___ (1) create/instantiate intent ___ \\
        //  Instantiate the intent declared globally - which will be passed to startService and stopService.
        intent = new Intent(this, FlipService.class);
        // ___________________________________________________________________________ \\

    }


    // --------------------------------------------------------------------------- \\
    // ___ (2) create Broadcast Receiver ___ \\
    // create a BroadcastReceiver - to receive the message that is going to be broadcast from the Service
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // call updateViews passing in our intent which is holding the data to display.
            updateViews(intent);
        }
    };
    // ___________________________________________________________________________ \\


    @Override
    protected void onResume() {
        super.onResume();

        hideNavBar();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void hideNavBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }


    // --------------------------------------------------------------------------- \\
    // ___ (5) retrieve data from intent & set data to textviews __ \\
    private void updateViews(Intent intent) {
        // retrieve data out of the intent.
        String flipStatus = intent.getStringExtra("flip_state");

        // set textviews to the data retrieved from the intents.
        flipTxV.setText(flipStatus);

    }

}
