package com.example.requestrides;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button driverWelcomeBtn;
    private Button riderWelcomeBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        driverWelcomeBtn = findViewById(R.id.driver_btn);
        riderWelcomeBtn = findViewById(R.id.rider_btn);

        driverWelcomeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginSignupDriverBtn = new Intent(MainActivity.this, DriverLoginSignupActivity.class);
                startActivity(loginSignupDriverBtn);
            }
        });

        riderWelcomeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginSignupRiderBtn = new Intent(MainActivity.this, RiderLoginSignupActivity.class);
                startActivity(loginSignupRiderBtn);
            }
        });
    }
}
