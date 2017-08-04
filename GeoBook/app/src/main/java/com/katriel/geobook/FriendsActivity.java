package com.katriel.geobook;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.katriel.geobook.bl.entities.User;
import com.katriel.geobook.bl.factories.ListenerFactory;
import com.katriel.geobook.bl.utils.AuthUtils;
import com.katriel.geobook.bl.utils.DbUtils;
import com.katriel.geobook.bl.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends AppCompatActivity {

	// Controls
	private Button btnGoToMap;
	private Button btnGoToSettings;
	private RadioGroup rgViewChoice;
	private RadioButton rbFriendsList;
	private RadioButton rbIncomingFriendRequests;
	private RadioButton rbAddFriend;
	private LinearLayout container;

	// Log tag
	private static final String TAG = "FriendsActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_friends);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		Utils.enableImmersiveMode(this);

		AuthUtils.validateUserLoggedIn(this);

		initializeVariables();

		initializeListeners();

		DbUtils.attachUsersListListener(this);

		DbUtils.attachFriendListListener(this);

		DbUtils.attachFriendRequestsListeners(this);

		createFriendList();

	}

	private void initializeVariables() {
		Log.i(TAG, "Initializing variables");
		btnGoToMap = (Button) findViewById(R.id.btnFriendsToMap);
		btnGoToSettings = (Button) findViewById(R.id.btnFriendsToSettings);
		rgViewChoice = (RadioGroup) findViewById(R.id.rgViewChoice);
		rbFriendsList = (RadioButton) findViewById(R.id.rbFriendsList);
		rbIncomingFriendRequests = (RadioButton) findViewById(R.id.rbIncomingFriendRequests);
		rbAddFriend = (RadioButton) findViewById(R.id.rbAddFriend);
		container = (LinearLayout) findViewById(R.id.container);
	}

	private void initializeListeners() {
		Log.i(TAG, "Initializing listeners");

		Log.d(TAG, "Initialize navigation to map button listener");
		btnGoToMap.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(FriendsActivity.this, MapsActivity.class);
				startActivity(intent);
				overridePendingTransition(R.anim.swipe_right_in, R.anim.swipe_right_out);
				finish();
			}
		});

		Log.d(TAG, "Initialize navigation to settings activity button listener");
		btnGoToSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(FriendsActivity.this, SettingsActivity.class);
				startActivity(intent);
				overridePendingTransition(R.anim.swipe_left_in, R.anim.swipe_left_out);
				finish();
			}
		});

		Log.d(TAG, "Initialize view choice radio group listener");
		rgViewChoice.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
				container.removeAllViews();
				switch (checkedId) {
					case R.id.rbFriendsList:
						Log.i(TAG, "User chose to view friend list");
						createFriendList();
						break;
					case R.id.rbIncomingFriendRequests:
						Log.i(TAG, "User chose to view friend request");
						createFriendRequestsList();
						break;
					case R.id.rbAddFriend:
						Log.i(TAG, "User chose to view add friends");
						createAddFriend();
						break;
				}
			}
		});
	}

	public void createFriendList() {
		if (!rbFriendsList.isChecked())
			return;
		Log.i(TAG, "Creating previous list");
		container.removeAllViews();
		List<User> users = DbUtils.getFriends();
		for (int i = 0; i < users.size(); i++) {
			if (i > 0) {
				Log.d(TAG, "Creating horizontal line");
				View line = new View(this);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 3);
				params.setMargins(16, 16, 16, 0);
				line.setLayoutParams(params);
				line.setBackgroundColor(Color.parseColor("#aaaaaa"));
				container.addView(line);
			}
			final User user = users.get(i);
			Log.d(TAG, "Creating user list item for user '" + user.getUid() + "'");
			// Frame handling
			LinearLayout frame = new LinearLayout(this);
			Utils.setLinearLayoutParam(frame, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
			frame.setOrientation(LinearLayout.HORIZONTAL);

			// Button Handling
			// Button btnRemove = new Button(this, null, R.style.Widget_AppCompat_Button_Borderless);
			Button btnRemove = new Button(this);
			Utils.setLinearLayoutParam(btnRemove, 0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
			btnRemove.setText("הסר");
			btnRemove.setTextColor(Color.RED);
			btnRemove.setOnClickListener(ListenerFactory.getRemoveFriendButtonListener(user));
			frame.addView(btnRemove);

			// Details frame handling
			LinearLayout detailsFrame = getUserDetailsFrame(user);
			frame.addView(detailsFrame);
			container.addView(frame);
		}
	}

	public void createFriendRequestsList() {
		if (!rbIncomingFriendRequests.isChecked())
			return;
		Log.i(TAG, "Creating previous list of friend requests");
		container.removeAllViews();
		List<User> users = DbUtils.getFriendRequestsReversed();
		for (int i = 0; i < users.size(); i++) {
			if (i > 0) {
				Log.d(TAG, "Creating horizontal line");
				View line = new View(this);
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 3);
				params.setMargins(16, 16, 16, 0);
				line.setLayoutParams(params);
				line.setBackgroundColor(Color.parseColor("#aaaaaa"));
				container.addView(line);
			}
			final User user = users.get(i);
			Log.d(TAG, "Creating user list item for user '" + user.getUid() + "'");
			// Frame handling
			LinearLayout frame = new LinearLayout(this);
			Utils.setLinearLayoutParam(frame, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
			frame.setOrientation(LinearLayout.HORIZONTAL);

			// Buttons Handling
			Button btnApprove = new Button(this);
			Utils.setLinearLayoutParam(btnApprove, 0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
			btnApprove.setText("אשר");
			btnApprove.setTextColor(Color.GREEN);
			btnApprove.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String uidTo = AuthUtils.getCurrentUid();
					String uidFrom = user.getUid();
					DbUtils.addOrRemoveFriendRequests(uidFrom, uidTo, false);
					DbUtils.addOrRemoveFriend(uidFrom, uidTo, true);
				}
			});
			frame.addView(btnApprove);

			Button btnRemove = new Button(this);
			Utils.setLinearLayoutParam(btnRemove, 0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
			btnRemove.setText("דחה");
			btnRemove.setTextColor(Color.RED);
			btnRemove.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String uidTo = AuthUtils.getCurrentUid();
					String uidFrom = user.getUid();
					DbUtils.addOrRemoveFriendRequests(uidFrom, uidTo, false);
					DbUtils.addOrRemoveFriend(uidFrom, uidTo, false);
				}
			});
			frame.addView(btnRemove);

			// Details frame handling
			LinearLayout detailsFrame = new LinearLayout(this);
			Utils.setLinearLayoutParam(detailsFrame, 0, LinearLayout.LayoutParams.MATCH_PARENT, 4.0f);
			detailsFrame.setOrientation(LinearLayout.VERTICAL);

			// Text views handling
			TextView tvDisplayName = new TextView(this);
			tvDisplayName.setText("שם משתמש: " + user.getDisplayName());
			tvDisplayName.setTextSize(20.0f);
			tvDisplayName.setTypeface(null, Typeface.BOLD);
			tvDisplayName.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

			TextView tvEmail = new TextView(this);
			tvEmail.setText("מייל: " + user.getEmail());
			tvEmail.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

			detailsFrame.addView(tvDisplayName);
			detailsFrame.addView(tvEmail);
			frame.addView(detailsFrame);
			container.addView(frame);
		}
	}

	public void createAddFriend() {
		if (!rbAddFriend.isChecked())
			return;
		Log.i(TAG, "createAddFriend: Creating previous list");
		container.removeAllViews();

		// Create autocomplete text box;
		AutoCompleteTextView tbAutoComplete = new AutoCompleteTextView(this);
		Utils.setLinearLayoutParam(tbAutoComplete, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		tbAutoComplete.setTextAlignment(AutoCompleteTextView.TEXT_ALIGNMENT_CENTER);
		tbAutoComplete.setHint("חפש חברים");
		List<User> nonFriends = getNonFriends();
		User[] nonFriendsArr = new User[nonFriends.size()];
		for (int i = 0; i < nonFriends.size(); i++)
			nonFriendsArr[i] = new User(nonFriends.get(i));
		final ArrayAdapter<User> adapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_item, nonFriendsArr);
		tbAutoComplete.setThreshold(1);
		tbAutoComplete.setAdapter(adapter);

		Log.d(TAG, "createAddFriend: Creating disabled button");
		final Button btnFriendRequest = new Button(this);
		Utils.setLinearLayoutParam(btnFriendRequest, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
		btnFriendRequest.setText("שליחת בקשת חברות");
		btnFriendRequest.setEnabled(false);
		tbAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final User userToAdd = adapter.getItem(position);
				btnFriendRequest.setText("בקש חברות ממשתמש: " + userToAdd.getDisplayName());
				btnFriendRequest.setEnabled(true);
				btnFriendRequest.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String uidFrom = AuthUtils.getCurrentUid();
						String uidTo = userToAdd.getUid();
						List<User> friends = DbUtils.getFriends();
						for (User friend : friends) {
							if (friend.getUid().equals(uidTo)) {
								Log.i(TAG, "User is already send friend request '" + uidFrom + "' to user '" + uidTo + "'");
								return;
							}
						}
						Log.i(TAG, "Sending friend request from user '" + uidFrom + "' to user '" + uidTo + "'");
						DbUtils.addOrRemoveFriendRequests(uidFrom, uidTo, true);
					}
				});
			}
		});


		Log.d(TAG, "createAddFriend: Adding views to container");
		container.addView(tbAutoComplete);
		//container.addView(line);
		container.addView(btnFriendRequest);
	}

	private List<User> getNonFriends() {
		Log.i(TAG, "getNonFriends: Fetching non-friends list");

		List<User> users = new ArrayList<>(DbUtils.getUsers());
		List<User> friendRequests = new ArrayList<>(DbUtils.getFriendRequests());
		List<User> friendRequestsReversed = new ArrayList<>(DbUtils.getFriendRequestsReversed());
		List<User> friends = new ArrayList<>(DbUtils.getFriends());
		Log.i(TAG, "getNonFriends: All Users:\n" + users);
		Log.i(TAG, "getNonFriends: User sent friend request to:\n" + friendRequests);
		Log.i(TAG, "getNonFriends: Users sent request to current user:\n" + friendRequestsReversed);
		users.removeAll(friends);
		users.removeAll(friendRequests);
		users.removeAll(friendRequestsReversed);
		users.remove(new User(AuthUtils.getCurrentUid(),"",""));
		Log.i(TAG, "getNonFriends: Non-friends:\n" + users);
		return users;
	}

	private LinearLayout getUserDetailsFrame(User user) {
		Log.d(TAG, "Creating user details frame for user '" + user.getEmail() + "'");
		LinearLayout detailsFrame = new LinearLayout(this);
		Utils.setLinearLayoutParam(detailsFrame, 0, LinearLayout.LayoutParams.MATCH_PARENT, 6.0f);
		detailsFrame.setOrientation(LinearLayout.VERTICAL);

		// Text views handling
		TextView tvDisplayName = new TextView(this);
		tvDisplayName.setText("שם משתמש: " + user.getDisplayName());
		tvDisplayName.setTextSize(20.0f);
		tvDisplayName.setTypeface(null, Typeface.BOLD);
		tvDisplayName.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		TextView tvEmail = new TextView(this);
		tvEmail.setText("מייל: " + user.getEmail());
		tvEmail.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		detailsFrame.addView(tvDisplayName);
		detailsFrame.addView(tvEmail);
		return detailsFrame;
	}
}
