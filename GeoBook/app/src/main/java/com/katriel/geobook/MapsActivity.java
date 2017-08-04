package com.katriel.geobook;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.os.AsyncTaskCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.katriel.geobook.bl.entities.Post;
import com.katriel.geobook.bl.entities.User;
import com.katriel.geobook.bl.factories.DialogFactory;
import com.katriel.geobook.bl.utils.AuthUtils;
import com.katriel.geobook.bl.utils.DbUtils;
import com.katriel.geobook.bl.utils.Utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity {

	// Controls
	private Button btnAddPost;
	private Button btnLogOut;
	private Switch switchSat;
	private SeekBar seekBarRadius;
	private TextView tvRadius;
	private Button btnGoToFriends;
	private Button btnGoToSettings;

	// Google maps
	private GoogleMap mMap;
	private SupportMapFragment mapFragment;
	private int radius = 3000;
	private Circle circle;
	public List<Marker> markers;

	// Log tag
	private static final String TAG = "MapsActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		Utils.enableImmersiveMode(this);

		AuthUtils.validateUserLoggedIn(this);

		DbUtils.attachUsersListListener();

		DbUtils.attachFriendListListener();

		initializeVariables();

		configureVariables();

		initializeListeners();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

		Log.i(TAG, "Handling permission request code " + requestCode + " with the inputs:\nPermissions:" + Arrays.asList(permissions) + "\nResults:" + Arrays.asList(grantResults));
		switch (requestCode) {
			// Handling location permission request
			case Utils.LOCATION_REQUEST_CODE:
				Log.d(TAG, "Handling location request");
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Log.d(TAG, "Location request granted, recreating activity");
					mMap.clear();
					drawMap(mMap);
				} else {
					Log.d(TAG, "Location request denied, alerting user and closing activity");
					AlertDialog alert = DialogFactory.createLocationPermissionIsNeeded(this);
					alert.show();
				}
				break;
		}
	}

	public void drawMap(GoogleMap map) {
		drawMap(map, true);
	}

	public void drawMap(final GoogleMap map, boolean focusOnUser) {
		Location location = Utils.getBestLocationWithPermissionCheck(this, map);
		if (location == null) {
			return;
		}
		map.clear();
		double userLat = location.getLatitude();
		double userLng = location.getLongitude();
		LatLng current = new LatLng(userLat, userLng);

		drawMarkers(map, current);
		Log.d(TAG, "Drawing circle with center in latitude " + current.latitude + ", longitude " + current.longitude + " and radius of " + radius + " meters");
		circle = map.addCircle(new CircleOptions().center(current).radius(radius).strokeWidth(8).strokeColor(Color.BLUE));
		if (focusOnUser)
			map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 12));

		map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
			@Override
			public View getInfoWindow(Marker marker) {
				return null;
			}

			@Override
			public View getInfoContents(Marker marker) {
				LinearLayout layout = new LinearLayout(MapsActivity.this);
				layout.setOrientation(LinearLayout.VERTICAL);

				Utils.setLinearLayoutParam(layout, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

				Log.v(TAG, "Handle marker title");
				TextView tvTitle = new TextView(MapsActivity.this);
				String title = marker.getTitle();
				tvTitle.setTextColor(Color.BLACK);
				tvTitle.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
				tvTitle.setText(title);
				tvTitle.setTextSize(14.0f);
				tvTitle.setTypeface(null, Typeface.BOLD);
				Utils.setLinearLayoutParam(tvTitle, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

				TextView tvContent = new TextView(MapsActivity.this);
				tvContent.setTextColor(Color.BLACK);
				tvContent.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
				String content = marker.getSnippet();
				tvContent.setText(content);
				Utils.setLinearLayoutParam(tvContent, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

				layout.addView(tvTitle);
				layout.addView(tvContent);

				String postUid = ((Post) marker.getTag()).getUid();
				String currentUid = AuthUtils.getCurrentUid();
				if (currentUid.equals(postUid)) {
					TextView tvDelete = new TextView(MapsActivity.this);
					tvDelete.setTextColor(Color.RED);
					tvDelete.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
					tvDelete.setText("למחיקה הקש בפוסט");
					tvDelete.setTypeface(null, Typeface.BOLD);
					Utils.setLinearLayoutParam(tvDelete, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
					layout.addView(tvDelete);
				}

				return layout;
			}
		});

		map.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
			@Override
			public void onInfoWindowClick(Marker marker) {
				Post post = (Post) marker.getTag();
				String currentUid = AuthUtils.getCurrentUid();
				if (currentUid == null || post == null) {
					Log.e(TAG, "drawMap: onMarkerClick: user of post is null.");
					return;
				}
				if (post.getUid().equals(currentUid)) {
					Log.i(TAG, "drawMap: onMarkerClick: Deleting post of user '" + currentUid + "', time stamp " + post.getTimeStamp());
					marker.remove();
					DbUtils.removePost(currentUid, post.getTimeStamp(), MapsActivity.this);
					return;
				}
			}
		});


		map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
			@Override
			public boolean onMarkerClick(final Marker marker) {
				if (marker.isInfoWindowShown())
					marker.hideInfoWindow();
				else
					marker.showInfoWindow();
				return true;
			}
		});

	}

	public void drawMarkers(GoogleMap map, LatLng current) {
		double userLat = current.latitude;
		double userLng = current.longitude;
		final DateFormat df = DateFormat.getDateTimeInstance();
		String currentUid = AuthUtils.getCurrentUid();
		if (currentUid == null) {
			Log.e(TAG, "drawMarkers: Can't draw markers because no user is logged in");
			return;
		}
		Log.i(TAG, "drawMarkers: drawing all relevant markers");
		Map<User, List<Post>> userPosts = DbUtils.getPosts();
		List<User> friends = DbUtils.getFriends();
		removeIrrelevantMarkers(userPosts, current);
		for (Map.Entry<User, List<Post>> entry : userPosts.entrySet()) {
			User user = entry.getKey();
			Log.v(TAG, "drawMarkers: Drawing user '" + user.getEmail() + "' markers, if any.");
			if (friends.contains(user) || currentUid.equals(user.getUid())) {
				List<Post> posts = entry.getValue();
				for (Post post : posts) {
					Log.v(TAG, "drawMarkers: Drawing marker " + post.getTimeStamp() + " marker, if needed.");
					double postLat = post.getLat();
					double postLng = post.getLng();
					int distance = Utils.distance(userLat, userLng, postLat, postLng);
					Log.v(TAG, "drawMarkers: Distance between current position and marker " + post.getTimeStamp() + " is " + distance + " meters.\n" +
							"Radius of circle is " + radius);
					Log.v(TAG, "drawMarkers: Marker position: Latitude - " + postLat + ", Longitude - " + postLng);
					if (distance < radius) {
						long timeStamp = post.getTimeStamp();
						String date = df.format(new Date(timeStamp));
						String content = date + "\n" + post.getContent();
						LatLng postPosition = new LatLng(postLat, postLng);
						if (!doesPostExists(post)) {
							Marker marker = map.addMarker(new MarkerOptions().position(postPosition).title(user.getDisplayName()).snippet(content));
							marker.setTag(post);
							marker.setVisible(true);
							// marker.showInfoWindow();
							markers.add(marker);
						}
					}
				}
			}
		}
	}

	private void initializeVariables() {
		Log.i(TAG, "Initializing variables");
		btnAddPost = (Button) findViewById(R.id.btnAddPost);
		btnLogOut = (Button) findViewById(R.id.btnLogOut);
		switchSat = (Switch) findViewById(R.id.switchSat);
		seekBarRadius = (SeekBar) findViewById(R.id.seekBarRadius);
		tvRadius = (TextView) findViewById(R.id.tvRadius);
		btnGoToFriends = (Button) findViewById(R.id.btnMapToFriends);
		btnGoToSettings = (Button) findViewById(R.id.btnMapToSettings);
		mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		circle = null;
		markers = new ArrayList<>();
	}

	private void configureVariables() {
		Log.d(TAG, "Configuring seek bar");
		seekBarRadius.setProgress(radius);
		tvRadius.setText("רדיוס: " + radius + " מטרים");
	}

	private void initializeListeners() {
		Log.i(TAG, "Initializing listeners");

		Log.d(TAG, "Initializing 'Add Post' button listener");
		btnAddPost.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Log.i(TAG, "initializeListeners: Opening new post dialog.");
				String uid = AuthUtils.getCurrentUid();
				AlertDialog alertDialog = DialogFactory.createAddPostDialog(uid, MapsActivity.this, mMap);
				alertDialog.show();

			}
		});

		Log.d(TAG, "Initializing 'Log Out' button listener");
		btnLogOut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
				overridePendingTransition(R.anim.swipe_down_in, R.anim.swipe_down_out);
				AuthUtils.signOut();
			}
		});

		Log.d(TAG, "Initializing 'Satellite View' switch listener");
		switchSat.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Switch sw = (Switch) v;
				mMap.setMapType(sw.isChecked() ? GoogleMap.MAP_TYPE_HYBRID : GoogleMap.MAP_TYPE_NORMAL);

			}
		});

		Log.d(TAG, "Initialize onMapReady listener");
		mapFragment.getMapAsync(new OnMapReadyCallback() {
			/**
			 * Manipulates the map once available.
			 * This callback is triggered when the map is ready to be used.
			 * This is where we can add markers or lines, add listeners or move the camera. In this case,
			 * we just add a marker near Sydney, Australia.
			 * If Google Play services is not installed on the device, the user will be prompted to install
			 * it inside the SupportMapFragment. This method will only be triggered once the user has
			 * installed Google Play services and returned to the app.
			 */
			@Override
			public void onMapReady(GoogleMap googleMap) {
				mMap = googleMap;
				DbUtils.attachPostListListener(MapsActivity.this, mMap);
				MapsActivity.this.drawMap(mMap);
			}
		});

		Log.d(TAG, "Initialize radius picker seek bar listener");
		seekBarRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				radius = progress;
				tvRadius.setText("רדיוס: " + radius + " מטרים");
				circle.setRadius(radius);
				Location currentLocation = Utils.getBestLocationWithPermissionCheck(MapsActivity.this, mMap);
				LatLng current = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
				drawMarkers(mMap, current);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		});

		Log.d(TAG, "Initialize navigation to friends activity button listener");
		btnGoToFriends.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MapsActivity.this, FriendsActivity.class);
				startActivity(intent);
				overridePendingTransition(R.anim.swipe_left_in, R.anim.swipe_left_out);
				finish();
			}
		});

		Log.d(TAG, "Initialize navigation to settings activity button listener");
		btnGoToSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MapsActivity.this, SettingsActivity.class);
				startActivity(intent);
				overridePendingTransition(R.anim.swipe_left_in, R.anim.swipe_left_out);
				finish();
			}
		});
	}

	private void removeIrrelevantMarkers(Map<User, List<Post>> usersPosts, LatLng current) {
		double userLat = current.latitude;
		double userLng = current.longitude;
		List<Marker> markersToRemove = new ArrayList<>();
		for (Marker marker : markers) {
			Post markerPost = (Post) marker.getTag();
			if (markerPost == null) {
				marker.remove();
				markersToRemove.add(marker);
				continue;
			}
			double postLat = markerPost.getLat();
			double postLng = markerPost.getLng();
			int distance = Utils.distance(userLat, userLng, postLat, postLng);
			if (distance >= radius) {
				marker.remove();
				markersToRemove.add(marker);
				continue;
			}
			boolean deleteMarker = true;
			for (Map.Entry<User, List<Post>> userPosts : usersPosts.entrySet()) {
				for (Post userPost : userPosts.getValue()) {
					if (userPost.equals(markerPost))
						deleteMarker = false;
				}
			}
			if (deleteMarker) {
				marker.remove();
				markersToRemove.add(marker);
			}
		}
		markers.removeAll(markersToRemove);
	}

	private boolean doesPostExists(Post post) {
		for (Marker marker : markers)
			if (post.equals(marker.getTag()))
				return true;
		return false;
	}

}
