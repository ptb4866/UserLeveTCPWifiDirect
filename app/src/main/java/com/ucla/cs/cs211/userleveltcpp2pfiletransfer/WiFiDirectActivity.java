
package com.ucla.cs.cs211.userleveltcpp2pfiletransfer;


import java.util.ArrayList;
import java.util.List;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

//import com.ucla.cs.cs211.userleveltcpp2pfiletransfer.WifiDirectActivityDialog.WifiDirectActivityFragment;

public class WiFiDirectActivity extends Activity {


    //public final int fileRequestID = 55;
    public final int port = 8880;
    public final String TAG = "wifiDirectActivity";

    private WifiP2pManager mManager;
    private Channel mChannel;
    private BroadcastReceiver mReceiver;

    private IntentFilter mIntentFilter;


    public Intent serverServiceIntent;
    public boolean serverThreadActive;

    private WifiP2pDevice targetDevice;
    private List<WifiP2pDevice> itemsList = new ArrayList<WifiP2pDevice>();


    ProgressDialog progressDialog = null;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serverServiceIntent = null;
        serverThreadActive = false;

        //Block auto opening keyboard
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);


        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();


        findViewById(R.id.disconnect).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onFailure(int reasonCode) {
                        Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);
                        Toast.makeText(WiFiDirectActivity.this, "Disconnect failed", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onSuccess() {

                        Toast.makeText(WiFiDirectActivity.this, "Disconnected Sucessfully", Toast.LENGTH_SHORT).show();

                    }

                });

            }

        });

    }


    public void displayPeers(final WifiP2pDeviceList peers) {
        //Dialog to show errors/status

        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setTitle("WiFi Direct Connection View");

        //Get list view
        ListView peerView = (ListView) findViewById(R.id.peers_listview);

        //Make array list
        ArrayList<String> peersStringArrayList = new ArrayList<String>();

        //Fill array list with strings of peer names
        for (WifiP2pDevice wd : peers.getDeviceList()) {
            peersStringArrayList.add(wd.deviceName);
        }

        //Set list view as clickable
        peerView.setClickable(true);

        //Make adapter to connect peer data to list view
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, peersStringArrayList.toArray());

        //Show peer data in listview
        peerView.setAdapter(arrayAdapter);


        peerView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> arg0, View view, int arg2, long arg3) {

                //Get string from textview
                TextView tv = (TextView) view;

                WifiP2pDevice device = null;

                final WifiP2pDevice deviceTmp;
                //Search all known peers for matching name
                for (WifiP2pDevice wd : peers.getDeviceList()) {
                    if (wd.deviceName.equals(tv.getText()))
                        device = wd;
                }
                deviceTmp = device;
                if (deviceTmp != null) {

                    dialog.setTitle("Connect To Peer")
                            .setMessage("Select 'OK' to connect")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // continue with delete

                                    connectToPeer(deviceTmp);
                                }
                            })
                            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();


                    //Connect to selected peer
                    //connectToPeer(device);

                } else {
                    dialog.setMessage("Failed");
                    dialog.show();

                }
            }
            // TODO Auto-generated method stub
        });

    }


    public void searchForPeers(View view) {
        //Discover peers, no call back method given
        //   mManager.discoverPeers(mChannel, null);

        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reasonCode) {

                Toast.makeText(WiFiDirectActivity.this, "Discovery Failed", Toast.LENGTH_SHORT).show();
            }
        });


    }


    public void connectToPeer(final WifiP2pDevice wifiPeer) {
        this.targetDevice = wifiPeer;

        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = wifiPeer.deviceAddress;
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            public void onSuccess() {

                setConnectionStatus("Connection to " + targetDevice.deviceName + " sucessful");


            }

            public void onFailure(int reason) {

                setConnectionStatus("Connection to " + targetDevice.deviceName + " failed");

            }
        });

    }


    public void setWifiStatus(String message) {
        TextView wifiEnableStatusText = (TextView) findViewById(R.id.wifi_enabled_text);
        wifiEnableStatusText.setText(message);
    }


    public void setConnectionStatus(String message) {

        TextView connectionStatusText = (TextView) findViewById(R.id.connection_status_text);
        connectionStatusText.setText(message);


    }






        //register the broadcast receiver with the intent values to be matched
    @Override
    protected void onResume() {
        super.onResume();
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(mReceiver, mIntentFilter);
    }

    //unregister the broadcast receiver
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bar, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "WiFiDirect Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.ucla.cs.cs211.userleveltcpp2pfiletransfer/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "WiFiDirect Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.ucla.cs.cs211.userleveltcpp2pfiletransfer/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
}
