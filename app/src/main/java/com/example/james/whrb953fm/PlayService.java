package com.example.james.whrb953fm;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import java.io.IOException;

public class PlayService extends Service implements MediaPlayer.OnPreparedListener {
    MediaPlayer mediaPlayer = null;

    public void onCreate(){
        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null){
            String url = "http://stream.whrb.org:8000/whrb-mp3";

            mediaPlayer = new MediaPlayer();

            try {
                mediaPlayer.setDataSource(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.prepareAsync();
            PendingIntent pi = PendingIntent.getActivity(this, 0,
                    new Intent(getApplicationContext(), MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new Notification.Builder(this)
                    .setContentText("Streaming from WHRB")
                    .setOngoing(true)
                    .setContentIntent(pi).setContentTitle("WHRB 95.3 FM")
                    .setContentText("Click to return")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setAutoCancel(false)
                    .build();
            startForeground(1, notification);
        }
        return START_STICKY;
    }

    /** Called when MediaPlayer is ready */
    public void onPrepared(MediaPlayer player) {
        player.start();
    }

    public void onDestroy(){
        if (mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public IBinder onBind(Intent intent)
    {
        return null;
    }
}