package com.kiof.flashlight;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.IOException;
import java.util.List;

public final class Flashlight extends AppCompatActivity implements SurfaceHolder.Callback {
    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private Camera mCamera;
    private SurfaceHolder mHolder;
    private ImageView mButton;
    private AdView mAdView;

    private static final String SOUND = "sound";
    private static final String AUTOLIGHT = "autolight";
    private static final String TAG = "FLASHLIGHT";
    private static final int REQUEST_CAMERA = 1;

    private static final int COLOR_DARK = 0x00000000;
    private static final int COLOR_LIGHT = 0xFFFFFFFF;

    private boolean lightOn;
    private boolean useCamera;

    private PowerManager.WakeLock wakeLock;

    private static Flashlight flashlight;

    public Flashlight() {
        super();
        flashlight = this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getApplicationContext();
        setContentView(R.layout.main);

//        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(myToolbar);

        // Get Preferences
        PreferenceManager.setDefaultValues(mContext, R.xml.setting, false);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        // AdMob
        MobileAds.initialize(getApplicationContext(), getString(R.string.ad_unit_id));
        mAdView = (AdView) this.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
//                .addTestDevice("53356E870D99B80A68F8E2DBBFCD28FB")
                .build();
        mAdView.loadAd(adRequest);

        mButton = (ImageView) this.findViewById(R.id.button);

        SurfaceView surfaceView = (SurfaceView) this.findViewById(R.id.surfaceview);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
//		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // Disable Phone Sleep
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Display change log if new version
        ChangeLog cl = new ChangeLog(this);
        if (cl.firstRun())
            new HtmlAlertDialog(this, R.raw.about,
                    getString(R.string.about_title),
                    android.R.drawable.ic_menu_info_details).show();

        // Background music
        if (mSharedPreferences.getBoolean(SOUND, false))
            playSound(R.raw.bgmusic);

        // Check if this device has a camera flash
        if (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            // Manage camera permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
            } else {
                useCamera = true;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.setting:
                startActivityForResult(new Intent(Flashlight.this, Setting.class), 1);
                return true;
            case R.id.share:
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.share_title));
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_title));
                sharingIntent.putExtra(Intent.EXTRA_TEMPLATE, Html.fromHtml(getString(R.string.share_link)));
                sharingIntent.putExtra(Intent.EXTRA_TEXT, Html.fromHtml(getString(R.string.share_link)));
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_with)));
                return true;
            case R.id.about:
                new HtmlAlertDialog(this, R.raw.about, getString(R.string.about_title), android.R.drawable.ic_menu_info_details).show();
                return true;
            case R.id.other:
                Intent otherIntent = new Intent(Intent.ACTION_VIEW);
                otherIntent.setData(Uri.parse(getString(R.string.other_link)));
                startActivity(otherIntent);
                return true;
            case R.id.quit:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void toggleLight(View view) {
        if (lightOn) {
            turnLightOff();
        } else {
            turnLightOn();
        }
    }

    private void turnLightOn() {
//		Toast.makeText(getApplicationContext(), "On", Toast.LENGTH_SHORT).show();

        if (useCamera) {
            if (mCamera == null) {
                mCamera = getCameraInstance();
                if (mCamera != null && mHolder != null) {
                    try {
                        mCamera.setPreviewDisplay(mHolder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (mCamera != null) {
                Log.d(TAG, "mCamera != null");
                Parameters parameters = mCamera.getParameters();
                if (parameters != null) {
                    Log.d(TAG, "parameters != null");
                    List<String> flashModes = parameters.getSupportedFlashModes();
                    // Check if camera flash exists
                    if (flashModes != null) {
                        Log.d(TAG, "flashModes != null");
                        String flashMode = parameters.getFlashMode();
                        if (!Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                            Log.d(TAG, "!Parameters.FLASH_MODE_TORCH.equals(flashMode)");
                            // Turn on the flash
                            if (flashModes.contains(Parameters.FLASH_MODE_TORCH)) {
                                Log.d(TAG, "flashModes.contains(Parameters.FLASH_MODE_TORCH)");
                                parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                                mCamera.setParameters(parameters);
                                mCamera.startPreview();
                                if (mSharedPreferences.getBoolean(SOUND, false)) playSound(R.raw.on);
                                startWakeLock();
                                mButton.setImageResource(R.drawable.buttonon);
                                lightOn = true;
                                return;
                            }
                        }
                    }
                }
            }
        }

        // Use the screen as a flashlight (next best thing)
        Log.d(TAG, "FLASH_MODE_TORCH not supported");
        mButton.setBackgroundColor(COLOR_LIGHT);
        setBrightness(1f);
        if (mSharedPreferences.getBoolean(SOUND, false)) playSound(R.raw.on);
        startWakeLock();
        mButton.setImageResource(R.drawable.buttonon);
        lightOn = true;
    }

    private void turnLightOff() {
//        Toast.makeText(getApplicationContext(), "Off", Toast.LENGTH_SHORT).show();

        if (lightOn) {
            if (useCamera) {
                if (mCamera != null) {
                    Parameters parameters = mCamera.getParameters();
                    if (parameters != null) {
                        List<String> flashModes = parameters.getSupportedFlashModes();
                        String flashMode = parameters.getFlashMode();
                        // Check if camera flash exists
                        if (flashModes != null) {
                            if (!Parameters.FLASH_MODE_OFF.equals(flashMode)) {
                                // Turn off the flash
                                if (flashModes.contains(Parameters.FLASH_MODE_OFF)) {
                                    parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                                    mCamera.setParameters(parameters);
                                    mCamera.stopPreview();
                                    if (mSharedPreferences.getBoolean(SOUND, false)) playSound(R.raw.off);
                                    stopWakeLock();
                                    mButton.setImageResource(R.drawable.buttonoff);
                                    lightOn = false;
                                    return;
                                }
                            }
                        }
                    }
                }
            }

            Log.d(TAG, "FLASH_MODE_OFF not supported");
            mButton.setBackgroundColor(COLOR_DARK);
            setBrightness(-1f);
            if (mSharedPreferences.getBoolean(SOUND, false)) playSound(R.raw.off);
            stopWakeLock();
            mButton.setImageResource(R.drawable.buttonoff);
            lightOn = false;
        }
    }

    int playSound(int soundId) {
        MediaPlayer mp = MediaPlayer.create(mContext, soundId);
        if (mp == null)
            return 0;
        mp.setOnCompletionListener(new OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }

            ;
        });
        mp.start();
        return mp.getDuration();
    }

    private void setBrightness(float brightness) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightness;
        getWindow().setAttributes(lp);
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            // attempt to get a Camera instance
            c = Camera.open();
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }
        // returns null if camera is unavailable
        return c;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    useCamera = true;
//                else useCamera = false;
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mSharedPreferences.getBoolean(AUTOLIGHT, false)) turnLightOn();
        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onPause() {
        turnLightOff();
        if (mAdView != null) {
            mAdView.pause();
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        if (mAdView != null) {
            mAdView.destroy();
        }
        flashlight = null;
        super.onDestroy();
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        // When the search button is long pressed, quit
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            finish();
            return true;
        }
        return false;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder = null;
    }

    private void startWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        wakeLock.acquire();
    }

    private void stopWakeLock() {
        if (wakeLock != null) {
            wakeLock.release();
        }
    }


}