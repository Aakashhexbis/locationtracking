package com.hex.currentlocationapplication;

import android.animation.ValueAnimator;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class Testing extends FragmentActivity implements OnMapReadyCallback {
    LatLng startPosition,endPosition;
    private GoogleMap mMap;
    private Marker carMarker;
    private List<LatLng> path = new ArrayList<>();
    private ValueAnimator valueAnimator;
    ImageView play;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_testing);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        play = findViewById(R.id.btnPlay);
        play.setOnClickListener(v -> {
            if (valueAnimator != null && valueAnimator.isRunning()) {
                valueAnimator.cancel();

            }else{
                animateMarker();
            }});

        // Define your path here
//        path.add(new LatLng(37.7749, -122.4194)); // Example coordinates
//        path.add(new LatLng(37.7750, -122.4185));
//        path.add(new LatLng(37.7751, -122.4175));
        path.add(new LatLng (  28.4275364,77.2937147));
        path.add(new LatLng ( 28.4276797,77.2937864));
        path.add(new LatLng(  28.4277136,77.2938543));
        path.add(new LatLng (28.4276844,77.2937429));
        path.add(new LatLng (28.4276501,77.2937511));
        path.add(new LatLng (28.4276789,77.2924496));
        path.add(new LatLng (28.4261061,77.2926443));
        path.add(new LatLng (28.4196825,77.2938714));
        path.add(new LatLng (28.4125773,77.2942241));
        path.add(new LatLng (28.4090538,77.2943879));
        path.add(new LatLng(28.4060293,77.2979994));
        path.add(new LatLng(28.404592,77.299821));
        path.add(new LatLng(28.4005162,77.2986321));
        path.add(new LatLng(28.3973937,77.3015274));
        path.add(new LatLng(28.3973884,77.301499));
        path.add(new LatLng(28.3974129,77.3015593));
        path.add(new LatLng(28.3973845,77.3014919));
        path.add(new LatLng(28.3973982,77.3015277));
        path.add(new LatLng(28.4120339,77.2947141));
        path.add(new LatLng(28.4136612,77.2942662));
        path.add(new LatLng(28.4158121,77.2940535));
        path.add(new LatLng( 28.4194068,77.2937769));
        path.add(new LatLng(28.4295525,77.288064));
        path.add(new LatLng(28.4277789,77.2894));
        path.add(new LatLng(28.4275383,77.2894796));
        path.add(new LatLng(28.4274921,77.2937373));
        path.add(new LatLng(28.427512,77.2938058));
        path.add(new LatLng(28.4275889,77.2937326));
        path.add(new LatLng(28.4276059,77.2937278));
        path.add(new LatLng(28.427573,77.2937241));
        path.add(new LatLng(28.4275988,77.2937314));
        
        
        // Add more path as needed
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker at the start position
        if (!path.isEmpty()) {
            LatLng startPosition = path.get(0);
            carMarker = mMap.addMarker(new MarkerOptions().position(startPosition).icon(BitmapDescriptorFactory.fromResource(R.drawable.carm2)).flat(true).anchor(0.5f, 0.5f));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startPosition, 15));
        }

        // Start animating the marker
        animateMarker();
    }

    private void animateMarker() {
        if (path.size() < 2 || carMarker == null) return;


        valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(7000); // Duration of the animation
        valueAnimator.setInterpolator(new LinearInterpolator());

        valueAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            int index = (int) (fraction * (path.size() - 1));
            if(index<path.size()-2){
                startPosition   = path.get(index);
                endPosition = path.get(index + 1);
            }


            LatLng newPosition = interpolate(fraction, startPosition, endPosition);
            carMarker.setPosition(newPosition);

            float bearing = (float) bearingBetweenLocations(startPosition, endPosition);
            carMarker.setRotation(bearing);
        });

        valueAnimator.start();
    }

    private LatLng interpolate(float fraction, LatLng a, LatLng b) {
        double lat = (b.latitude - a.latitude) * fraction + a.latitude;
        double lng = (b.longitude - a.longitude) * fraction + a.longitude;
        return new LatLng(lat, lng);
    }

    private double bearingBetweenLocations(LatLng latLng1, LatLng latLng2) {
        double PI = 3.14159;
        double lat1 = latLng1.latitude * PI / 180;
        double long1 = latLng1.longitude * PI / 180;
        double lat2 = latLng2.latitude * PI / 180;
        double long2 = latLng2.longitude * PI / 180;

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;

        return brng;
    }
}

