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
import android.widget.Toast;
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

    // class variables
    Intent intent;
    TextView nowPlaying;
    // now playing and recently played JSON objects
    String npURL = "http://whrb-api.herokuapp.com/nowplaying";
    String rpURL = "http://whrb-api.herokuapp.com/recentplays/5";
    Thread t;
    // boolean for thread to stop
    boolean stop = false;
    NotificationManager mNotificationManager;
    // strings to hold previous nowPlaying/recently Played info
    String prevNowPlaying = "";
    boolean changed = false;

    /** Called when app is opened anew  */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // renders layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create an Intent to start PlayService when play button pressed
        intent = new Intent(getApplicationContext(), PlayService.class);

        // get nowPlaying TextView so we can update it as needed
        nowPlaying = (TextView) findViewById(R.id.nowPlaying);

        // create thread to query for playlist history and nowPlaying every 5 seconds
        t = new Thread(new Runnable() {
            @Override
            public void run(){
                while (!stop){
                    // create a new JSON object request to get nowPlaying info
                    JsonObjectRequest req1 = new JsonObjectRequest
                            (Request.Method.GET, npURL, null, new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    // handle errors and get info
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
                                    JSONObject info = null;
                                    try {
                                        info = (JSONObject) response.get("ShowInfo");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    String show = "";
                                    try {
                                        assert info != null;
                                        show = (String) info.get("ShowName");
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                    // create text to be displayed in nowPlaying TextView and update
                                    String np ="Now Playing: " + title + " by " + artist + " on "
                                            + show;
                                    // if nowPlaying has changed, update UI
                                    if (!prevNowPlaying.equals(title + artist)){
                                        changed = true;
                                        prevNowPlaying = title + artist;
                                        nowPlaying.setText(np);
                                    }

                                    // creates ongoing notification that updates nowPlaying
                                    if (isServiceRunning(PlayService.class)){
                                        mNotificationManager =
                                                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0,
                                                new Intent(getApplicationContext(), MainActivity.class),
                                                PendingIntent.FLAG_UPDATE_CURRENT);
                                        Notification.Builder notification =
                                                new Notification.Builder(getApplicationContext())
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
                                    // when Volley returns an error, tell user that there is no network
                                    Toast toast = Toast.makeText(getApplicationContext(),
                                            "No network connection", Toast.LENGTH_SHORT);
                                    toast.show();
                                    // stops thread
                                    stop = true;
                                }
                            });
                    // second Json request, this time for an array of recently played tracks
                    JsonArrayRequest req2 = new JsonArrayRequest
                            (Request.Method.GET, rpURL, null, new Response.Listener<JSONArray>() {
                                @Override
                                public void onResponse(JSONArray response) {
                                    // store listView entries in an array of strings
                                    String[] recent_plays = new String[response.length()];
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
                                        // creates date/time format
                                        SimpleDateFormat hr_24 = new SimpleDateFormat("HH:mm:ss");
                                        SimpleDateFormat hr_12 = new SimpleDateFormat("K:mm a, z");
                                        Date hr_24dt = null;
                                        try {
                                            hr_24dt = hr_24.parse(time);
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                        time = hr_12.format(hr_24dt);

                                        recent_plays[i] = time + " | " + title + " by " + artist;
                                    }

                                    // only update UI when song is changed
                                    if (changed){
                                        // update listView with new information on playlist history
                                        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                                                android.R.layout.simple_list_item_1, recent_plays);
                                        ListView listView = (ListView) findViewById(R.id.listView);
                                        listView.setAdapter(adapter);
                                        changed = false;
                                    }

                                }
                                },new Response.ErrorListener()
                                {
                                    @Override
                                    public void onErrorResponse (VolleyError error){
                                        // when Volley returns an error, tell user that there is no network
                                        Toast toast = Toast.makeText(getApplicationContext(),
                                                "No network connection", Toast.LENGTH_SHORT);
                                        toast.show();
                                        // stops thread
                                        stop = true;
                                    }
                                }
                            );

                    // add requests to RequestQueue to be processed
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

        // actually start above thread
        t.start();
    }

    /** Called when app is opened from a running state (but in the background) */
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

    /** Called when app is destroyed via backspace key or manually */
    @Override
    protected void onDestroy(){
        stop=true;
        super.onDestroy();
    }


    /** Starts PlayService when button is toggle "on" */
    public void onToggleClicked(View view){
        boolean on = ((ToggleButton)view).isChecked();

        if (on){
            rememberToggleState(true);
            startService(intent);
        }
        else {
            rememberToggleState(false);
            stopService(intent);
        }

    }

    /** Checks if PlayService is running */
    private boolean isServiceRunning(Class<?> PlayService){
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (PlayService.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /** Remembers toggle state of button */
    private void rememberToggleState(boolean state){
        SharedPreferences.Editor editor = getSharedPreferences("com.example.james.whrb953fm",
                MODE_PRIVATE).edit();
        editor.putBoolean("checked", state);
        editor.apply();
    }



}
