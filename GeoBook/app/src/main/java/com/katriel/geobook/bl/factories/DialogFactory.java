package com.katriel.geobook.bl.factories;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.katriel.geobook.MapsActivity;
import com.katriel.geobook.bl.utils.AuthUtils;
import com.katriel.geobook.bl.utils.DbUtils;
import com.katriel.geobook.bl.utils.Utils;

public class DialogFactory {

	// Log tag
	private static final String TAG = "DialogFactory";

	public static AlertDialog createLocationPermissionIsNeeded(final Activity activity) {

		Log.i(TAG, "createLocationPermissionIsNeeded: Creating 'LocationPermissionIsNeeded' dialog for activity '" + activity.getLocalClassName() + "'");
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setMessage("נדרשת הרשאת מיקום על מנת להפעיל את האפליקציה.")
				.setCancelable(false)
				.setPositiveButton("אישור", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						activity.finish();
					}
				});
		return builder.create();
	}

	public static AlertDialog createUserRemovalConfirmation(final Activity activity) {
		Log.i(TAG, "createUserRemovalConfirmation: Creating 'UserRemovalConfirmation' dialog for activity '" + activity.getLocalClassName() + "'");

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					boolean success = AuthUtils.removeCurrentUser();
					if (success)
						activity.finish();
				}
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(activity)
				.setMessage("האם אתה בטוח שתרצה למחוק את המשתמש?\nפעולה זו הינה לצמיתות.").setCancelable(false)
				.setPositiveButton("כן", dialogClickListener)
				.setNegativeButton("לא", dialogClickListener);
		return builder.create();
	}

	public static AlertDialog createAddPostDialog(final String uid, final MapsActivity mapsActivity, final GoogleMap mMap) {
		Log.i(TAG, "createAddPostDialog: Creating new post for user '" + uid + "'");
		final EditText input = new EditText(mapsActivity);
		input.setInputType(InputType.TYPE_CLASS_TEXT);

		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					String content = input.getText().toString();
					if (content.isEmpty()) {
						Log.w(TAG, "createAddPostDialog: Post is empty, canceling post");
						Utils.showToast(mapsActivity, "לא ניתן לשלוח פוסט ללא טקסט");
						return;
					}
					DbUtils.addPost(content, mapsActivity, mMap);
					Location location = Utils.getBestLocationWithPermissionCheck(mapsActivity, mMap);
					LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
					mapsActivity.drawMarkers(mMap, current);
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(mapsActivity)
				.setMessage("כתוב כאן את הפוסט")
				.setView(input)
				.setCancelable(false)
				.setPositiveButton("שגר פוסט", dialogClickListener)
				.setNegativeButton("בטל", dialogClickListener);
		return builder.create();
	}
}
