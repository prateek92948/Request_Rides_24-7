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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
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
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class RiderMapActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener{

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;
    Location lastLocation;
    LocationRequest locationRequest;

    private Button riderSettingBtn;
    private Button logoutBtn;
    private Button reqRideBtn;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    String riderId;
    private DatabaseReference riderRequestsDBRef;
    private LatLng riderPickUpLocLatLng;
    private DatabaseReference driverAvaiRef;

    private int radius = 1;
    private Boolean isDriverFound = false;
    private String driverFoundId;
    private DatabaseReference driversRef;
    private DatabaseReference driverWorkingRef;
    Marker driverMarker, riderPickUpMarker;

    private Boolean requestRideBtnPressed = false;
    private ValueEventListener driverWorkingRefListener;

    GeoQuery geoQuery;

    private TextView txtName, txtPhone, txtVehType;
    private CircleImageView driverProfilePic;
    private RelativeLayout relativeLayoutDrInfo;
    String profileStatus = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_map);

        firebaseAuth =  FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        riderSettingBtn = findViewById(R.id.settingBtn);
        logoutBtn = findViewById(R.id.logoutBtn);
        reqRideBtn = findViewById(R.id.reqRideBtn);
        riderId = firebaseAuth.getCurrentUser().getUid();
        riderRequestsDBRef = FirebaseDatabase.getInstance("https://request-rides-5eb90-default-rtdb.firebaseio.com/").getReference().child("Riders Requests");

        driverAvaiRef = FirebaseDatabase.getInstance("https://request-rides-5eb90-default-rtdb.firebaseio.com/").getReference().child("Available Drivers");

        driverWorkingRef = FirebaseDatabase.getInstance("https://request-rides-5eb90-default-rtdb.firebaseio.com/").getReference().child("Working Drivers");

        txtName = findViewById(R.id.name_driver);
        txtPhone = findViewById(R.id.phone_driver);
        txtVehType = findViewById(R.id.vehicle_type_driver);
        driverProfilePic = findViewById(R.id.profile_image_driver);
        relativeLayoutDrInfo = findViewById(R.id.rel1);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        riderSettingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent riderSettingIntent = new Intent(RiderMapActivity.this, SettingsActivity.class);
                riderSettingIntent.putExtra("type", "Rider");
                startActivity(riderSettingIntent);
            }
        });

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firebaseAuth.signOut();
                logoutRider();
            }
        });

        reqRideBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(requestRideBtnPressed)
                {
                    requestRideBtnPressed = false;
                    geoQuery.removeAllListeners();
                    driverWorkingRef.removeEventListener(driverWorkingRefListener);

                    if(isDriverFound)
                    {
                        driversRef =  FirebaseDatabase.getInstance("https://request-rides-5eb90-default-rtdb.firebaseio.com/").getReference()
                                .child("User").child("Driver").child(driverFoundId).child("RiderID");
                        driversRef.removeValue(); //
                        driverFoundId = null;

                    }

                    isDriverFound = false;
                    radius = 1;
                    String riderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    GeoFire geoFire = new GeoFire(riderRequestsDBRef);
                    geoFire.removeLocation(riderId);

                    if(riderPickUpMarker != null)
                        riderPickUpMarker.remove();

                    if(driverMarker != null)
                        driverMarker.remove();


                 /*   try { Thread.sleep(2000);}
                    catch(Exception e)
                    {}*/

                    reqRideBtn.setText("Request a Ride");
                    relativeLayoutDrInfo.setVisibility(View.GONE);

                }
                else {
                    requestRideBtnPressed = true;
                    String riderId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    GeoFire geoFire = new GeoFire(riderRequestsDBRef);
                    geoFire.setLocation(riderId, new GeoLocation(lastLocation.getLatitude(), lastLocation.getLongitude()));
                    riderPickUpLocLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    riderPickUpMarker = mMap.addMarker(new MarkerOptions().position(riderPickUpLocLatLng).title("Your Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.rider_icon)));

                    //reqRideBtn.setText("Searching Nearby Driver..");
                    //reqRideBtn.setAlpha(.5f);
                    //reqRideBtn.setEnabled(false);

                    showClosestDriver();
                }

            }
        });
    }

    private void showClosestDriver() {
        GeoFire geoFire = new GeoFire(driverAvaiRef);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(riderPickUpLocLatLng.latitude, riderPickUpLocLatLng.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!isDriverFound && requestRideBtnPressed)
                {
                    isDriverFound = true;
                    driverFoundId = key;

                    driversRef = FirebaseDatabase.getInstance("https://request-rides-5eb90-default-rtdb.firebaseio.com/").getReference()
                            .child("User").child("Driver").child(driverFoundId);
                    driversRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                            {
                                if (dataSnapshot.hasChild("profile status"))
                                {
                                    profileStatus = dataSnapshot.child("profile status").getValue().toString();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                    HashMap driversHashMap = new HashMap();
                    driversHashMap.put("RiderID", riderId);
                    driversRef.updateChildren(driversHashMap);
                    reqRideBtn.setText("Looking for Driver Location..");

                    gettingDriverLoc();

                    //reqRideBtn.animate().alpha(.9f).setDuration(2000);
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!isDriverFound)
                {
                    radius+=1;
                    showClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    private void gettingDriverLoc() {

        driverWorkingRefListener = driverWorkingRef.child(driverFoundId).child("l")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists() && requestRideBtnPressed)
                        {
                            List<Object> driverLocMap = (List<Object>)dataSnapshot.getValue();
                            double locLat = 0;
                            double locLng = 0;
                            //reqRideBtn.setText("Driver Found");

                            if(profileStatus.equals("updated"))
                            {
                                relativeLayoutDrInfo.setVisibility(View.VISIBLE);
                                getAssignedDriverInfo();
                            }

                            if(driverLocMap.get(0) !=null)
                                locLat = Double.parseDouble(driverLocMap.get(0).toString());
                            if(driverLocMap.get(1) !=null)
                                locLng = Double.parseDouble(driverLocMap.get(1).toString());

                            LatLng driverLatLng = new LatLng(locLat,locLng);
                            if (driverMarker!=null)
                                driverMarker.remove();

                            //calculating distance b/w rider and driver
                            Location riderLoc = new Location("Rider");
                            riderLoc.setLatitude(riderPickUpLocLatLng.latitude);
                            riderLoc.setLongitude(riderPickUpLocLatLng.longitude);

                            Location driverLoc = new Location("Driver");
                            driverLoc.setLatitude(driverLatLng.latitude);
                            driverLoc.setLongitude(driverLatLng.longitude);

                            //Log.i("check","La1 "+ riderLoc.getLatitude()+" Lng1 "+riderLoc.getLongitude());
                            //Log.i("check", "La2 "+ driverLoc.getLatitude()+" Lng2 "+driverLoc.getLongitude());

                            int distanceBetweenRiderAndDriver = (int)riderLoc.distanceTo(driverLoc);


                            if (distanceBetweenRiderAndDriver <= 100)
                               reqRideBtn.setText("Your Driver has Arrived. Have a Safe journey!");
                            else
                                reqRideBtn.setText("Driver is "+ distanceBetweenRiderAndDriver+" m away");

                            driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Driver's Location").icon(BitmapDescriptorFactory.fromResource(R.drawable.cab)));
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
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }
        buildGoogleApiClient();
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
        lastLocation = location;
        LatLng latLng= new LatLng(location.getLatitude(), location.getLongitude());
        //mMap.addMarker(new MarkerOptions().position(latLng).title("Your Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(12));

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
    }

    private void logoutRider() {
        Intent riderLogOutIntent = new Intent(RiderMapActivity.this, MainActivity.class);
        riderLogOutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(riderLogOutIntent);
        finish();
    }

    private void getAssignedDriverInfo()
    {
        DatabaseReference reference = FirebaseDatabase.getInstance("https://request-rides-5eb90-default-rtdb.firebaseio.com/").getReference()
                .child("User").child("Driver").child(driverFoundId);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.exists()  &&  dataSnapshot.getChildrenCount() > 0)
                {
                    String name = dataSnapshot.child("name").getValue().toString();
                    String phoneNum = dataSnapshot.child("phone number").getValue().toString();
                    String vehicleType = dataSnapshot.child("vehicle type").getValue().toString();


                    txtName.setText(name);
                    txtPhone.setText(phoneNum);
                    txtVehType.setText(vehicleType);

                    if (dataSnapshot.hasChild("profile picture"))
                    {
                        String profilePic = dataSnapshot.child("profile picture").getValue().toString();
                        Picasso.get().load(profilePic).into(driverProfilePic);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
