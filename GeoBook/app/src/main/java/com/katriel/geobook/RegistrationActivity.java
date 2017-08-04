package com.katriel.geobook;

import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.katriel.geobook.bl.utils.AuthUtils;
import com.katriel.geobook.bl.utils.Utils;

public class RegistrationActivity extends AppCompatActivity {

	private EditText etEmail;
	private EditText etPassword;
	private EditText etConfirmPassword;
	private EditText etDisplayName;
	private Button btnRegister;
	private Button btnBackToLogin;

	// Log tag
	private static final String TAG = "RegistrationActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_registration);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		Utils.enableImmersiveMode(this);

		initializeControls();

		initializeListeners();
	}

	private void initializeControls() {
		etEmail = (EditText) findViewById(R.id.etRegistrationEmail);
		etPassword = (EditText) findViewById(R.id.etRegistrationPassword);
		etConfirmPassword = (EditText) findViewById(R.id.etConfirmPassword);
		etDisplayName = (EditText) findViewById(R.id.etDisplayName);
		btnRegister = (Button) findViewById(R.id.btnRegister);
		btnBackToLogin = (Button) findViewById(R.id.btnBackToLogin);
	}

	private void initializeListeners() {
		btnBackToLogin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
				overridePendingTransition(R.anim.swipe_right_in, R.anim.swipe_right_out);
			}
		});

		btnRegister.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String email = etEmail.getText().toString();
				final String password = etPassword.getText().toString();
				final String confirmPassword = etConfirmPassword.getText().toString();
				final String displayName = etDisplayName.getText().toString();
				if (!validateRegistrationFields(email, password, confirmPassword, displayName))
					return;
				Log.i(TAG, "Trying ot register new user with parameters:\nEmail: " + email + "\nPassword: " + password + "\nDisplay Name: " + displayName);
				AuthUtils.register(RegistrationActivity.this, MapsActivity.class, email, password, displayName);
			}
		});
	}

	private boolean validateRegistrationFields(String email, String password, String confirmPassword, String displayName) {
		String msg;
		if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || displayName.isEmpty()) {
			msg = "נא למלא את כל השדות";
			Utils.showToast(this,msg);
			return false;
		}
		if (email.split("@").length != 2) {
			msg = "נא למלא אימייל תקין";
			Utils.showToast(this,msg);
			return false;
		}
		if (password.length() < 6 || confirmPassword.length() < 6) {
			msg = "נא למלא סיסמה עם לפחות 6 תווים";
			Utils.showToast(this,msg);
			return false;
		}
		if (!password.equals(confirmPassword)) {
			msg = "נא להזין את אותה סיסמה פעמיים";
			Utils.showToast(this,msg);
			return false;
		}
		return true;
	}
}

