package com.example.fredrik.caliditas;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ConnectActivity extends AppCompatActivity {
    private static final String TAG = "ConnectActivity";

    private Toolbar toolbar;
    private ListView availableDevices;
    private Button disconnectButton;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice currentDevice;
    private Context context;
    private boolean bluetoothReady = false;

    private List<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
    private List<String> deviceNameList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        BluetoothDevice device = getIntent().getParcelableExtra("device");

        if (device != null) {
            currentDevice = device;
        }

        context = this;
        availableDevices = (ListView) findViewById(R.id.availableDevices);
        disconnectButton = (Button) findViewById(R.id.disconnectButton);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Anslut termometer");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothStatusReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        LocalBroadcastManager.getInstance(this).registerReceiver(connectionStatusReceiver, new IntentFilter("connectionStatus"));

        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent disconnectIntent = new Intent("closeConnection");
                LocalBroadcastManager.getInstance(ConnectActivity.this).sendBroadcast(disconnectIntent);
                currentDevice = null;

                Toast.makeText(
                        getApplicationContext(),
                        "Kopplar ifrån..",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        // Set an item click listener for ListView
        availableDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedDevice = (String) parent.getItemAtPosition(position);

                if (bluetoothReady) {
                    for (BluetoothDevice device : deviceList) {
                        if (device.getName().equals(selectedDevice)) {
                            if (currentDevice == null || !currentDevice.equals(device)) {
                                Intent bluetoothWorker = new Intent(context, BluetoothSocketService.class);
                                bluetoothWorker.putExtra("targetDevice", device);
                                startService(bluetoothWorker);
                            } else {
                                Toast.makeText(
                                        getApplicationContext(),
                                        "Du är redan ansluten till denna enhet",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                    }
                } else {
                    checkBluetoothStatus();
                }
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        checkBluetoothStatus();
    }

    private final void checkBluetoothStatus() {
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Din enhet stödjer inte bluetooth",Toast.LENGTH_SHORT).show();

            bluetoothReady = false;
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);
        } else {
            if (deviceList.isEmpty()) {
                scanForPairedDevices();
            }

            bluetoothReady = true;
        }
    }

    private final void scanForPairedDevices() {
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        deviceNameList.clear();
        deviceList.clear();

        if (bondedDevices.isEmpty()) {
            Toast.makeText(getApplicationContext(),"Para först ihop din enhet via inställningarna",Toast.LENGTH_SHORT).show();
        } else {
            for (BluetoothDevice device : bondedDevices) {
                deviceNameList.add(device.getName());
                deviceList.add(device);
            }

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceNameList);
            availableDevices.setAdapter(arrayAdapter);
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver bluetoothStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(bluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, bluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "BT State off");
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "BT State turning off");
                        bluetoothReady = false;
                        break;

                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "BT State on");
                        bluetoothReady = true;
                        scanForPairedDevices();
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "BT State turning on");
                        break;
                }
            }
        }
    };

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver connectionStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received connectionStatus receiver");
            String status = intent.getStringExtra("status");
            switch (status) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    BluetoothDevice device = intent.getParcelableExtra("device");
                    currentDevice = device;
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    currentDevice = null;
            }

        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_connect, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_refreshBondedDevices:
                scanForPairedDevices();

                return false;
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(connectionStatusReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothStatusReceiver);
    }
}
