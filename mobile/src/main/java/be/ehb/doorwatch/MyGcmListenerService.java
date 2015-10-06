/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package be.ehb.doorwatch;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

public class MyGcmListenerService extends GcmListenerService {
    private static final String TAG = "MyGcmListenerService";
    private SharedPreferences sharedPreferences;

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "From:" + from);
        sharedPreferences = getSharedPreferences(QuickstartPreferences.PREFS_NAME,0);
        String topic = "/topics/" + sharedPreferences.getString(QuickstartPreferences.DOOR_ID,null);
        Log.d(TAG, "Topic:" + topic);
        if (from.equals(topic)){
            Log.d(TAG, "Door called");
            Intent startIntent = new Intent(this,IncommingActivity.class);
            //startIntent.setAction(RingtoneService.ACTION_PLAY);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
    }
}
