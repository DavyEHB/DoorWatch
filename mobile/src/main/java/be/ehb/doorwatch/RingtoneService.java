package be.ehb.doorwatch;

import android.app.IntentService;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

public class RingtoneService extends IntentService {
    private static final String TAG = "RingtoneService";
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_STOP = "ACTION_STOP";

    private static Ringtone ringtone;

    public RingtoneService() {
        super("RingtoneService");
    }

    private void playRingtone() {
        //this will sound the alarm tone
        //this will sound the alarm once, if you wish to
        //raise alarm in loop continuously then use MediaPlayer and setLooping(true)
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        ringtone = RingtoneManager.getRingtone(this, alarmUri);
        ringtone.play();
    }

    private void stopRingtone() {
        ringtone.stop();
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Handeling Intent");
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) {
                playRingtone();
            } else if (ACTION_STOP.equals(action)){
                stopRingtone();
            }
        }
    }
}
