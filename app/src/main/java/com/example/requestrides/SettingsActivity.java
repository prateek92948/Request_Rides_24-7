package com.example.requestrides;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private CircleImageView profileImageView;
    private EditText nameEditText, phoneEditText, vehicleTypeEditText;
    private ImageView closeButton, saveButton;
    private TextView profileChangeBtn;

    private String getType;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;

    private String checker = "";
    private Uri imageUri;
    private String myUrl = "";
    private StorageTask uploadTask;
    private StorageReference storageProfilePicsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        getType = getIntent().getStringExtra("type");

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance("https://request-rides-5eb90-default-rtdb.firebaseio.com/").getReference().child("User").child(getType);
        storageProfilePicsRef = FirebaseStorage.getInstance().getReference().child("Profile Pictures");

        profileImageView = findViewById(R.id.profile_image);
        nameEditText = findViewById(R.id.name);
        phoneEditText = findViewById(R.id.phone_number);

        vehicleTypeEditText = findViewById(R.id.vehicleType);

        closeButton = findViewById(R.id.close_button);
        saveButton = findViewById(R.id.save_button);

        profileChangeBtn = findViewById(R.id.change_picture_btn);

        if (getType.equals("Driver"))
        {
            vehicleTypeEditText.setVisibility(View.VISIBLE);
        }

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (getType.equals("Driver"))
                    startActivity(new Intent(SettingsActivity.this, DriverMapActivity.class));
                else
                    startActivity(new Intent(SettingsActivity.this, RiderMapActivity.class));
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (checker.equals("clicked"))
                {
                    validateControllers();
                }
                else
                {
                    validateAndSaveOnlyInformation();
                }
            }
        });

        profileChangeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                checker = "clicked";

                CropImage.activity()
                        .setAspectRatio(1, 1)
                        .start(SettingsActivity.this);
            }
        });

        getUserInformation();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE  &&  resultCode==RESULT_OK  &&  data!=null)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            imageUri = result.getUri();

            profileImageView.setImageURI(imageUri);
        }
        else
        {
            Toast.makeText(this, "Image not selected", Toast.LENGTH_SHORT).show();
        }
    }



    private void validateControllers()
    {
        if (TextUtils.isEmpty(nameEditText.getText().toString()))
        {
            Toast.makeText(this, "Please Enter Your Name", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(phoneEditText.getText().toString()))
        {
            Toast.makeText(this, "Please Enter Your Phone Number", Toast.LENGTH_SHORT).show();
        }
        else if (phoneEditText.getText().toString().length() < 10)
        {
            Toast.makeText(this, "Please Enter a Valid Phone Number", Toast.LENGTH_SHORT).show();
        }
        else if (getType.equals("Driver")  &&  TextUtils.isEmpty(vehicleTypeEditText.getText().toString()))
        {
            Toast.makeText(this, "Please Enter Your Vehicle Type", Toast.LENGTH_SHORT).show();
        }
        else if (checker.equals("clicked"))
        {
            uploadProfilePictureAndOtherInfo();
        }
    }


    private void uploadProfilePictureAndOtherInfo()
    {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Updating Account");
        progressDialog.setMessage("Please wait while we are updating your account information..");
        progressDialog.show();


        if (imageUri != null)
        {
            final StorageReference fileRef = storageProfilePicsRef
                    .child(firebaseAuth.getCurrentUser().getUid()  +  ".jpg");

            uploadTask = fileRef.putFile(imageUri);

            uploadTask.continueWithTask(new Continuation() {
                @Override
                public Object then(@NonNull Task task) throws Exception
                {
                    if (!task.isSuccessful())
                    {
                        throw task.getException();
                    }

                    return fileRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task)
                {
                    if (task.isSuccessful())
                    {
                        Uri downloadUrl = task.getResult();
                        myUrl = downloadUrl.toString();


                        HashMap<String, Object> userMap = new HashMap<>();
                        userMap.put("uid", firebaseAuth.getCurrentUser().getUid());
                        userMap.put("name", nameEditText.getText().toString());
                        userMap.put("phone number", phoneEditText.getText().toString());
                        userMap.put("profile picture", myUrl);
                        userMap.put("profile status","updated");

                        if (getType.equals("Driver"))
                            userMap.put("vehicle type", vehicleTypeEditText.getText().toString());

                        databaseReference.child(firebaseAuth.getCurrentUser().getUid()).updateChildren(userMap);

                        progressDialog.dismiss();

                        Toast.makeText(SettingsActivity.this, "Your account information has been updated successfully", Toast.LENGTH_LONG).show();

                        if (getType.equals("Driver"))
                        {
                            Intent intent = new Intent(SettingsActivity.this, DriverMapActivity.class);
                            startActivity(intent);
                        }
                        else
                        {
                            Intent intent = new Intent(SettingsActivity.this, RiderMapActivity.class);
                            startActivity(intent);
                        }
                    }
                }
            });
        }
        else
        {
            Toast.makeText(this, "Please select your profile picture", Toast.LENGTH_SHORT).show();
        }
    }




    private void validateAndSaveOnlyInformation()
    {
        if (TextUtils.isEmpty(nameEditText.getText().toString()))
        {
            Toast.makeText(this, "Please Enter Your Name", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(phoneEditText.getText().toString()))
        {
            Toast.makeText(this, "Please Enter Your Phone Number", Toast.LENGTH_SHORT).show();
        }
        else if (phoneEditText.getText().toString().length() < 10)
        {
            Toast.makeText(this, "Please Enter a Valid Phone Number", Toast.LENGTH_SHORT).show();
        }
        else if (getType.equals("Driver")  &&  TextUtils.isEmpty(vehicleTypeEditText.getText().toString()))
        {
            Toast.makeText(this, "Please Enter Your Vehicle Type", Toast.LENGTH_SHORT).show();
        }
        else
        {
            HashMap<String, Object> userMap = new HashMap<>();
            userMap.put("uid", firebaseAuth.getCurrentUser().getUid());
            userMap.put("name", nameEditText.getText().toString());
            userMap.put("phone number", phoneEditText.getText().toString());
            userMap.put("profile status","updated");

            if (getType.equals("Driver"))
                userMap.put("vehicle type", vehicleTypeEditText.getText().toString());

            databaseReference.child(firebaseAuth.getCurrentUser().getUid()).updateChildren(userMap);

            Toast.makeText(SettingsActivity.this, "Your account information has been updated successfully", Toast.LENGTH_LONG).show();

            if (getType.equals("Driver"))
            {
                Intent intent = new Intent(SettingsActivity.this, DriverMapActivity.class);
                startActivity(intent);
            }
            else
            {
                Intent intent = new Intent(SettingsActivity.this, RiderMapActivity.class);
                startActivity(intent);
            }
        }
    }


    private void getUserInformation()
    {
        databaseReference.child(firebaseAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phoneNum= dataSnapshot.child("phone number").getValue().toString();

                    nameEditText.setText(name);
                    phoneEditText.setText(phoneNum);

                    if (getType.equals("Driver"))
                    {
                        String vehicleType = dataSnapshot.child("vehicle type").getValue().toString();
                        vehicleTypeEditText.setText(vehicleType);
                    }


                    if (dataSnapshot.hasChild("profile picture"))
                    {
                        String image = dataSnapshot.child("profile picture").getValue().toString();
                        Picasso.get().load(image).into(profileImageView);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
