package com.kawasin73.webrtcsample;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    private static String TAG = "MainActivity";
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
                Log.d(TAG, String.valueOf(o.getClass()));
                Log.d(TAG, String.valueOf(o));
                if (o instanceof MediaConnection) {
                    MediaConnection connection = (MediaConnection) o;
                    MediaStream stream = MainActivity.this.getMediaStream();
                    connection.answer(stream);
                    MainActivity.this.connection = connection;
                }
            }
        });
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
            final MediaConnection connection =  peer.call(selectedPeerId, stream, option);
            if (connection == null) {
                Log.d(TAG, "MediaConnection is null");
                return;
            }

            connection.on(MediaConnection.MediaEventEnum.CLOSE, new OnCallback() {
                @Override
                public void onCallback(Object o) {
                    // TODO:
                    connection.close();
                    MainActivity.this.connection = null;
                }
            });

            this.connection = connection;
        }
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
