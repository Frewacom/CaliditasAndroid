package com.example.fredrik.caliditas;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
    private BluetoothAdapter bluetoothAdapter;
    private Context context;

    private List<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
    private List<String> deviceNameList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        context = this;
        availableDevices = (ListView) findViewById(R.id.availableDevices);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set an item click listener for ListView
        availableDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedDevice = (String) parent.getItemAtPosition(position);

                for (BluetoothDevice device : deviceList) {
                    if (device.getName().equals(selectedDevice)) {
                        Intent bluetoothWorker = new Intent(context, BluetoothSocketService.class);
                        bluetoothWorker.putExtra("targetDevice", device);
                        startService(bluetoothWorker);
                    }
                }
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Din enhet stödjer inte bluetooth",Toast.LENGTH_SHORT).show();
        } else if (!bluetoothAdapter.isEnabled())
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);
        } else {
            Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

            if (bondedDevices.isEmpty()) {
                Toast.makeText(getApplicationContext(),"Para ihop din enhet först via inställningarna",Toast.LENGTH_SHORT).show();
            } else {
                for (BluetoothDevice device : bondedDevices) {
                    deviceNameList.add(device.getName());
                    deviceList.add(device);
                }

                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, deviceNameList);
                availableDevices.setAdapter(arrayAdapter);
            }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
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
                        break;

                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "BT State on");
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "BT State turning on");
                        break;
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
