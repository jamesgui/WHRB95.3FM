package com.example.james.whrb953fm;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {

    Intent intent;
    TextView nowPlaying;
    String npURL = "http://whrb-api.herokuapp.com/nowplaying";
    String rpURL = "http://whrb-api.herokuapp.com/recentplays/5";
    Thread t;
    boolean stop = false;
    NotificationManager mNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        intent = new Intent(getApplicationContext(), PlayService.class);
        nowPlaying = (TextView) findViewById(R.id.nowPlaying);

        // create thread to query for playlist history and current track
        t = new Thread(new Runnable() {
            @Override
            public void run(){
                while (!stop){
                    JsonObjectRequest req1 = new JsonObjectRequest
                            (Request.Method.GET, npURL, null, new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    String title = null;
                                    try {
                                        title = (String) response.get("SongName");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    String artist = null;
                                    try {
                                        artist = (String) response.get("ArtistName");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    String np ="Now Playing: " + title + " by " + artist;
                                    nowPlaying.setText(np);
                                    if (isServiceRunning(PlayService.class)){
                                        mNotificationManager =
                                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                                                new Intent(getApplicationContext(), MainActivity.class),
                                                PendingIntent.FLAG_UPDATE_CURRENT);
                                        Notification.Builder notification = new Notification.Builder(getApplicationContext())
                                                .setContentText("Streaming from WHRB")
                                                .setContentIntent(pi)
                                                .setOngoing(true).setContentTitle("WHRB 95.3 FM")
                                                .setContentText(np)
                                                .setSmallIcon(android.R.drawable.ic_media_play)
                                                .setAutoCancel(false);
                                        mNotificationManager.notify(1,notification.build());

                                    }
                                }
                            }, new Response.ErrorListener() {

                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    // TODO Auto-generated method stub
                                }
                            });
                    JsonArrayRequest req2 = new JsonArrayRequest
                            (Request.Method.GET, rpURL, null, new Response.Listener<JSONArray>() {
                                @Override
                                public void onResponse(JSONArray response) {
                                    String[] recent_plays = new String[response.length()+1];
                                    recent_plays[0] = "Last 5 Tracks Played";
                                    for (int i = 0; i < response.length(); i++) {
                                        JSONObject song = null;
                                        try {
                                            song = (JSONObject) response.get(i);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        String title = null;
                                        try {
                                            assert song != null;
                                            title = (String) song.get("SongName");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        String artist = null;
                                        try {
                                            artist = (String) song.get("ArtistName");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        String time = null;
                                        try{
                                            time = (String) song.get("Timestamp");
                                        } catch (JSONException e){
                                            e.printStackTrace();
                                        }
                                        SimpleDateFormat hr_24 = new SimpleDateFormat("HH:mm:ss");
                                        SimpleDateFormat hr_12 = new SimpleDateFormat("K:mm a, z");
                                        Date hr_24dt = null;
                                        try {
                                            hr_24dt = hr_24.parse(time);
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                        time = hr_12.format(hr_24dt);

                                        String history = time + " | " + title + " by " + artist;
                                        recent_plays[i+1] = history;
                                    }
                                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                            MainActivity.this, android.R.layout.simple_list_item_1,
                                            recent_plays);
                                    ListView listView = (ListView) findViewById(R.id.listView);
                                    listView.setAdapter(adapter);
                                }
                                },new Response.ErrorListener()
                                {
                                    @Override
                                    public void onErrorResponse (VolleyError error){
                                    // TODO Auto-generated method stub
                                    }
                                }
                            );

                    // Access the RequestQueue through your singleton class.
                    RequestQueueSingleton.getInstance(getApplicationContext()).addToRequestQueue(req1);
                    RequestQueueSingleton.getInstance(getApplicationContext()).addToRequestQueue(req2);

                    // query every 5 seconds
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }

        });
        t.start();
    }


    @Override
    protected void onStart()
    {
        super.onStart();

        // ensure toggle state is remembered
        SharedPreferences sharedPrefs = getSharedPreferences("com.example.james.whrb953fm",
                MODE_PRIVATE);
        ToggleButton toggle = (ToggleButton)findViewById(R.id.button);
        toggle.setChecked(sharedPrefs.getBoolean("checked", true));
    }

    @Override
    protected void onDestroy(){

        stop=true;
        super.onDestroy();
    }



    public void onToggleClicked(View view){
        boolean on = ((ToggleButton)view).isChecked();

        if (on){
            SharedPreferences.Editor editor = getSharedPreferences("com.example.james.whrb953fm",
                    MODE_PRIVATE).edit();
            editor.putBoolean("checked", true);
            editor.apply();
            startService(intent);
        }
        else {
            SharedPreferences.Editor editor = getSharedPreferences("com.example.james.whrb953fm",
                    MODE_PRIVATE).edit();
            editor.putBoolean("checked", false);
            editor.apply();
            stopService(intent);
        }

    }
    private boolean isServiceRunning(Class<?> PlayService){
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PlayService.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }



}
