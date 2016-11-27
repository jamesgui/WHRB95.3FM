package com.example.james.whrb953fm;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class MainActivity extends AppCompatActivity {

    Intent intent;
    TextView nowPlaying;
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences sharedPrefs = getSharedPreferences("com.example.james.whrb953fm",
                MODE_PRIVATE);
        ToggleButton toggle = (ToggleButton)findViewById(R.id.button);
        toggle.setChecked(sharedPrefs.getBoolean("checked", true));

        intent = new Intent(getApplicationContext(), PlayService.class);
        nowPlaying = (TextView) findViewById(R.id.nowPlaying);

        new JsonTask().execute("http://whrb-api.herokuapp.com/nowplaying");

    }

    // read json for Now Playing and Playlist code from
    // http://stackoverflow.com/questions/33229869/get-json-data-from-url-using-android
    private class JsonTask extends AsyncTask<String, String, String> {

        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(MainActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        protected String doInBackground(String... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuilder buffer = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    buffer.append(line).append("\n");
                    Log.d("Response: ", "> " + line);
                }
                return buffer.toString();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }
            JSONObject song = null;
            try {
                song = new JSONObject(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String title = null;
            try {
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
            nowPlaying.setText("Now Playing: " + title + " by " + artist);
        }
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


}
