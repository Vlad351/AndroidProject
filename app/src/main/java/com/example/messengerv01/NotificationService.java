package com.example.messengerv01;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import androidx.annotation.Nullable;



public class NotificationService extends Service {

    //onStartCommand plays notification sound once
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final MediaPlayer player = MediaPlayer.create(this,R.raw.notification );
        player.start();
        onDestroy();
        return START_STICKY;

    }



    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
