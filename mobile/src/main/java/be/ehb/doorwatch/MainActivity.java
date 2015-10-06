package be.ehb.doorwatch;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends Activity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private static final String TAG = "MainActivity";


    private EditText edDoorID;
    private Button btnPair;
    private Button btnUnpair;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ProgressBar mRegistrationProgressBar;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edDoorID = (EditText) findViewById(R.id.edDoorID);
        btnPair = (Button) findViewById(R.id.btnPair);
        btnUnpair = (Button) findViewById(R.id.btnUnpair);

        sharedPreferences = getSharedPreferences(QuickstartPreferences.PREFS_NAME,0);
        edDoorID.setText(sharedPreferences.getString(QuickstartPreferences.DOOR_ID,null));

        mRegistrationProgressBar = (ProgressBar) findViewById(R.id.registerProgress);
        mRegistrationBroadcastReceiver = new MyBroadcastReceiver();

        if (TextUtils.isEmpty(edDoorID.getText())){
            btnPair.setEnabled(false);
        } else {
            btnPair.setEnabled(true);
            if (sharedPreferences.getBoolean(QuickstartPreferences.AUTO_PAIR,false)) {
                register();
            }
        }

        edDoorID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (TextUtils.isEmpty(s)) {
                    btnPair.setEnabled(false);
                } else {
                    btnPair.setEnabled(true);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QuickstartPreferences.REGISTRATION_SERVICE_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    public void obBtnPairClick(View view) {
        register();
    }

    private void register() {
        mRegistrationProgressBar.setVisibility(ProgressBar.VISIBLE);
        btnPair.setEnabled(false);

        if (checkPlayServices()){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(QuickstartPreferences.DOOR_ID, edDoorID.getText().toString());
            editor.apply();
            // Start IntentService to register this application with GCM
            Intent intent = new Intent(this, RegisterIntentService.class);
            intent.setAction(QuickstartPreferences.REGISTER_ACTION);
            //intent.putExtra(QuickstartPreferences.DOOR_ID,edDoorID.getText().toString());
            startService(intent);
        } else
        {
            btnPair.setEnabled(true);
            mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
            Toast.makeText(this, R.string.Play_not_available, Toast.LENGTH_SHORT).show();
            btnPair.setEnabled(false);
        }
    }

    public void obBtnUnpairClick(View view) {
        mRegistrationProgressBar.setVisibility(ProgressBar.VISIBLE);
        btnUnpair.setEnabled(false);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(QuickstartPreferences.AUTO_PAIR,false);
        editor.apply();

        Intent intent = new Intent(this, RegisterIntentService.class);
        intent.setAction(QuickstartPreferences.UNREGISTER_ACTION);
        startService(intent);
    }

    class MyBroadcastReceiver extends BroadcastReceiver
    {
        private static final String TAG = "MyBroadcastReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra(QuickstartPreferences.RETURN_STRING);
            Log.d(TAG, "onReceive: " + action);
            switch (action){
                //Registration succeed return
                case QuickstartPreferences.REGISTRATION_SUCCEED:
                    mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                    btnPair.setEnabled(true);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(QuickstartPreferences.AUTO_PAIR,true);
                    editor.apply();

                    Toast.makeText(getApplicationContext(), "Regsitration to Google Cloud message succeed", Toast.LENGTH_SHORT).show();
                    btnPair.setVisibility(View.GONE);
                    btnUnpair.setVisibility(View.VISIBLE);

                    break;

                //Registration failed return
                case QuickstartPreferences.REGISTRATION_FAILED:
                    mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                    btnPair.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "Registration to GCM failed", Toast.LENGTH_SHORT).show();
                    break;

                //Unregistration succeed return
                case QuickstartPreferences.UNREGISTRATION_SUCCEED:
                    mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                    btnUnpair.setEnabled(true);
                    btnUnpair.setVisibility(View.GONE);
                    btnPair.setVisibility(View.VISIBLE);
                    Toast.makeText(getApplicationContext(),"Unregistration succeed",Toast.LENGTH_SHORT).show();
                    break;

                //Unregistration failed return
                case QuickstartPreferences.UNREGISTRATION_FAILED:
                    mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                    btnUnpair.setEnabled(true);
                    Toast.makeText(getApplicationContext(),"Unregistration failed",Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}
