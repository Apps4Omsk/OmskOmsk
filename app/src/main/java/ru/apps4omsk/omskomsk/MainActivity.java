package ru.apps4omsk.omskomsk;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";

    private static final String KEY_IN_RESOLUTION = "is_in_resolution";

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    @InjectView(R.id.main_min_counter) TextView minCounter;

    private GoogleApiClient googleApiClient;
    private MainHelper mainHelper;
    private Handler handler;
    private Runnable updateCounterRunnable;
    private long timeOfLastCheckInMs;

    /**
     * Determines if the client is in a resolution state, and
     * waiting for resolution intent to return.
     */
    private boolean isInResolution;

    /**
     * Called when the activity is starting. Restores the activity state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);
        if (savedInstanceState != null) {
            isInResolution = savedInstanceState.getBoolean(KEY_IN_RESOLUTION, false);
        }
        mainHelper = new MainHelper(this);
        handler = new Handler(Looper.myLooper());
        updateCounterRunnable = new Runnable() {
            @Override
            public void run() {
                updateMinCounter();
                handler.postDelayed(updateCounterRunnable, 100);
            }
        };
    }

    /**
     * Called when the Activity is made visible.
     * A connection to Play Services need to be initiated as
     * soon as the activity is visible. Registers {@code ConnectionCallbacks}
     * and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onStart() {
        super.onStart();
        setTitle(R.string.main_activity_title);
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    // Optionally, add additional APIs and scopes if required.
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        googleApiClient.connect();
        timeOfLastCheckInMs = mainHelper.getTimeOfLastCheckInMs();
        if (timeOfLastCheckInMs == -1) {
            timeOfLastCheckInMs = getCurrentTimeMs();
            mainHelper.updateTimeOfLastCheckInMs(timeOfLastCheckInMs);
            Toast.makeText(this, "Вы в игре!", Toast.LENGTH_LONG);
        }
        handler.post(updateCounterRunnable);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch(keyCode){
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                timeOfLastCheckInMs = -1;
                mainHelper.updateTimeOfLastCheckInMs(timeOfLastCheckInMs);
                Toast.makeText(MainActivity.this, "Вы покинули Омск!", Toast.LENGTH_SHORT).show();
                break;
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (timeOfLastCheckInMs == -1) {
                    timeOfLastCheckInMs = getCurrentTimeMs();
                    mainHelper.updateTimeOfLastCheckInMs(timeOfLastCheckInMs);
                    Toast.makeText(MainActivity.this, "Вы снова в игре!", Toast.LENGTH_SHORT).show();
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Called when activity gets invisible. Connection to Play Services needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    protected void onStop() {
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }
        handler.removeCallbacks(updateCounterRunnable);
        super.onStop();
    }

    /**
     * Saves the resolution state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IN_RESOLUTION, isInResolution);
    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case REQUEST_CODE_RESOLUTION:
            retryConnecting();
            break;
        }
    }

    private void retryConnecting() {
        isInResolution = false;
        if (!googleApiClient.isConnecting()) {
            googleApiClient.connect();
        }
    }

    /**
     * Called when {@code googleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        // TODO: Start making API requests.
    }

    /**
     * Called when {@code googleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
        retryConnecting();
    }

    /**
     * Called when {@code googleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                    result.getErrorCode(), this, 0, new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    retryConnecting();
                }
            }).show();
            return;
        }
        // If there is an existing resolution error being displayed or a resolution
        // activity has started before, do nothing and wait for resolution
        // progress to be completed.
        if (isInResolution) {
            return;
        }
        isInResolution = true;
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
            retryConnecting();
        }
    }

    private void updateMinCounter() {
        if (timeOfLastCheckInMs == -1) {
            minCounter.setText("Вернитесь в Омск, чтобы продолжить игру!");
        } else {
            long diff = getCurrentTimeMs() - timeOfLastCheckInMs;
            double diffInMins = diff / (1000.0 * 60);
            minCounter.setText(String.format("Вы в Омске, уже %.2f минут", diffInMins));
        }
    }

    private long getCurrentTimeMs() {
        return System.currentTimeMillis();
    }
}
