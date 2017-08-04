package com.katriel.geobook;

import android.content.Intent;
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

public class LoginActivity extends AppCompatActivity {
	// Controls
	private Button btnLogin;
	private Button btnRegistration;
	private EditText etEmail;
	private EditText etPassword;

	// Log tag
	private static final String TAG = "LoginActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		Utils.enableImmersiveMode(this);

		initializeControls();

		initializeListeners();
	}

	private void initializeControls() {
		btnLogin = (Button) findViewById(R.id.btnLogin);
		btnRegistration = (Button) findViewById(R.id.btnRegistration);
		etEmail = (EditText) findViewById(R.id.etEmail);
		etPassword = (EditText) findViewById(R.id.etPassword);
	}

	private void initializeListeners() {
		btnLogin.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String email = etEmail.getText().toString();
				final String password = etPassword.getText().toString();
				if (!validateLoginFields(email, password))
					return;
				Log.i(TAG, "Authenticating with email '" + email + "' and password '" + password + "'");
				AuthUtils.signIn(LoginActivity.this, MapsActivity.class, email, password);
			}
		});

		btnRegistration.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(LoginActivity.this, RegistrationActivity.class);
				startActivity(intent);
				overridePendingTransition(R.anim.swipe_left_in, R.anim.swipe_left_out);
			}
		});
	}

	private boolean validateLoginFields(String email, String password) {
		String msg;
		if (email.isEmpty() || password.isEmpty()) {
			msg = "נא למלא אימייל וסיסמה";
			Utils.showToast(this,msg);
			return false;
		}
		if (email.split("@").length != 2) {
			msg = "נא למלא אימייל תקין";
			Utils.showToast(this,msg);
			return false;
		}
		if (password.length() < 6) {
			msg = "נא למלא סיסמה עם לפחות 6 תווים";
			Utils.showToast(this,msg);
			return false;
		}
		return true;
	}
}
