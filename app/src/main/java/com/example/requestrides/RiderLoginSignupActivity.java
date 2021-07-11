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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RiderLoginSignupActivity extends AppCompatActivity {

    private Button riderLoginBtn;
    private Button riderRegBtn;
    private TextView riderLoginTV;
    private TextView riderRegLinkTV;
    private EditText riderEmail;
    private EditText riderPassword;

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;

    private DatabaseReference riderDBRef;
    private String currentRiderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_login_signup);

        setTitle("Login to Rider Account");

        riderLoginBtn = findViewById(R.id.riderLoginBtn);
        riderRegBtn = findViewById(R.id.riderRegBtn);
        riderLoginTV = findViewById(R.id.riderloginTV);
        riderRegLinkTV = findViewById(R.id.riderRegLinkTV);

        riderEmail = findViewById(R.id.riderEmail);
        riderPassword = findViewById(R.id.riderPW);

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(this);

        riderRegBtn.setVisibility(View.INVISIBLE);
        riderRegBtn.setEnabled(false);

        riderRegLinkTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                riderLoginBtn.setVisibility(View.INVISIBLE);
                riderRegLinkTV.setVisibility(View.INVISIBLE);
                riderLoginTV.setText("Register a Rider");
                riderRegBtn.setVisibility(View.VISIBLE);
                riderRegBtn.setEnabled(true);
                riderRegBtn.animate().translationYBy(-150f).setDuration(500);
            }
        });

        riderRegBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String rider_email = riderEmail.getText().toString();
                String rider_psw = riderPassword.getText().toString();

                registerRider(rider_email,rider_psw);
            }
        });

        riderLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String rider_email = riderEmail.getText().toString();
                String rider_psw = riderPassword.getText().toString();

                logInRider(rider_email,rider_psw);
            }
        });
    }

    private void logInRider(String rider_email, String rider_psw) {
        if (TextUtils.isEmpty(rider_email))
            Toast.makeText(this, "Please Enter Your Email", Toast.LENGTH_SHORT).show();
        else if (TextUtils.isEmpty(rider_psw))
            Toast.makeText(this, "Please Enter Your Password", Toast.LENGTH_SHORT).show();
        else {
            progressDialog.setTitle("Rider Login");
            progressDialog.setMessage("Please wait while we are validating your credentials..");
            progressDialog.show();

            firebaseAuth.signInWithEmailAndPassword(rider_email, rider_psw)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                progressDialog.dismiss();
                                Intent riderMapIntent = new Intent(RiderLoginSignupActivity.this, RiderMapActivity.class);
                                startActivity(riderMapIntent);
                                Toast.makeText(RiderLoginSignupActivity.this, "Rider Logged in Successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                progressDialog.dismiss();
                                Toast.makeText(RiderLoginSignupActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }
    }

    private void registerRider(String rider_email, String rider_psw) {
        if(TextUtils.isEmpty(rider_email))
            Toast.makeText(this, "Please Enter Your Email", Toast.LENGTH_SHORT).show();
        else if (TextUtils.isEmpty(rider_psw))
            Toast.makeText(this, "Please Enter Your Password", Toast.LENGTH_SHORT).show();
        else
        {
            progressDialog.setTitle("Rider Registration");
            progressDialog.setMessage("Please wait, Registration is in process..");
            progressDialog.show();

            firebaseAuth.createUserWithEmailAndPassword(rider_email,rider_psw)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()) {
                                currentRiderId = firebaseAuth.getCurrentUser().getUid();
                                riderDBRef = FirebaseDatabase.getInstance("https://request-rides-5eb90-default-rtdb.firebaseio.com/").getReference()
                                        .child("User").child("Rider").child(currentRiderId);
                                riderDBRef.setValue(true);
                                progressDialog.dismiss();
                                Intent riderMapIntent = new Intent(RiderLoginSignupActivity.this, RiderMapActivity.class);
                                startActivity(riderMapIntent);
                                Toast.makeText(RiderLoginSignupActivity.this, "Rider Registered Successfully", Toast.LENGTH_SHORT).show();

                            }
                            else {
                                progressDialog.dismiss();
                                Toast.makeText(RiderLoginSignupActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }

    }
}
