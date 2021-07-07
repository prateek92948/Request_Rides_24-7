package com.example.requestrides;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class DriverMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;

    private Button settingBtn;
    private Button logoutBtn;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private Boolean isDriverLoggedOut = false;
    Marker riderPickUpMarker;
    private DatabaseReference assignedRiderRef, assignedRiderPickUpRef;
    private ValueEventListener assignedRiderPickUpRefListener;
    String driverID, riderID="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_map);

        firebaseAuth =  FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        settingBtn = findViewById(R.id.settingBtn);
        logoutBtn = findViewById(R.id.logoutBtn);
        driverID = firebaseAuth.getCurrentUser().getUid();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isDriverLoggedOut = true;
                disconnectDriver();
                firebaseAuth.signOut();
                logoutDriver();
            }
        });

        getAssignedRiderReq();
    }

    private void getAssignedRiderReq() {
        assignedRiderRef = FirebaseDatabase.getInstance("https://request-rides-5eb90-default-rtdb.firebaseio.com/").getReference()
                .child("User").child("Driver").child(driverID).child("RiderID");

        assignedRiderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    riderID = dataSnapshot.getValue().toString();
                    getAssignedRiderPickUpLoc();
                }
                else {
                    riderID = "";
                    if(riderPickUpMarker != null)
                        riderPickUpMarker.remove();

                    if(assignedRiderPickUpRefListener != null)
                        assignedRiderPickUpRef.removeEventListener(assignedRiderPickUpRefListener);

                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedRiderPickUpLoc() {
        assignedRiderPickUpRef = FirebaseDatabase.getInstance("https://request-rides-5eb90-default-rtdb.firebaseio.com/").getReference()
                                    .child("Riders Requests").child(riderID).child("l");

        assignedRiderPickUpRefListener = assignedRiderPickUpRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    List<Object> riderLocMap = (List<Object>)dataSnapshot.getValue();
                    double locLat = 0;
                    double locLng = 0;

                    if(riderLocMap.get(0) !=null)
                        locLat = Double.parseDouble(riderLocMap.get(0).toString());
                    if(riderLocMap.get(1) !=null)
                        locLng = Double.parseDouble(riderLocMap.get(1).toString());

                    LatLng riderLatLng = new LatLng(locLat,locLng);
                    riderPickUpMarker = mMap.addMarker(new MarkerOptions().position(riderLatLng).title("Rider Pickup Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.rider_icon)));
                    //Log.i("marker status", "marker added at "+ riderLatLng.latitude+" "+ riderLatLng.longitude);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        buildGoogleApiClient();
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }
        mMap.setMyLocationEnabled(true);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(locationRequest.PRIORITY_HIGH_ACCURACY);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (getApplicationContext() != null)
        {
            lastLocation = location;
            LatLng latLng= new LatLng(location.getLatitude(), location.getLongitude());
            //mMap.addMarker(new MarkerOptions().position(latLng).title("Your Location"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(13));

            if(!isDriverLoggedOut)
            {
                String onlineDriverID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference driverAvailabilityRef = FirebaseDatabase.getInstance("https://request-rides-5eb90-default-rtdb.firebaseio.com/").getReference().child("Available Drivers");
                GeoFire geoFireAvai = new GeoFire(driverAvailabilityRef);

                DatabaseReference driverWorkingRef = FirebaseDatabase.getInstance("https://request-rides-5eb90-default-rtdb.firebaseio.com/").getReference().child("Working Drivers");
                GeoFire geoFireWorking = new GeoFire(driverWorkingRef);

               switch (riderID)
               {
                   case "":
                       geoFireWorking.removeLocation(onlineDriverID);
                       geoFireAvai.setLocation(onlineDriverID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                       break;
                   default:
                       geoFireAvai.removeLocation(onlineDriverID);
                       geoFireWorking.setLocation(onlineDriverID, new GeoLocation(location.getLatitude(), location.getLongitude()));
                       break;
               }
            }
        }
    }

    protected  synchronized void buildGoogleApiClient()
    {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isDriverLoggedOut)
        {
            disconnectDriver();
        }

    }

    private void disconnectDriver() {
        String driverID = firebaseAuth.getCurrentUser().getUid();
        DatabaseReference driverAvailabilityRef = FirebaseDatabase.getInstance("https://request-rides-5eb90-default-rtdb.firebaseio.com/").getReference().child("Available Drivers");

        GeoFire geoFire = new GeoFire(driverAvailabilityRef);
        geoFire.removeLocation(driverID);
    }

    private void logoutDriver() {
        Intent driverLogOutIntent = new Intent(DriverMapActivity.this, MainActivity.class);
        driverLogOutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(driverLogOutIntent);
        finish();
    }

}
