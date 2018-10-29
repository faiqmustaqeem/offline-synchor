package com.edgeon.faiq.synchor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AutoStart extends BroadcastReceiver {
    public void onReceive(Context context, Intent arg1) {
        if (!BackgroundSynchor.isServiceRunning) {
            Intent intent = new Intent(context, BackgroundSynchor.class);
            context.startService(intent);
        }

        Log.i("status", "started on boot");
    }
}