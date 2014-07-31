package com.kiof.flashlight;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class Flashlight extends Activity {
	private Context mContext;
	private SharedPreferences mSharedPreferences;
	private ViewSwitcher mViewSwitcher;
	private Camera mCamera;

	private static final String SOUND = "sound";
	private static final String AUTOWIDGET = "autowidget";
	private static final String WIDGET = "widget";
	private static int RETURN_SETTING = 1;
	private static final String TAG = "FLASHLIGHT";

	/** Check if this device has a camera */
	private boolean checkCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

	/** A safe way to get an instance of the Camera object. */
	public static Camera getCameraInstance() {
		Camera c = null;
		try {
			c = Camera.open(); // attempt to get a Camera instance
		} catch (Exception e) {
			// Camera is not available (in use or does not exist)
		}
		return c; // returns null if camera is unavailable
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// super.onCreate(savedInstanceState);
		super.onCreate(savedInstanceState);

		mContext = getApplicationContext();
		// Get Preferences
		PreferenceManager.setDefaultValues(mContext, R.xml.setting, false);
		mSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(mContext);

		setContentView(R.layout.main);

        AdView adView = (AdView) this.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
//                .addTestDevice("53356E870D99B80A68F8E2DBBFCD28FB")
                .build();
        adView.loadAd(adRequest);

        mViewSwitcher = (ViewSwitcher) findViewById(R.id.profileSwitcher);

		// Display change log if new version
		ChangeLog cl = new ChangeLog(this);
		if (cl.firstRun())
			new HtmlAlertDialog(this, R.raw.about,
					getString(R.string.about_title),
					android.R.drawable.ic_menu_info_details).show();

		// Background music
		if (mSharedPreferences.getBoolean(SOUND, false))
			playSound(R.raw.bgmusic);

		// Auto Widget
		if (mSharedPreferences.getBoolean(AUTOWIDGET, false)) {
			Intent intent = this.getIntent();
			// Log.d(TAG, "Widget : " + intent.getBooleanExtra(WIDGET, false));
			if (intent != null && intent.getBooleanExtra(WIDGET, false)) {
				View view = findViewById(R.id.buttonframe);
				view.performClick();
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
			startActivityForResult(new Intent(Flashlight.this, Setting.class),
					RETURN_SETTING);
			return true;
		case R.id.share:
			Intent sharingIntent = new Intent(Intent.ACTION_SEND);
			sharingIntent.setType("text/plain");
			sharingIntent.putExtra(Intent.EXTRA_TITLE,
					getString(R.string.share_title));
			sharingIntent.putExtra(Intent.EXTRA_SUBJECT,
					getString(R.string.share_title));
			sharingIntent.putExtra(Intent.EXTRA_TEMPLATE,
					Html.fromHtml(getString(R.string.share_link)));
			sharingIntent.putExtra(Intent.EXTRA_TEXT,
					Html.fromHtml(getString(R.string.share_link)));
			startActivity(Intent.createChooser(sharingIntent,
					getString(R.string.share_with)));
			return true;
		case R.id.about:
			new HtmlAlertDialog(this, R.raw.about,
					getString(R.string.about_title),
					android.R.drawable.ic_menu_info_details).show();
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

	@Override
	public void onPause() {
		super.onPause();
		setBrightness(-1f);
		if (mCamera != null) {
			mCamera.release();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		// setBrightness(1f);
	}
	
	public void switchOn(View view) {
		Toast.makeText(getApplicationContext(), "On", Toast.LENGTH_SHORT).show();
		setBrightness(1f);
		if (mSharedPreferences.getBoolean(SOUND, false)) playSound(R.raw.on);
		mViewSwitcher.showPrevious();
		if (checkCameraHardware(mContext)) {
			mCamera = getCameraInstance();
			if (mCamera != null) {
				Parameters params = mCamera.getParameters();
				params.setFlashMode(Parameters.FLASH_MODE_TORCH);
				mCamera.setParameters(params);
				mCamera.startPreview();
			}
		}
	}

	public void switchOff(View view) {
		Toast.makeText(getApplicationContext(), "Off", Toast.LENGTH_SHORT).show();
		setBrightness(-1f);
		if (mSharedPreferences.getBoolean(SOUND, false)) playSound(R.raw.off);
		mViewSwitcher.showPrevious();
		if (checkCameraHardware(mContext)) {
			if (mCamera != null) {
				mCamera.stopPreview();
				mCamera.release();
			}
		}
	}

	int playSound(int soundId) {
		MediaPlayer mp = MediaPlayer.create(mContext, soundId);
		if (mp == null)
			return 0;
		mp.setOnCompletionListener(new OnCompletionListener() {
			public void onCompletion(MediaPlayer mp) {
				mp.release();
			};
		});
		mp.start();
		return mp.getDuration();
	}

	private void setBrightness(float brightness) {
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = brightness;
		getWindow().setAttributes(lp);
	}


}