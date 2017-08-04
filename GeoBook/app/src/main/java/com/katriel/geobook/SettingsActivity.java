package com.katriel.geobook;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.katriel.geobook.bl.factories.DialogFactory;
import com.katriel.geobook.bl.utils.AuthUtils;
import com.katriel.geobook.bl.utils.DbUtils;
import com.katriel.geobook.bl.utils.Utils;

public class SettingsActivity extends AppCompatActivity {

	// Controls
	private Button btnGoToMap;
	private Button btnGoToFriends;
	private EditText etPassword;
	private EditText etConfirmPassword;
	private EditText etDisplayName;
	private Button btnUpdateDetails;
	private Button btnRemoveUser;

	// Log tag
	private static final String TAG = "SettingsActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		Utils.enableImmersiveMode(this);

		AuthUtils.validateUserLoggedIn(this);

		DbUtils.attachUsersListListener();

		DbUtils.attachFriendListListener();

		DbUtils.attachFriendRequestsListeners();

		initializeVariables();

		initializeListeners();

	}

	private void initializeVariables() {
		Log.i(TAG, "Initializing variables");
		btnGoToMap = (Button) findViewById(R.id.btnSettingsToMap);
		btnGoToFriends = (Button) findViewById(R.id.btnSettingsToFriends);
		etPassword = (EditText) findViewById(R.id.etSettingsPassword);
		etConfirmPassword = (EditText) findViewById(R.id.etSettingsConfirmPassword);
		etDisplayName = (EditText) findViewById(R.id.etSettingsDisplayName);
		btnUpdateDetails = (Button) findViewById(R.id.btnUpdateDetails);
		btnRemoveUser = (Button) findViewById(R.id.btnRemoveUser);

	}

	private void initializeListeners() {
		Log.i(TAG, "Initializing listeners");

		Log.d(TAG, "Initialize navigation to map button listener");
		btnGoToMap.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SettingsActivity.this, MapsActivity.class);
				startActivity(intent);
				overridePendingTransition(R.anim.swipe_right_in, R.anim.swipe_right_out);
				finish();
			}
		});

		Log.d(TAG, "Initialize navigation to friends activity button listener");
		btnGoToFriends.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SettingsActivity.this, FriendsActivity.class);
				startActivity(intent);
				overridePendingTransition(R.anim.swipe_right_in, R.anim.swipe_right_out);
				finish();
			}
		});

		Log.d(TAG, "Initialize update user details button listener");
		btnUpdateDetails.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						Looper.prepare();
						String password = etPassword.getText().toString();
						String confirmPassword = etConfirmPassword.getText().toString();
						String displayName = etDisplayName.getText().toString();
						if (!validateInputFields(password, confirmPassword, displayName))
							return;
						AuthUtils.updateUserDetails(SettingsActivity.this, password, displayName);
						Looper.loop();
					}
				}).start();
				etPassword.setText("");
				etConfirmPassword.setText("");
				etDisplayName.setText("");
			}
		});
		Log.d(TAG, "Initialize user removal button listener");
		btnRemoveUser.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						Looper.prepare();
						AlertDialog dialog = DialogFactory.createUserRemovalConfirmation(SettingsActivity.this);
						dialog.show();
						Looper.loop();
					}
				}).start();
			}
		});
	}

	private boolean validateInputFields(String password, String confirmPassword, String displayName) {
		Log.d(TAG, "Validating correctness of parameters: password='" + password
				+ "', confirmPassword='" + confirmPassword + "', displayName='" + displayName + "'");
		String msg;
		if (password.isEmpty() && confirmPassword.isEmpty() && displayName.isEmpty()) {
			msg = "נא למלא סיסמה חדשה או כינוי חדש";
			Utils.showToast(this, msg);
			return false;
		}
		if (xor(password.isEmpty(), confirmPassword.isEmpty())) {
			msg = "נא למלא סיסמה ואישור סיסמה";
			Utils.showToast(this, msg);
			return false;
		}
		if (!password.isEmpty() && !confirmPassword.isEmpty()) {
			if (password.length() < 6 || confirmPassword.length() < 6) {
				msg = "נא למלא סיסמה עם לפחות 6 תווים";
				Utils.showToast(this, msg);
				return false;
			}
			if (!password.equals(confirmPassword)) {
				msg = "נא להזין את אותה סיסמה פעמיים";
				Utils.showToast(this, msg);
				return false;
			}
		}
		return true;
	}

	private boolean xor(boolean exp1, boolean exp2) {
		boolean res = exp1 && !exp2 || !exp1 && exp2;
		Log.v(TAG, "xor: " + exp1 + "^" + exp2 + "=" + res);
		return res;
	}
}
