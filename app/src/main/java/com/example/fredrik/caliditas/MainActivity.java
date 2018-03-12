package com.example.fredrik.caliditas;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import static java.lang.Math.min;
import static java.lang.Math.round;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Toolbar toolbar;
    private TextView temperature;
    private TextView humidity;
    private TextView battery;
    private Button applyLimit;
    private BluetoothDevice currentDevice;
    private BluetoothAdapter bluetoothAdapter;
    private boolean hasSentNotification = false;

    private int maxTemp = 0;
    private int minTemp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getIntent().getIntExtra("notification", 0) != 0) {
            hasSentNotification = false;
        }

        temperature = (TextView) findViewById(R.id.temperature);
        humidity = (TextView) findViewById(R.id.humidity);
        battery = (TextView) findViewById(R.id.battery);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        applyLimit = (Button) findViewById(R.id.applyLimit);
        toolbar.setTitle("Ej ansluten");
        setSupportActionBar(toolbar);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(
                        socketReceiver,
                        new IntentFilter("incomingData")
                );

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(
                        closeConnectionReceiver,
                        new IntentFilter("closeConnection")
                );

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(
                        limitReceiver,
                        new IntentFilter("setLimit")
                );

        applyLimit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent limitIntent = new Intent(MainActivity.this, LimitActivity.class);
                startActivity(limitIntent);
            }
        });
    }

    private void createNotification(String title, String message) {
        // Create an Intent for the activity you want to start
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra("notification", 1);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        // Get the PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationManager notification = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notify = new Notification.Builder
                (getApplicationContext())
                .setContentTitle("Warning")
                .setContentText(message)
                .setContentTitle(title)
                .setContentIntent(resultPendingIntent)
                .setSound(alarmSound)
                .setVibrate(new long[] { 1000, 1000})
                .setSmallIcon(R.drawable.ic_warning_black_24dp)
                .build();

        notify.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.notify(0, notify);
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

                if (maxTemp != 0) {
                    if (temp > maxTemp) {
                        Log.d(TAG, "Temperature above max");

                        if (!hasSentNotification) {
                            createNotification(
                                    "Temperaturen är för hög!",
                                    "Temperaturen är nu: " + Integer.toString(temp) + "°C"
                            );

                            hasSentNotification = true;
                        }
                    }
                }

                if (minTemp != 0) {
                    if (temp < minTemp) {
                        Log.d(TAG, "Temperature lower than min");

                        if (!hasSentNotification) {
                            createNotification(
                                    "Temperaturen är för låg!",
                                    "Temperaturen är nu: " + Integer.toString(temp) + "°C"
                            );

                            hasSentNotification = true;
                        }
                    }
                }

                temperature.setText(temp + "°C");
                humidity.setText("Luftfuktighet: " + hum + "%");
                battery.setText("Batteri: 100%");
            }
        }
    };

    private final BroadcastReceiver closeConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean wasPlanned = intent.getBooleanExtra("wasPlanned", false);

            Log.d(TAG, "wasPlanned: " + wasPlanned);

            if (!wasPlanned) {
                createNotification("Termometern tappade anslutningen", "Du är inte längre ansluten till termometern");
            }

            currentDevice = null;
            toolbar.setTitle("Ej ansluten");
            temperature.setText("°C");
            humidity.setText("Luftfuktighet: -");
            battery.setText("Batteri: -");
        }
    };

    private final BroadcastReceiver limitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra("type");
            int value = intent.getIntExtra("value", 0);

            switch (type) {
                case "min":
                    minTemp = value;
                    break;

                case "max":
                    maxTemp = value;
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
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

        LocalBroadcastManager.getInstance(this).unregisterReceiver(socketReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(closeConnectionReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(limitReceiver);
    }
}
