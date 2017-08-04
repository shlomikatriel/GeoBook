package com.katriel.geobook.bl.factories;


import android.util.Log;
import android.view.View;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.katriel.geobook.bl.entities.User;
import com.katriel.geobook.bl.utils.AuthUtils;
import com.katriel.geobook.bl.utils.DbUtils;

import java.util.List;

public class ListenerFactory {

	private static final String TAG = "ListenerFactory";

	public static View.OnClickListener getRemoveFriendButtonListener(final User user) {
		return new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String uid1 = AuthUtils.getCurrentUid();
				String uid2 = user.getUid();
				if (uid1 == null || uid2 == null) {
					Log.w(TAG, "Couldn't remove friendship. one of the uid values is null. uid1: '" + uid1 + "', uid2: '" + uid2 + "'");
					return;
				}
				Log.d(TAG, "Removing friendship of users with uid's '" + uid1 + "' and '" + uid2 + "'");
				DbUtils.addOrRemoveFriend(uid1, uid2, false);
			}
		};
	}

	public static ValueEventListener getAddOrRemoveFriendDbEventListener(final String uid1, final String uid2, final boolean trueAddFalseRemove) {
		return new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				String uidsStr = dataSnapshot.getValue(String.class);
				Log.d(TAG, "getAddOrRemoveFriendDbEventListener: Friend list before change:\n" + uidsStr);
				List<String> friendUids = DbUtils.parseUidList(uidsStr);
				friendUids.remove(uid2);
				if (trueAddFalseRemove)
					friendUids.add(uid2);
				uidsStr = DbUtils.stringifyUidList(friendUids);
				if (uidsStr.isEmpty())
					uidsStr = null;
				Log.d(TAG, "getAddOrRemoveFriendDbEventListener: Friend list after change:\n" + uidsStr);
				dataSnapshot.getRef().setValue(uidsStr);
			}

			@Override
			public void onCancelled(DatabaseError error) {
				if (trueAddFalseRemove)
					Log.e(TAG, "getAddOrRemoveFriendDbEventListener: Could not make user with uid '" + uid1 + "' friend with uid '" + uid2 + "'");
				else
					Log.e(TAG, "getAddOrRemoveFriendDbEventListener: Could not remove '" + uid2 + "' from '" + uid1 + "' friend list");
			}
		};
	}

	public static ValueEventListener getAddOrRemoveFriendRequestsDbEventListener(final String uidFrom, final String uidTo, final boolean trueAddFalseRemove) {
		return new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				String uidsStr = dataSnapshot.getValue(String.class);
				Log.d(TAG, "getAddOrRemoveFriendRequestDbEventListener: Friend requests of '" + uidTo + "' before change:\n" + uidsStr);
				List<String> requestsUids = DbUtils.parseUidList(uidsStr);
				requestsUids.remove(uidFrom);
				if (trueAddFalseRemove)
					requestsUids.add(uidFrom);
				uidsStr = DbUtils.stringifyUidList(requestsUids);
				Log.d(TAG, "getAddOrRemoveFriendRequestDbEventListener: Friend requests of '" + uidTo + "' after change:\n" + uidsStr);
				if (uidsStr.isEmpty())
					uidsStr = null;
				dataSnapshot.getRef().setValue(uidsStr);
			}

			@Override
			public void onCancelled(DatabaseError error) {
				Log.e(TAG, "getAddOrRemoveFriendRequestDbEventListener: Could not " + (trueAddFalseRemove ? "add" : "remove") + " friend request from'" + uidFrom + "' to '" + uidTo + "'");
			}
		};
	}

}
