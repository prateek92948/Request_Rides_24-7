package com.example.requestrides;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginSignupActivity extends AppCompatActivity {

    private Button driverLoginBtn;
    private Button driverRegBtn;
    private TextView driverLoginTV;
    private TextView driverRegLinkTV;
    private EditText driverEmail;
    private EditText driverPassword;

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    private DatabaseReference driverDBRef;
    private String currentDriverId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login_signup);

        setTitle("Login to Driver Account");

        driverLoginBtn = findViewById(R.id.driverLoginBtn);
        driverRegBtn = findViewById(R.id.driverRegBtn);
        driverLoginTV = findViewById(R.id.driverloginTV);
        driverRegLinkTV = findViewById(R.id.driverRegLinkTV);
        driverEmail = findViewById(R.id.driverEmail);
        driverPassword = findViewById(R.id.driverPW);

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);

        driverRegBtn.setVisibility(View.INVISIBLE);
        driverRegBtn.setEnabled(false);

        driverRegLinkTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                driverLoginBtn.setVisibility(View.INVISIBLE);
                driverRegLinkTV.setVisibility(View.INVISIBLE);
                driverLoginTV.setText("Register a Driver");
                driverRegBtn.setVisibility(View.VISIBLE);
                driverRegBtn.setEnabled(true);
                driverRegBtn.animate().translationYBy(-170f).setDuration(500);
            }
        });

        driverRegBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String driver_email = driverEmail.getText().toString();
                String driver_psw = driverPassword.getText().toString();

                registerDriver(driver_email,driver_psw);
            }
        });

        driverLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String driver_email = driverEmail.getText().toString();
                String driver_psw = driverPassword.getText().toString();

                logInDriver(driver_email,driver_psw);
            }
        });
    }

    private void logInDriver(String driver_email, String driver_psw) {
        if(TextUtils.isEmpty(driver_email))
            Toast.makeText(this, "Please Enter Your Email", Toast.LENGTH_SHORT).show();
        else if (TextUtils.isEmpty(driver_psw))
            Toast.makeText(this, "Please Enter Your Password", Toast.LENGTH_SHORT).show();
        else
        {
            progressDialog.setTitle("Driver Login");
            progressDialog.setMessage("Please wait while we are validating your credentials..");
            progressDialog.show();

            firebaseAuth.signInWithEmailAndPassword(driver_email,driver_psw)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()) {
                                progressDialog.dismiss();
                                Intent driverMapIntent = new Intent(DriverLoginSignupActivity.this, DriverMapActivity.class);
                                startActivity(driverMapIntent);
                                Toast.makeText(DriverLoginSignupActivity.this, "Driver Logged In Successfully", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                progressDialog.dismiss();
                                Toast.makeText(DriverLoginSignupActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }


    }


    private void registerDriver(String driver_email, String driver_psw) {
        if(TextUtils.isEmpty(driver_email))
            Toast.makeText(this, "Please Enter Your Email", Toast.LENGTH_SHORT).show();
        else if (TextUtils.isEmpty(driver_psw))
            Toast.makeText(this, "Please Enter Your Password", Toast.LENGTH_SHORT).show();
        else
        {
            progressDialog.setTitle("Driver Registration");
            progressDialog.setMessage("Please wait, Registration is in process..");
            progressDialog.show();

            firebaseAuth.createUserWithEmailAndPassword(driver_email,driver_psw)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                           if(task.isSuccessful()) {
                               currentDriverId = firebaseAuth.getCurrentUser().getUid();
                               driverDBRef = FirebaseDatabase.getInstance("https://request-rides-5eb90-default-rtdb.firebaseio.com/").getReference()
                                       .child("User").child("Driver").child(currentDriverId);
                               driverDBRef.setValue(true);
                               progressDialog.dismiss();
                               Intent driverMapIntent = new Intent(DriverLoginSignupActivity.this, DriverMapActivity.class);
                               startActivity(driverMapIntent);
                               Toast.makeText(DriverLoginSignupActivity.this, "Driver Registered Successfully", Toast.LENGTH_SHORT).show();
                           }
                           else {
                               progressDialog.dismiss();
                               Toast.makeText(DriverLoginSignupActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                           }
                        }
                    });
        }
    }
}
