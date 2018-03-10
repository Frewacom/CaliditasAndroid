package com.example.fredrik.caliditas;

import android.app.IntentService;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.Context;
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

    private BluetoothDevice connectedDevice = null;
    private BluetoothSocket socket = null;

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

        InputStream inputStream;
        OutputStream outputStream;

        Log.d(TAG, "Target: " + targetDevice);
        Log.d(TAG, "Connected: " + connectedDevice);

        try {
            socket = targetDevice.createRfcommSocketToServiceRecord(deviceUUID);

            try {
                DisplayToast("Ansluter till termometern", Toast.LENGTH_LONG);
                socket.connect();
                DisplayToast("Ansluten till " + targetDevice.getName(), Toast.LENGTH_LONG);

                connectedDevice = targetDevice;

                outputStream = socket.getOutputStream();
                inputStream = socket.getInputStream();

                String tmp = "";
                String data = "";

                while (true) {
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
                DisplayToast("Kunde ej ansluta till termometern", Toast.LENGTH_LONG);
            }
        } catch(IOException e) {
            DisplayToast("Kunde inte skapa socket", Toast.LENGTH_LONG);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            socket.close();
            connectedDevice = null;
        } catch (IOException e) {
            Log.d(TAG, "Couldn't close socket");
        }
    }
}
