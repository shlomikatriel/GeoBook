package com.katriel.geobook.bl.utils;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.katriel.geobook.MapsActivity;
import com.katriel.geobook.R;
import com.katriel.geobook.bl.entities.User;

import java.util.List;


public class AuthUtils {

	private static FirebaseAuth mAuth = FirebaseAuth.getInstance();

	private static final String TAG = AuthUtils.class.getSimpleName();


	private enum ErrorCode {
		ERROR_EMAIL_ALREADY_IN_USE("האימייל כבר קיים במערכת, נא להשתמש באימייל אחר"),
		ERROR_INVALID_EMAIL("האימייל שהוזן אינו תקין"),
		ERROR_OPERATION_NOT_ALLOWED("פעולה לא מאושרת"),
		ERROR_WEAK_PASSWORD("סיסמה חלשה מדי, נא להשתמש בסיסמה חזקה יותר"),
		ERROR_USER_DISABLED("המשתמש אינו פעיל, נא לפנות לתמיכה"),
		ERROR_USER_NOT_FOUND("המשתמש אינו קיים, נא להירשם קודם"),
		ERROR_WRONG_PASSWORD("סיסמה אינה נכונה, נסה שנית");


		private String errorMessage;

		private ErrorCode(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		@Override
		public String toString() {
			return errorMessage;
		}
	}

	public static void signIn(final Activity logInActivity, final Class<MapsActivity> mapsActivity, final String email, final String password) {
		final String TAG = logInActivity.getClass().getSimpleName();
		mAuth.signInWithEmailAndPassword(email, password)
				.addOnCompleteListener(logInActivity, new OnCompleteListener<AuthResult>() {
					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());
						// If sign in fails, display a message to the user. If sign in succeeds
						// the auth state listener will be notified and logic to handle the
						// signed in user can be handled in the listener.
						if (task.getException() != null) {
							final String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();
							Log.e(TAG, "createUserWithEmail:onComplete: Found error code '" + errorCode + "'");
							ErrorCode cuec = ErrorCode.valueOf(errorCode);
							Utils.showToast(logInActivity, cuec.toString());
							return;
						}
						if (!task.isSuccessful()) {
							Log.e(TAG, "signInWithEmail:failed", task.getException());
							Utils.showToast(logInActivity, "לא היה ניתן להתחבר באמצעות משתמש '" + email + "'");
							return;
						} else {
							Log.i(TAG, "signInWithEmail:success");
							FirebaseUser user = mAuth.getCurrentUser();
							DbUtils.addUser(user);
							Utils.showToast(logInActivity, "ברוכים הבאים " + user.getDisplayName());
							Intent intent = new Intent(logInActivity, mapsActivity);
							logInActivity.startActivity(intent);
							logInActivity.overridePendingTransition(R.anim.swipe_up_in, R.anim.swipe_up_out);
						}
					}
				});
	}

	public static void register(final Activity registrationActivity, final Class<MapsActivity> mapsActivity, final String email, final String password, final String displayName) {
		final String TAG = registrationActivity.getClass().getSimpleName();
		mAuth.createUserWithEmailAndPassword(email, password)
				.addOnCompleteListener(registrationActivity, new OnCompleteListener<AuthResult>() {
					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());
						// If sign in fails, display a message to the user. If sign in succeeds
						// the auth state listener will be notified and logic to handle the
						// signed in user can be handled in the listener.

						if (task.getException() != null) {
							final String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();
							Log.e(TAG, "createUserWithEmail:onComplete: Found error code '" + errorCode + "'");
							ErrorCode cuec = ErrorCode.valueOf(errorCode);
							Utils.showToast(registrationActivity, cuec.toString());
							return;
						}
						Log.i(TAG, "createUserWithEmail:onComplete: Validating success on task level:\ntask.isSuccessful()=" + task.isSuccessful() + "\ntask.isComplete=" + task.isComplete());
						if (!task.isSuccessful()) {
							Log.e(TAG, "createUserWithEmail:onComplete: Task failed. Exception message '" + task.getException().getMessage() + "'");
							final String errorMessage = "לא ניתן ליצור משתמש '" + email + "'";
							Utils.showToast(registrationActivity, errorMessage);
							return;
						}

						Log.i(TAG, "createUserWithEmail:onComplete: User with email '" + email + "' created successfully");
						final FirebaseUser user = task.getResult().getUser();
						if (user != null) {
							UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
									.setDisplayName(displayName).build();
							user.updateProfile(profileUpdates);
							DbUtils.addUser(user.getUid(), user.getEmail(), displayName);
							final String email = user.getEmail();
							Utils.showToast(registrationActivity, "המשתמש '" + email + "' רשום ומחובר");
							Intent intent = new Intent(registrationActivity, mapsActivity);
							registrationActivity.startActivity(intent);
							registrationActivity.finish();
						}
					}
				});
	}

	public static void validateUserLoggedIn(Activity activity) {
		Log.i(TAG, "Validating user is logged in in activity '" + activity.getLocalClassName() + "'");
		if (mAuth.getCurrentUser() == null) {
			Log.e(TAG, "Couldn't create maps activity because no user is signed in");
			Utils.showToast(activity, "נא להירשם לפני השימוש באפליקציה");
			activity.finish();
		}
	}

	public static void signOut() {
		mAuth.signOut();
	}

	public static void updateUserDetails(Activity activity, String password, String displayName) {
		if (password == null) {
			password = "";
		}
		if (displayName == null) {
			displayName = "";
		}
		Log.i(TAG, "updateUserDetails: Updating current user details with new password '" + password + "' and displayName '" + displayName + "'");
		FirebaseUser user = mAuth.getCurrentUser();
		if (user == null) {
			Log.e(TAG, "updateUserDetails: Cant update user details because no user is logged on");
			return;
		}
		if (password.isEmpty() && displayName.isEmpty()) {
			Log.w(TAG, "updateUserDetails: Cant update user details parameters given are empty");
			return;
		}
		if (!password.isEmpty()) {
			Task<Void> task = user.updatePassword(password);
			if (!Utils.syncOnTaskCompletion(task, "Update password for current user"))
				Log.e(TAG, "Couldn't update password for current user");
		}
		if (!displayName.isEmpty()) {
			UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
					.setDisplayName(displayName).build();
			Task<?> task = user.updateProfile(profileUpdates);
			if (!Utils.syncOnTaskCompletion(task, "Update display name for current user"))
				Log.e(TAG, "Couldn't update display name for current user");
		}
		Utils.showToast(activity, "פרטי המשתמש עודכנו בהצלחה");
	}

	public static boolean removeCurrentUser() {
		Log.i(TAG, "removeCurrentUser: Removing current logged in user.");
		FirebaseUser user = mAuth.getCurrentUser();
		if (user == null) {
			Log.e(TAG, "removeCurrentUser: Couldn't remove user because no user is logged on.");
			return false;
		}
		String currentUid = user.getUid();
		Task<Void> task = user.delete();
		Log.d(TAG, "removeCurrentUser: Removing user from database");
		for (long i = 0; i < 5000 && !task.isComplete(); i += 100) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Log.e(TAG, "removeCurrentUser: Thread couldn't sleep", e);
			}
		}
		if (!task.isComplete() || !task.isSuccessful()) {
			Log.e(TAG, "removeCurrentUser: Removal of user '" + currentUid + "' task failed or didn't finished within 5 seconds");
			return false;
		}
		List<User> friends = DbUtils.getFriends();
		List<User> friendRequests = DbUtils.getFriendRequests();
		List<User> friendRequestsReversed = DbUtils.getFriendRequestsReversed();
		Log.d(TAG, "removeCurrentUser: Friends:\n" + friends);
		Log.d(TAG, "removeCurrentUser: Friend requests:\n" + friendRequests);
		Log.d(TAG, "removeCurrentUser: Friends requests reversed:\n" + friendRequestsReversed);
		Log.d(TAG, "removeCurrentUser: Removing friends");
		for (User friend : friends) {
			String friendUid = friend.getUid();
			DbUtils.addOrRemoveFriend(currentUid, friendUid, false);
		}
		Log.d(TAG, "removeCurrentUser: Removing friend requests");
		for (User friendRequest : friendRequests) {
			String friendRequestUid = friendRequest.getUid();
			DbUtils.addOrRemoveFriendRequests(currentUid, friendRequestUid, false);
		}
		Log.d(TAG, "removeCurrentUser: Removing friend requests reversed");
		for (User friendRequestReversed : friendRequestsReversed) {
			String friendRequestReversedUid = friendRequestReversed.getUid();
			DbUtils.addOrRemoveFriendRequests(friendRequestReversedUid, currentUid, false);
		}
		DbUtils.removeUserListings(currentUid);
		Log.d(TAG, "removeCurrentUser: Removing user from firebase");

		return true;
	}

	public static String getCurrentUid() {
		FirebaseUser user = mAuth.getCurrentUser();
		if (user == null) {
			Log.d(TAG, "getCurrentUid: No user logged in - returning null");
			return null;
		}
		String uid = user.getUid();
		Log.i(TAG, "getCurrentUid: Current user uid is '" + uid + "'");
		return uid;
	}
}
