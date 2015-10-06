package be.ehb.doorwatch;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class RegisterIntentService extends IntentService {
    private static final String TAG = "RegIntentService";
    private static final ArrayList<String> TOPICS = new ArrayList<>(Arrays.asList("global"));
    private SharedPreferences sharedPreferences;

    public RegisterIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Intent returnIntent = new Intent(QuickstartPreferences.REGISTRATION_SERVICE_COMPLETE);
        sharedPreferences = getSharedPreferences(QuickstartPreferences.PREFS_NAME,0);
        //String doorID = intent.getStringExtra(QuickstartPreferences.DOOR_ID);
        String doorID = sharedPreferences.getString(QuickstartPreferences.DOOR_ID,null);
        if (!TextUtils.isEmpty(doorID)) {
            TOPICS.add(doorID.replaceAll("\\s",""));
        }

        switch (intent.getAction()){
            case QuickstartPreferences.REGISTER_ACTION:
                try {
                    // [START register_for_gcm]
                    // Initially this call goes out to the network to retrieve the token, subsequent calls
                    // are local.
                    // [START get_token]
                    InstanceID instanceID = InstanceID.getInstance(this);

                    String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    // [END get_token]
                    Log.i(TAG, "GCM Registration Token: " + token);

                    // TODO: Implement this method to send any registration to your app's servers.
                    sendRegistrationToServer(token);

                    // Subscribe to topic channels
                    subscribeTopic(token);

                    // You should store a boolean that indicates whether the generated token has been
                    // sent to your server. If the boolean is false, send the token to your server,
                    // otherwise your server should have already received the token.
                    returnIntent.putExtra(QuickstartPreferences.RETURN_STRING, QuickstartPreferences.REGISTRATION_SUCCEED);
                    // [END register_for_gcm]
                } catch (Exception e) {
                    Log.d(TAG, "Failed to complete token refresh", e);
                    // If an exception happens while fetching the new token or updating our registration data
                    // on a third-party server, this ensures that we'll attempt the update at a later time.
                    returnIntent.putExtra(QuickstartPreferences.RETURN_STRING, QuickstartPreferences.REGISTRATION_FAILED);
                }
                // Notify UI that registration has completed, so the progress indicator can be hidden.
                Log.d(TAG, "Sending broadcast");
                LocalBroadcastManager.getInstance(this).sendBroadcast(returnIntent);
                break;

            case QuickstartPreferences.UNREGISTER_ACTION:
                //Unsubscribing
                try {
                    Log.d(TAG, "Unsubscribing");
                    InstanceID instanceID = InstanceID.getInstance(this);

                    String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                            GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    unSubscribeTopic(token);
                    returnIntent.putExtra(QuickstartPreferences.RETURN_STRING, QuickstartPreferences.UNREGISTRATION_SUCCEED);
                } catch (IOException e) {
                    Log.d(TAG, "Unsubscribing failed", e);
                    returnIntent.putExtra(QuickstartPreferences.RETURN_STRING, QuickstartPreferences.UNREGISTRATION_FAILED);
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(returnIntent);
                break;
        }
    }


    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // Add custom implementation, as needed.
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopic(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            Log.d(TAG,"Subscribing to: "  + topic);
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]

    private void unSubscribeTopic(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.unsubscribe(token, "/topics/" + topic);
        }
    }
}
