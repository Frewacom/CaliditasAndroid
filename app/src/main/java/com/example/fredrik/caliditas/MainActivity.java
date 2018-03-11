package com.example.fredrik.caliditas;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import static java.lang.Math.round;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ConnectActivity";
    private Toolbar toolbar;
    private TextView temperature;
    private TextView humidity;
    private BluetoothDevice currentDevice;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperature = (TextView) findViewById(R.id.temperature);
        humidity = (TextView) findViewById(R.id.humidity);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Ej ansluten");
        setSupportActionBar(toolbar);

        LocalBroadcastManager.getInstance(this).registerReceiver(socketReceiver, new IntentFilter("incomingData"));
        LocalBroadcastManager.getInstance(this).registerReceiver(connectionStatusReceiver, new IntentFilter("connectionStatus"));
    }

    private final BroadcastReceiver socketReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String data = intent.getStringExtra("data");
            BluetoothDevice device = intent.getParcelableExtra("device");

            if (device != null) {
                if (currentDevice == null) {
                    currentDevice = device;
                } else {
                    if (!currentDevice.equals(device)) {
                        currentDevice = device;
                    }
                }

                if (!toolbar.getTitle().equals(currentDevice.getName())) {
                    toolbar.setTitle(currentDevice.getName());
                }
            } else {
                toolbar.setTitle("Ej ansluten");
            }

            data = data.replace("#", "");
            data = data.replaceAll("/[^0-9.]/g", "");
            String[] values = data.split(",");

            if (values.length == 2) {
                int temp = Math.round(Float.parseFloat(values[0]));
                int hum = Math.round(Float.parseFloat(values[1]));

                temperature.setText(temp + "°C");
                humidity.setText("Luftfuktighet: " + hum + "%");
            }
        }
    };

    private final BroadcastReceiver connectionStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received connectionStatus receiver");
            String status = intent.getStringExtra("status");
            switch (status) {
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    currentDevice = null;
                    toolbar.setTitle("Ej ansluten");
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.appmenu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_bluetooth:
                Intent connectIntent = new Intent(MainActivity.this, ConnectActivity.class);
                if (currentDevice != null) {
                    connectIntent.putExtra("device", currentDevice);
                }
                startActivity(connectIntent);

                return false;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(connectionStatusReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(socketReceiver);
    }
}
