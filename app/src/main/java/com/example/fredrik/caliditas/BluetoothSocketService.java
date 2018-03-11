package com.example.fredrik.caliditas;

import android.app.IntentService;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothSocketService extends IntentService {
    private static final String TAG = "BluetoothSocketService";

    private BluetoothSocket socket = null;
    private BluetoothDevice currentDevice = null;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean plannedDisconnect = false;

    private UUID deviceUUID;
    private Handler handler;

    public BluetoothSocketService() {
        super("BluetoothSocketService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Bluetooth service started");

        handler = new Handler();
        deviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        IntentFilter bluetoothConnectionFilter = new IntentFilter();
        bluetoothConnectionFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        bluetoothConnectionFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothConnectionReceiver, bluetoothConnectionFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(closeConnectionReceiver, new IntentFilter("closeConnection"));
    }

    private void DisplayToast(final String message, final int length) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BluetoothSocketService.this, message, length).show();
            }
        });
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        BluetoothDevice targetDevice = intent.getParcelableExtra("targetDevice");

        try {
            socket = targetDevice.createRfcommSocketToServiceRecord(deviceUUID);

            try {
                DisplayToast("Ansluter till termometern", Toast.LENGTH_SHORT);
                socket.connect();
                DisplayToast("Ansluten till " + targetDevice.getName(), Toast.LENGTH_SHORT);

                currentDevice = targetDevice;
                sendBroadcast(currentDevice, BluetoothDevice.ACTION_ACL_CONNECTED);

                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();

                String tmp = "";
                String data = "";

                while (currentDevice != null) {
                    int byteCount = inputStream.available();

                    if(byteCount > 0)
                    {
                        byte[] rawBytes = new byte[byteCount];

                        inputStream.read(rawBytes);
                        tmp = new String(rawBytes,"UTF-8");

                        int startIndex = tmp.indexOf("#");
                        int endIndex = tmp.indexOf(";");

                        if (endIndex > -1) {
                            data += tmp.substring(0, endIndex);

                            // Datan skiftar väldigt mycket så vi borde samla in data under 10 sekunder
                            // innan vi uppdaterar den. Vi skickar då ett medelvärde istället för
                            // alla värdena för sig och får då ett resultat som skiftar mindre.
                            Intent incomingData = new Intent("incomingData");
                            incomingData.putExtra("data", data);
                            LocalBroadcastManager.getInstance(this).sendBroadcast(incomingData);

                            data = tmp.substring(endIndex, (tmp.length() - 1));
                        } else {
                            data += tmp;
                        }
                    }
                }

            } catch (IOException e) {
                if (!plannedDisconnect) {
                    DisplayToast("Kunde ej ansluta till termometern", Toast.LENGTH_LONG);
                } else {
                    DisplayToast("Ifrånkopplingen lyckades", Toast.LENGTH_SHORT);
                }
            }
        } catch(IOException e) {
            DisplayToast("Kunde inte skapa socket", Toast.LENGTH_LONG);
        }
    }

    private final void closeBluetoothConnection() {
        try {
            inputStream.close();
        } catch (IOException e) {
            Log.d(TAG, "Couldn't close inputStream");
        }

        try {
            outputStream.close();
        } catch (IOException e) {
            Log.d(TAG, "Couldn't close outputStream");
        }

        try {
            socket.close();
        } catch (IOException e) {
            Log.d(TAG, "Couldn't close socket");
        }

        currentDevice = null;
    }

    private final void sendBroadcast(BluetoothDevice device, String status) {
        Intent statusIntent = new Intent("connectionStatus");
        statusIntent.putExtra("device", device);
        statusIntent.putExtra("status", status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(statusIntent);
    }

    private final BroadcastReceiver bluetoothConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            switch (action) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.d(TAG, "Device connected (ACL_CONNECTED)");
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Log.d(TAG, "Device disconnected (ACL_DISCONNECTED)");
                    sendBroadcast(currentDevice, BluetoothDevice.ACTION_ACL_DISCONNECTED);
                    break;
            }
        }
    };

    private final BroadcastReceiver closeConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            plannedDisconnect = true;
            closeBluetoothConnection();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothConnectionReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeConnectionReceiver);
        closeBluetoothConnection();
    }
}
