package com.katriel.geobook.bl.utils;

import android.location.Location;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.katriel.geobook.FriendsActivity;
import com.katriel.geobook.MapsActivity;
import com.katriel.geobook.bl.entities.Post;
import com.katriel.geobook.bl.entities.User;
import com.katriel.geobook.bl.factories.ListenerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbUtils {

	private static List<User> users = new ArrayList<>();

	private static Map<User, List<Post>> posts = new HashMap<>();

	private static List<User> friends = new ArrayList<>();

	private static List<User> friendRequests = new ArrayList<>();

	private static List<User> friendRequestsReversed = new ArrayList<>();

	private static final FirebaseDatabase database = FirebaseDatabase.getInstance();

	private static final String TAG = DbUtils.class.getSimpleName();

	private enum UserParameter {
		EMAIL("email"), DISPLAY_NAME("displayName");

		private String parameter;

		UserParameter(String parameter) {
			this.parameter = parameter;
		}

		@Override
		public String toString() {
			return parameter;
		}
	}

	private enum PostParameter {
		CONTENT("content"), LAT("lat"), LNG("lng");

		private String parameter;

		PostParameter(String parameter) {
			this.parameter = parameter;
		}

		@Override
		public String toString() {
			return parameter;
		}
	}

	private enum Reference {
		USERS("Users"),
		FRIENDS("Friends"),
		FRIEND_REQUESTS("FriendRequests"),
		FRIEND_REQUESTS_REVERSED("FriendRequestReversed"),
		POSTS("Posts");

		private String refName;

		Reference(String refName) {
			this.refName = refName;
		}

		@Override
		public String toString() {
			return refName;
		}
	}

	// Friend management
	public static void addUser(FirebaseUser user) {
		addUser(user.getUid(), user.getEmail(), user.getDisplayName());
	}

	public static void addUser(String uid, String email, String displayName) {
		if (uid == null || email == null) {
			Log.w(TAG, "addUser: At least one of the given parameters is null");
			return;
		}
		if (displayName == null)
			displayName = "";
		Log.i(TAG, "addUser: If not exists, adding the new user to database. User details:" +
				"\nEmail: " + email + "\nUid: " + uid + "\nDisplay name: " + displayName);
		DatabaseReference userRef = database
				.getReference(Reference.USERS.toString())
				.child(uid);
		userRef.child(UserParameter.EMAIL.toString()).setValue(email);
		userRef.child(UserParameter.DISPLAY_NAME.toString()).setValue(displayName);
	}

	public static void attachPostListListener(final MapsActivity mapsActivity, final GoogleMap map) {
		DatabaseReference postsRef = database.getReference(Reference.POSTS.toString());
		postsRef.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				Log.d(TAG, "attachPostListListener: onDataChange: Clearing previous posts");
				posts.clear();
				Iterable<DataSnapshot> usersSnapshot = dataSnapshot.getChildren();
				for (DataSnapshot userSnapshot : usersSnapshot) {
					String uid = userSnapshot.getKey();
					Log.v(TAG, "attachPostListListener: onDataChange: Adding posts of user '" + uid + "'");
					User user = getUser(uid);
					if (user != null) {
						List<Post> userPosts = new ArrayList<>();
						Iterable<DataSnapshot> userPostsSnapshot = userSnapshot.getChildren();
						for (DataSnapshot userPostSnapshot : userPostsSnapshot) {
							long timeStamp = Long.valueOf(userPostSnapshot.getKey());
							Log.v(TAG, "attachPostListListener: onDataChange: Adding post " + timeStamp + " to user '" + uid + "'");
							double lat = Double.valueOf(userPostSnapshot.child(PostParameter.LAT.toString()).getValue(String.class));
							double lng = Double.valueOf(userPostSnapshot.child(PostParameter.LNG.toString()).getValue(String.class));
							String content = userPostSnapshot.child(PostParameter.CONTENT.toString()).getValue(String.class);
							Post userPost = new Post(uid, timeStamp, content, lat, lng);
							userPosts.add(userPost);
						}
						posts.put(user, userPosts);
					}
				}
				Location location = Utils.getBestLocationWithPermissionCheck(mapsActivity,map);
				LatLng current = new LatLng(location.getLatitude(),location.getLongitude());
				mapsActivity.drawMarkers(map,current);
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {

			}
		});
	}

	public static void attachUsersListListener() {
		attachUsersListListener(null);
	}

	public static void attachFriendListListener() {
		attachFriendListListener(null);
	}

	public static void attachFriendRequestsListeners() {
		attachFriendRequestsListeners(null);
	}

	public static void attachUsersListListener(final FriendsActivity friendsActivity) {
		DatabaseReference usersRef = database.getReference(Reference.USERS.toString());
		usersRef.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				String currentUid = AuthUtils.getCurrentUid();
				Log.i(TAG, "Fetching users from server, found " + dataSnapshot.getChildrenCount() + " users");
				users.clear();
				Iterable<DataSnapshot> usersData = dataSnapshot.getChildren();
				for (DataSnapshot userData : usersData) {
					String uid = userData.getKey();
					String email = null, displayName = null;
					Iterable<DataSnapshot> parametersData = userData.getChildren();
					for (DataSnapshot parameterData : parametersData) {
						String parameter = parameterData.getKey();
						String value = parameterData.getValue(String.class);
						if (parameter.equals(UserParameter.EMAIL.toString()))
							email = value;
						else
							displayName = value;
					}
					Log.v(TAG, "Found user: " + uid + ", " + email + ", " + displayName);
					User user = new User(uid, email, displayName);
					users.add(user);
				}
				if (friendsActivity != null)
					friendsActivity.createFriendList();
			}

			@Override
			public void onCancelled(DatabaseError error) {
				// Failed to read value
				Log.w(TAG, "Failed to read value.", error.toException());
			}
		});
	}

	public static void attachFriendListListener(final FriendsActivity friendsActivity) {
		final String currentUid = AuthUtils.getCurrentUid();
		if (currentUid == null) {
			Log.e(TAG, "attachFriendListListener: Couldn't load friend list because no user is logged on");
			return;
		}
		DatabaseReference friendsUidsRef = database.getReference(Reference.FRIENDS.toString()).child(currentUid);
		friendsUidsRef.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				Log.i(TAG, "attachFriendListListener: Fetching friends of user '" + currentUid + "' from server.");
				friends.clear();
				String uidsStr = dataSnapshot.getValue(String.class);
				List<String> uids = new ArrayList<>();
				if (uidsStr != null)
					uids = parseUidList(uidsStr);
				Log.i(TAG, "attachFriendListListener: Found friend uid's: \n" + uidsStr);
				for (String uid : uids) {
					for (User user : users) {
						if (uid.equals(user.getUid())) {
							friends.add(new User(user));
							break;
						}
					}
				}
				if (friendsActivity != null)
					friendsActivity.createFriendList();
			}

			@Override
			public void onCancelled(DatabaseError error) {
				Log.e(TAG, "Failed to read value.", error.toException());
			}
		});
	}

	public static void attachFriendRequestsListeners(final FriendsActivity friendsActivity) {
		final String currentUid = AuthUtils.getCurrentUid();
		if (currentUid == null) {
			Log.e(TAG, "attachFriendRequestsListeners: Couldn't load friend list because no user is logged on");
			return;
		}
		DatabaseReference friendsUidsRef = database.getReference(Reference.FRIEND_REQUESTS.toString()).child(currentUid);
		friendsUidsRef.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				Log.i(TAG, "attachFriendRequestsListeners: Fetching friend requests from user '" + currentUid + "' from server.");
				friendRequests.clear();
				String uidsStr = dataSnapshot.getValue(String.class);
				List<String> uids = new ArrayList<>();
				if (uidsStr != null)
					uids = parseUidList(uidsStr);
				Log.i(TAG, "attachFriendRequestsListeners: Found user uid's: \n" + uidsStr);
				for (String uid : uids) {
					for (User user : users) {
						if (uid.equals(user.getUid())) {
							friendRequests.add(new User(user));
							break;
						}
					}
				}
				if (friendsActivity != null)
					friendsActivity.createFriendRequestsList();
			}

			@Override
			public void onCancelled(DatabaseError error) {
				// Failed to read value
				Log.w(TAG, "Failed to read value.", error.toException());
			}
		});
		friendsUidsRef = database.getReference(Reference.FRIEND_REQUESTS_REVERSED.toString()).child(currentUid);
		friendsUidsRef.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				Log.i(TAG, "attachFriendRequestsListeners: Fetching friend requests to user '" + currentUid + "' from server.");
				friendRequestsReversed.clear();
				String uidsStr = dataSnapshot.getValue(String.class);
				List<String> uids = new ArrayList<>();
				if (uidsStr != null)
					uids = parseUidList(uidsStr);
				for (String uid : uids) {
					for (User user : users) {
						if (uid.equals(user.getUid())) {
							friendRequestsReversed.add(new User(user));
							break;
						}
					}
				}
				if (friendsActivity != null)
					friendsActivity.createFriendRequestsList();
			}

			@Override
			public void onCancelled(DatabaseError error) {
				// Failed to read value
				Log.w(TAG, "Failed to read value.", error.toException());
			}
		});
	}

	public static void addOrRemoveFriend(String uid1, String uid2, final boolean trueAddFalseRemove) {
		addOrRemoveFriendOneWay(uid1, uid2, trueAddFalseRemove);
		addOrRemoveFriendOneWay(uid2, uid1, trueAddFalseRemove);
	}

	private static void addOrRemoveFriendOneWay(final String uid1, final String uid2, final boolean trueAddFalseRemove) {
		if (trueAddFalseRemove)
			Log.i(TAG, "addFriendOneWay: Making user with uid '" + uid1 + "' friend with uid '" + uid2 + "'");
		else
			Log.i(TAG, "addFriendOneWay: Removing '" + uid2 + "' from '" + uid1 + "' friend list");
		DatabaseReference friendRef = database.getReference(Reference.FRIENDS.toString()).child(uid1);
		friendRef.addListenerForSingleValueEvent(ListenerFactory.getAddOrRemoveFriendDbEventListener(uid1, uid2, trueAddFalseRemove));
	}

	public static void addOrRemoveFriendRequests(String uidFrom, String uidTo, final boolean trueAddFalseRemove) {
		Log.i(TAG, "addFriendRequest: " + (trueAddFalseRemove ? "Adding" : "Removing") + " friend request from '" + uidFrom + "' to '" + uidTo + "'");
		DatabaseReference friendRef = database.getReference(Reference.FRIEND_REQUESTS_REVERSED.toString()).child(uidTo);
		friendRef.addListenerForSingleValueEvent(ListenerFactory.getAddOrRemoveFriendRequestsDbEventListener(uidFrom, uidTo, trueAddFalseRemove));
		friendRef = database.getReference(Reference.FRIEND_REQUESTS.toString()).child(uidFrom);
		friendRef.addListenerForSingleValueEvent(ListenerFactory.getAddOrRemoveFriendRequestsDbEventListener(uidTo, uidFrom, trueAddFalseRemove));
	}

	public static void removeUserListings(String uid) {
		Log.i(TAG, "removeUserListings: Remove user listings of '" + uid + "'");

		Reference[] references = new Reference[]{Reference.USERS, Reference.FRIENDS, Reference.FRIEND_REQUESTS, Reference.FRIEND_REQUESTS_REVERSED, Reference.POSTS};
		for (int i = 0; i < references.length; i++) {
			Reference reference = references[i];
			Log.d(TAG, "removeUserListings: Remove user listings of '" + uid + "' from reference '" + reference + "'");
			database.getReference(reference.toString()).child(uid).setValue(null);
		}
	}

	public static void addPost(String content, final MapsActivity mapsActivity, final GoogleMap mMap) {
		Log.i(TAG, "addPost: Adding post with content '" + content + "'");
		Location location = Utils.getBestLocationWithPermissionCheck(mapsActivity, mMap);

		String uid = AuthUtils.getCurrentUid();
		Double lat = location.getLatitude();
		Double lng = location.getLongitude();
		Long timeStamp = System.currentTimeMillis();
		final LatLng postPosition = new LatLng(lat, lng);
		User user = DbUtils.getUser(uid);
		Log.d(TAG, "addPost: Adding post with parameters:\nUid: " + uid + " Time stamp: " + timeStamp + " Latitude: " + lat + " Longitude: " + lng);

//		Marker marker = mMap.addMarker(new MarkerOptions().position(postPosition).title(user.getDisplayName()).snippet(content));
//		marker.setTag(new Post(uid, timeStamp, content, lat, lng));
//		marker.setVisible(true);
//		marker.showInfoWindow();

		DatabaseReference postRef = database.getReference(Reference.POSTS.toString()).child(uid).child(timeStamp.toString());
		Map<String, Object> postData = new HashMap<>();
		postData.put(PostParameter.CONTENT.toString(), content);
		postData.put(PostParameter.LAT.toString(), lat.toString());
		postData.put(PostParameter.LNG.toString(), lng.toString());
		postRef.updateChildren(postData, new DatabaseReference.CompletionListener() {
			@Override
			public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
				if (databaseError != null)
					Utils.showToast(mapsActivity, "שיגור הפוסט נכשל");
				else {
					Utils.showToast(mapsActivity, "שיגור הפוסט הצליח");
					mapsActivity.drawMarkers(mMap, postPosition);
				}
			}
		});
	}

	public static void removePost(final String currentUid, final long timeStamp, final MapsActivity mapsActivity) {
		Log.i(TAG, "removePost: Removing post of user '" + currentUid + "' with time stamp " + timeStamp);
		Long timeStampObj = Long.valueOf(timeStamp);
		database.getReference(Reference.POSTS.toString()).child(currentUid).child(timeStampObj.toString()).setValue(null, new DatabaseReference.CompletionListener() {
			@Override
			public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
				if (databaseError == null) {
					Log.i(TAG, "removePost: Removal of post of user '" + currentUid + "' with time stamp " + timeStamp + " finished successfully");
					Utils.showToast(mapsActivity, "מחיקת הפוסט הצליחה");
				} else {
					Log.e(TAG, "removePost: Removal of post of user '" + currentUid + "' with time stamp " + timeStamp + " failed");
					Utils.showToast(mapsActivity, "מחיקת הפוסט נכשלה");
				}
			}
		});

	}

	// Getters and Setters
	public static List<User> getUsers() {
		return users;
	}

	public static List<User> getFriends() {
		return friends;
	}

	public static List<User> getFriendRequests() {
		return friendRequests;
	}

	public static List<User> getFriendRequestsReversed() {
		return friendRequestsReversed;
	}

	public static Map<User, List<Post>> getPosts() {
		return posts;
	}

	// Auxiliary methods
	public static List<String> parseUidList(String uidsStr) {
		if (uidsStr == null || uidsStr.isEmpty())
			return new ArrayList<>();
		Log.v(TAG, "Parsing uid's string '" + uidsStr + "' to list");
		String[] uidsArr = uidsStr.split(",");
		Log.v(TAG, "Found " + uidsArr.length + " uid's");
		List<String> uids = new ArrayList<>(Arrays.asList(uidsArr));
		return uids;
	}

	public static String stringifyUidList(List<String> uids) {
		String uidsStr = "";
		for (int i = 0; i < uids.size(); i++) {
			if (i > 0)
				uidsStr += ",";
			uidsStr += uids.get(i);
		}
		return uidsStr;
	}

	public static User getUser(String uid) {
		for (User user : users)
			if (user.getUid().equals(uid))
				return user;
		return null;
	}
}
