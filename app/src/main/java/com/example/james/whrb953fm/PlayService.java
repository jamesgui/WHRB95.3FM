package com.example.james.whrb953fm;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.widget.Toast;

import java.io.IOException;

/**
 * Service to play music using MediaPlayer
 */
public class PlayService extends Service implements MediaPlayer.OnPreparedListener {
    MediaPlayer mediaPlayer = null;

    public void onCreate(){
        super.onCreate();
    }

    /** Called when service is started */
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null){
            // handle offline errors
            if (!isOnline()) {
                Toast toast = Toast.makeText(getApplicationContext(), "No network connection",
                        Toast.LENGTH_SHORT);
                toast.show();
                stopService(intent);
            }

            // initialize url string and mediaPlayer
            String url = "http://stream.whrb.org:8000/whrb-mp3";
            mediaPlayer = new MediaPlayer();

            // make it so the mediaPlayer streams from our url
            try {
                mediaPlayer.setDataSource(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // prepares mediaPlayer in another thread so it loads without interrupting main thread
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.prepareAsync();
            // let user know app is loading
            Toast loading = Toast.makeText(getApplicationContext(), "Loading stream...",
                    Toast.LENGTH_LONG);
            loading.show();
            // pendingIntent returns to app when notification is clicked
            PendingIntent pi = PendingIntent.getActivity(this, 0,
                    new Intent(getApplicationContext(), MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            // create new Notification so we can run this as a foreground service
            // i.e. music keeps playing even when app is away
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
        // just need this because Android needs it
        return START_STICKY;
    }

    /** Called when MediaPlayer is ready */
    public void onPrepared(MediaPlayer player) {
        Toast loaded = Toast.makeText(getApplicationContext(), "Loaded!",
                Toast.LENGTH_SHORT);
        loaded.show();
        player.start();
    }

    /** Called when service is stopped */
    public void onDestroy(){
        if (mediaPlayer != null){
            // take care of loose thread
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    /** Need this method to be declared because we implement an interface but unused */
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    /** checks if network is connected */
    public boolean isOnline(){
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}
