package com.example.fredrik.caliditas;

import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.concurrent.Callable;

public class LimitActivity extends AppCompatActivity implements View.OnClickListener{
    private final static String TAG = "LimitActivity";

    private Toolbar toolbar;
    private Button lowestButton;
    private Button highestButton;
    private EditText lowestTemp;
    private EditText highestTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_limit);

        lowestButton = (Button) findViewById(R.id.lowestTempButton);
        highestButton = (Button) findViewById(R.id.highestTempButton);
        lowestTemp = (EditText) findViewById(R.id.lowestTemperature);
        highestTemp = (EditText) findViewById(R.id.highestTemperature);

        lowestButton.setOnClickListener(this);
        highestButton.setOnClickListener(this);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Sätt gränser");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


    }

    private int getInputValue(EditText target) {
        if (!target.getText().toString().matches("")) {
            int value = Integer.parseInt(target.getText().toString());
            Log.d(TAG, "Received value: " + value);

            return value;
        }

        return 0;
    }

    private void setLimits(EditText target, String type) {
        int value = getInputValue(target);

        Intent limitIntent = new Intent("setLimit");
        limitIntent.putExtra("type", type);
        limitIntent.putExtra("value", value);
        LocalBroadcastManager.getInstance(this).sendBroadcast(limitIntent);
    }

    private void restoreButtonText(Button button) {
        button.setText("Spara");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.lowestTempButton:
                setLimits(lowestTemp, "min");
                lowestButton.setText("Sparades");
                Handler handlerLowest = new Handler();
                handlerLowest.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        lowestButton.setText("Spara");
                    }
                }, 2000);
                break;

            case R.id.highestTempButton:
                setLimits(highestTemp, "max");
                highestButton.setText("Sparades");
                Handler handlerHighest = new Handler();
                handlerHighest.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        highestButton.setText("Spara");
                    }
                }, 2000);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_limit, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
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
}
