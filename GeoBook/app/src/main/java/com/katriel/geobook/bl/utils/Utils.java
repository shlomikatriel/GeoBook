package com.katriel.geobook.bl.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.tasks.Task;
import com.katriel.geobook.bl.entities.User;

import java.util.List;

public class Utils {

	// Log tag
	private static final String TAG = "Utils";

	// Permission request codes
	public static final int LOCATION_REQUEST_CODE = 0;

	public static void enableImmersiveMode(Activity activity) {
		// Declarations
		String activityName = activity.getClass().getSimpleName();
		final View DECOR_VIEW = activity.getWindow().getDecorView();
		final int CONFIGURATION = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
				| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
				| View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
				| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

		Log.i(TAG, "Setting immersive mode for activity '" + activityName + "'");
		DECOR_VIEW.setSystemUiVisibility(CONFIGURATION);

		Log.i(TAG, "Setting ui visibility change listener for activity '" + activityName + "' to restore immersive mode on keyboard events");
		DECOR_VIEW.setOnSystemUiVisibilityChangeListener
				(new View.OnSystemUiVisibilityChangeListener() {
					@Override
					public void onSystemUiVisibilityChange(int visibility) {
						DECOR_VIEW.setSystemUiVisibility(CONFIGURATION);
					}
				});
	}

	public static void setLinearLayoutParam(View view, int width, int height, float weight) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height, weight);
		view.setLayoutParams(params);
	}

	public static void setLinearLayoutParam(View view, int width, int height) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
		view.setLayoutParams(params);
	}

	public static void showToast(Context context, String text) {
		showToast(context, text, Toast.LENGTH_LONG);
	}

	public static void showToast(Context context, String text, int length) {
		Toast.makeText(context, text, length).show();
	}

	public static boolean syncOnTaskCompletion(Task<?> task, String taskName) {
		return syncOnTaskCompletion(task, taskName, 10000);
	}

	public static boolean syncOnTaskCompletion(Task<?> task, String taskName, long millis) {
		final long sleepInterval = 100;
		long before = System.currentTimeMillis();
		Log.v(TAG, "syncOnTaskCompletion: Current system millis " + before);
		Log.i(TAG, "syncOnTaskCompletion: Waiting up to " + millis + " for task '" + taskName + "' to complete.");
		for (long i = 0; i < millis && !task.isComplete(); i += sleepInterval) {
			try {
				Thread.sleep(sleepInterval);
			} catch (InterruptedException e) {
				Log.e(TAG, "syncOnTaskCompletion: Thread couldn't perform seep to wait for task '" + taskName + "' to complete", e);
			}
		}
		long after = System.currentTimeMillis();
		long time = after - before;
		Log.v(TAG, "syncOnTaskCompletion: Current system millis after waiting " + after);
		Log.v(TAG, "syncOnTaskCompletion: Waited " + time + " millis for task '" + taskName + "'");
		if (!task.isComplete()) {
			Log.e(TAG, "syncOnTaskCompletion: Task '" + taskName + "' didn't complete within " + millis + " millis");
			return false;
		}
		if (!task.isSuccessful()) {
			Log.e(TAG, "syncOnTaskCompletion: Task '" + taskName + "' completed but not successfully within " + millis + " millis");
			return false;
		}
		Log.i(TAG, "syncOnTaskCompletion: Task '" + taskName + "' completed  successfully after " + time + " millis");
		return true;
	}

	public static Location getBestLocationWithPermissionCheck(Activity activity, GoogleMap map) {
		if (android.os.Build.VERSION.SDK_INT >= 23) {
			Log.i(TAG, "Validating location permissions are granted, if not, requesting them");
			if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
					|| activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				Log.i(TAG, "Location permissions are not granted, requesting them by dialog");
				activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST_CODE);
				return null;
			}
		}

		Log.i(TAG, "Enabling location services");
		map.setMyLocationEnabled(true);

		Log.i(TAG, "Fetching best location from providers");
		LocationManager lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = lm.getProviders(true);
		Log.d(TAG, "Fetching best location from providers:\n" + providers);
		if (android.os.Build.VERSION.SDK_INT < 23) {
			Log.w(TAG, "getBestLocationWithPermissionCheck: User phone android version is less then marshmallow returning location from the first network provider");
			for (String provider : providers) {
				Location loc = lm.getLastKnownLocation(provider);
				if (loc != null)
					return loc;
			}
			Log.e(TAG,"getBestLocationWithPermissionCheck: No provider found that returned location");
			return null;
		}
		Location bestLocation = null;
		for (String provider : providers) {
			Location location = lm.getLastKnownLocation(provider);
			Log.d(TAG, "Provider '" + provider + "' accuracy is " + location.getAccuracy());
			if (location == null)
				continue;
			if (bestLocation == null || location.getAccuracy() > bestLocation.getAccuracy()) {
				bestLocation = location;
			}
		}
		Log.d(TAG, "Provider '" + bestLocation.getProvider() + "' is the most accurate with accuracy " + bestLocation.getAccuracy());
		Log.d(TAG, "Latitude: " + bestLocation.getLatitude() + " Longitude: " + bestLocation.getLongitude());
		return bestLocation;
	}

	public static int distance(double lat1, double lon1, double lat2, double lon2) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		dist = dist * 1.609344;
		return (int) (dist * 1000);
	}

	public static double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	public static double rad2deg(double rad) {
		return (rad * 180.0 / Math.PI);
	}

}
