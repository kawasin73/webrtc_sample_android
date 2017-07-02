package com.kawasin73.webrtcsample;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.skyway.Peer.Browser.MediaConstraints;
import io.skyway.Peer.Browser.MediaStream;
import io.skyway.Peer.Browser.Navigator;
import io.skyway.Peer.CallOption;
import io.skyway.Peer.MediaConnection;
import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;
import io.skyway.Peer.PeerOption;

public class MainActivity extends AppCompatActivity {
    private static final int RECORD_AUDIO_REQUEST_ID = 1;
    private static final String TAG = "MainActivity";
    private List<String> idList = new ArrayList<String>();
    private MyAdapter adapter;
    private TextView idTextView;
    private Peer peer;
    private MediaConnection connection;
    private String currentPeerId = "";
    private String selectedPeerId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        am.setMode(AudioManager.MODE_IN_COMMUNICATION);

        checkAudioPermission();

        ListView listView = (ListView) findViewById(R.id.list_view);
        adapter = new MyAdapter(this, 0, idList);
        listView.setAdapter(adapter);

        PeerOption options = new PeerOption();
        options.key = BuildConfig.SKYWAY_API_KEY;
        options.domain = BuildConfig.SKYWAY_HOST;
        peer = new Peer(this, options);
        Navigator.initialize(peer);

        idTextView = (TextView) findViewById(R.id.id_text_view);

        Button refreshBtn = (Button) findViewById(R.id.refresh_btn);
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshAllPeers();
            }
        });

        Button callBtn = (Button) findViewById(R.id.call_btn);
        callBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                call();
            }
        });

        Button closeBtn = (Button) findViewById(R.id.close_btn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MainActivity.this.connection != null) {
                    MainActivity.this.connection.close();
                    MainActivity.this.connection = null;
                }
            }
        });

        peer.on(Peer.PeerEventEnum.OPEN, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                if (o instanceof String) {
                    final String id = (String) o;
                    currentPeerId = id;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            idTextView.setText("ID: " + id);
                        }
                    });
                }
            }
        });

        peer.on(Peer.PeerEventEnum.CALL, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                Log.d(TAG, "CALL Event is Received");
                if (o instanceof MediaConnection) {
                    MediaConnection connection = (MediaConnection) o;
                    MediaStream stream = MainActivity.this.getMediaStream();

                    connection.answer(stream);

                    setConnectionCallback(connection);

                    MainActivity.this.connection = connection;
                    Log.d(TAG, "CALL Event is Received and Set");
                }
            }
        });
    }

    private void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        RECORD_AUDIO_REQUEST_ID);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RECORD_AUDIO_REQUEST_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void refreshAllPeers() {
        Log.d(TAG, "Refreshing");
        peer.listAllPeers(new OnCallback() {
            @Override
            public void onCallback(Object o) {
                if (o instanceof JSONArray) {
                    JSONArray array = (JSONArray) o;
                    idList.clear();
                    for (int i = 0; i < array.length(); i++) {
                        try {
                            String id = array.getString(i);
                            idList.add(id);
                            Log.d(TAG, "Fetched PeerId: " + id);
                            if (!Objects.equals(id, currentPeerId)) {
                                selectedPeerId = id;
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Parse ListAllPeer", e);
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
    }

    private void call() {
        Log.d(TAG, "Calling");
        if (peer == null) {
            Log.i(TAG, "Call but peer is null");
            return;
        }

        Log.d(TAG, String.format("peer.isDestroyed: %b", peer.isDestroyed));
        Log.d(TAG, String.format("peer.isDisconnected: %b", peer.isDisconnected));
        Log.d(TAG, "selectedPeerId: " + selectedPeerId);

        if (!peer.isDestroyed && !peer.isDisconnected) {
            MediaStream stream = getMediaStream();
            CallOption option = new CallOption();
            option.metadata = "test"; // TODO: metadata
            final MediaConnection connection = peer.call(selectedPeerId, stream, option);
            if (connection == null) {
                Log.d(TAG, "MediaConnection is null");
                return;
            }

            setConnectionCallback(connection);

            this.connection = connection;
        }
    }

    private void setConnectionCallback(final MediaConnection connection) {
        connection.on(MediaConnection.MediaEventEnum.CLOSE, new OnCallback() {
            @Override
            public void onCallback(Object o) {
                Log.d(TAG, "Close Event is Received");
                // TODO:
                connection.close();
                MainActivity.this.connection = null;
            }
        });
    }

    private MediaStream getMediaStream() {
        MediaConstraints constraints = new MediaConstraints();
        constraints.videoFlag = false;
        constraints.audioFlag = true;
        return Navigator.getUserMedia(constraints);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (peer != null) {
            peer.destroy();
        }
        if (connection != null) {
            connection.close();
            this.connection = null;
        }
    }

    private class MyAdapter extends ArrayAdapter<String> {
        private LayoutInflater inflater;

        MyAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<String> objects) {
            super(context, resource, objects);
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View view = inflater.inflate(R.layout.list_item, null, false);
            TextView textView = (TextView) view.findViewById(R.id.list_item_text);
            String name = getItem(position);
            textView.setText(name);
            return view;
        }
    }
}
