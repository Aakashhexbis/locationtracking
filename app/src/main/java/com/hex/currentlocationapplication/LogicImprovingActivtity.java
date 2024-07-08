package com.hex.currentlocationapplication;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.hex.hextools.Api.ColumnWiseResultHashMap;
import com.hex.hextools.Api.HexApiService;
import com.hex.hextools.Api.ResultColumn;
import com.hex.hextools.Api.SQLQueryResult;
import com.hex.hextools.Api.SqlString;
import com.hex.hextools.DateUtils.DateUtils;
import com.hex.hextools.Information.Information;
import com.hex.hextools.Widgets.HexProgressDialog;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

public class LogicImprovingActivtity extends AppCompatActivity implements HexApiService.Listener, OnMapReadyCallback {
    HexProgressDialog hexProgressDialog;
    HexApiService hexApiService;
    ColumnWiseResultHashMap locationData;
    GoogleMap gMap;
    CardView cardView;
    TextView speedTxt;
    List<LatLng> temPoints = new ArrayList<>();
    DateFormat sdf = new SimpleDateFormat("HH:mm:aa");
    int runningIndex = 1;
    boolean play = false;

    SeekBar seekBar;
    TextView timeTxt;

    TextView currentLocationTxt;
    boolean isRunning = false;

    ColumnWiseResultHashMap dummyLocationData;
    private static final double EARTH_RADIUS_KM = 6371.0;
    ImageView btnPlay;
    ImageView backBtn;
    ImageView battery;
    TextView batteryTxt;
    private Marker animatedMarker;

    private Polyline polyline;
    int LOCATION_PERMISSION_REQUEST_CODE = Information.getUniqueID();
    int GET_LOCATION_DATA = Information.getUniqueID();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        hexProgressDialog = new HexProgressDialog(this);
        // seekBar = findViewById(R.id.proogressBar);
        Information.setDB(this, "AakashTraining");
        Information.setApiPath(this, "https:/trackhr.co.in/AakashTraining/api/");
        hexApiService = new HexApiService((Context) this, this);
        seekBar = findViewById(R.id.proogressBar);
        timeTxt = findViewById(R.id.time);
        backBtn = findViewById(R.id.backBtn);
        cardView = findViewById(R.id.speedCard);
        speedTxt = findViewById(R.id.runningSpeed);
        currentLocationTxt = findViewById(R.id.currentLocation);

        btnPlay = findViewById(R.id.btnPlay);
        battery = findViewById(R.id.battery);
        batteryTxt = findViewById(R.id.batteryText);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRunning) {
                    btnPlay.setImageResource(R.drawable.play);
                    animateMarker(temPoints, runningIndex);
                    isRunning = false;
                    play = false;
                    ZoomControl(true);
                } else {
                    btnPlay.setImageResource(R.drawable.pause2);
                    animateMarker(temPoints, runningIndex);
                    isRunning = true;
                    play = true;
                    ZoomControl(false);
                }
            }
        });


        backBtn.setOnClickListener(onClickListener -> {
            new AlertDialog.Builder(this).setTitle("Are you sure you want to exit?").setPositiveButton("Yes", (dialog, which) -> {
                finish();
            }).setNegativeButton("No", (dialog, which) -> {

            }).setCancelable(false).show();


        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (isRunning != true && progress >= 1) {
                    batteryTxt.setText(dummyLocationData.getColumnValue("BatteryPercentage", progress) + "%");
                    if (Integer.parseInt(dummyLocationData.getColumnValue("BatteryPercentage", progress)) <= 70 && Integer.parseInt(dummyLocationData.getColumnValue("BatteryPercentage", progress)) > 60) {
                        battery.setImageResource(R.drawable.halfbattery);
                    } else if (Integer.parseInt(dummyLocationData.getColumnValue("BatteryPercentage", progress)) <= 30 && Integer.parseInt(dummyLocationData.getColumnValue("BatteryPercentage", progress)) > 0) {
                        battery.setImageResource(R.drawable.lowbattery);
                    } else if (Integer.parseInt(dummyLocationData.getColumnValue("BatteryPercentage", progress)) > 70) {
                        battery.setImageResource(R.drawable.fullbattery);
                    }

                    animatedMarker.setPosition(temPoints.get(progress));
                    float bearing = (float) bearingBetweenLocations(temPoints.get(progress - 1), temPoints.get(progress));
                    if (!Float.isNaN(bearing)) {
                        animatedMarker.setRotation(bearing);
                    }
                    timeTxt.setText(sdf.format(new Date(Long.parseLong((dummyLocationData.getColumnValue("PhoneLogTime", progress))))));
                }

                if (progress == temPoints.size() - 2) {
                    isRunning = false;
                    play = false;
                    btnPlay.setImageResource(R.drawable.play);
                    runningIndex = 1;
                    ZoomControl(true);
                }


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isRunning = false;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();

                if (progress < temPoints.size() - 1) {
                    if (progress >= 1) {
                        if (play) {
                            animateMarker(temPoints, progress);
                            isRunning = true;
                        } else {
                            runningIndex = progress;
                        }
                    } else {
                        if (play) {
                            animateMarker(temPoints, 1);
                            isRunning = true;
                        } else {
                            runningIndex = 1;
                        }
                    }

                }

            }
        });


        // GetLocationData();

    }


    public void ZoomControl(boolean value) {
        gMap.getUiSettings().setZoomControlsEnabled(value);
        gMap.getUiSettings().setZoomGesturesEnabled(value);
        gMap.getUiSettings().setScrollGesturesEnabled(value);
        gMap.getUiSettings().setCompassEnabled(value);
    }

    void GetLocationData() {
        hexProgressDialog.setMessage("Loading...");
        hexProgressDialog.show();
        String query = SqlString.format("GP_MapActivity_GetRawLocations");
        hexApiService.execSql(query, GET_LOCATION_DATA, "", "");
    }


    private LatLng interpolate(float fraction, LatLng a, LatLng b) {
        double lat = (b.latitude - a.latitude) * fraction + a.latitude;
        double lng = (b.longitude - a.longitude) * fraction + a.longitude;
        return new LatLng(lat, lng);
    }

    public LatLng getLatLngFromCoord(String coord) {
        String[] parts = coord.split(",");
        double lat1 = Double.parseDouble(parts[0]);
        double lng1 = Double.parseDouble(parts[1]);
        return new LatLng(lat1, lng1);
    }

    public void createDummyData() {

        dummyLocationData = locationData.getBlankCopy();

        for (int i = 0; i < locationData.getRowCount() - 1; i++) {

            int dummyPointsCount = 0;
            Date d1 = DateUtils.getDateFromDateTime(locationData.getColumnValue("PhoneLogTime", i));
            Date d2 = DateUtils.getDateFromDateTime(locationData.getColumnValue("PhoneLogTime", i + 1));
            dummyPointsCount = Math.toIntExact((d2.getTime() - d1.getTime()) / 1000) * 2;
            if (locationData.getColumnValue("GPSCoordinates", i).equalsIgnoreCase("-1")
                    || locationData.getColumnValue("GPSCoordinates", i + 1).equalsIgnoreCase("-1")) {
                continue;
            }
            LatLng start = getLatLngFromCoord(locationData.getColumnValue("GPSCoordinates", i));
            LatLng end = getLatLngFromCoord(locationData.getColumnValue("GPSCoordinates", i + 1));
            // batteryList.add(locationData.getColumnValue("BatteryPercentage", i));
            // runnningTime.add(d1.getTime());
            temPoints.add(start);
            //runningLoations.add(locationData.getColumnValue("Message", i));
            for (ResultColumn rs : locationData.getValues()) {
                dummyLocationData.getColumn(rs.getColumnID()).addValueToArray(rs.getValues().get(i));
            }
            dummyLocationData.getColumn("PhoneLogTime").update(dummyLocationData.getRowCount() - 1, String.valueOf(d1.getTime()));
            //dummyLocationData.getColumn("Message").update(i,locationData.getColumnValue("Message",i));
            // dummyLocationData.getColumn("PhoneLogTime").update(i,);
            dummyLocationData.getColumn("Speed").update(dummyLocationData.getRowCount() - 1, String.valueOf(((calculateDistance(start.latitude, start.longitude, end.latitude, end.longitude) * (1000 * 60 * 60)) / ((d2.getTime() - d1.getTime())))));
            //speedList.add((float) ((calculateDistance(start.latitude, start.longitude, end.latitude, end.longitude) * (1000 * 60 * 60)) / ((d2.getTime() - d1.getTime()))));

            for (int p = 1; p < dummyPointsCount; p++) {
                float fraction = ((float) p) / ((float) dummyPointsCount);
                LatLng interpolatedPoint = interpolate(fraction, start, end);
                temPoints.add(interpolatedPoint);
                //  runnningTime.add(d1.getTime() + (p * 1000) / 2);
                for (ResultColumn rs : locationData.getValues()) {
                    dummyLocationData.getColumn(rs.getColumnID()).addValueToArray(rs.getValues().get(i));
                }


                dummyLocationData.getColumn("Message").update(dummyLocationData.getRowCount() - 1, locationData.getColumnValue("Message", i));
                dummyLocationData.getColumn("PhoneLogTime").update(dummyLocationData.getRowCount() - 1, String.valueOf((d1.getTime() + (p * 1000) / 2)));
                dummyLocationData.getColumn("BatteryPercentage").update(dummyLocationData.getRowCount() - 1, locationData.getColumnValue("BatteryPercentage", i));
                dummyLocationData.getColumn("Speed").update(dummyLocationData.getRowCount() - 1, String.valueOf(((calculateDistance(start.latitude, start.longitude, end.latitude, end.longitude) * (1000 * 60 * 60)) / ((d2.getTime() - d1.getTime())))));

                //speedList.add((float) );

                // runningLoations.add(locationData.getColumnValue("Message", i + 1));
                ///batteryList.add(locationData.getColumnValue("BatteryPercentage", i + 1));
            }
            if (i == locationData.getRowCount() - 2) {
                //runningLoations.add(locationData.getColumnValue("Message", i + 1));
                //  batteryList.add(locationData.getColumnValue("BatteryPercentage", i + 1));
                //dummyLocationData.getColumn("Message").update(i + 1, locationData.getColumnValue("Message", i + 1));
                //runnningTime.add(d2.getTime());
                for (ResultColumn rs : locationData.getValues()) {
                    dummyLocationData.getColumn(rs.getColumnID()).addValueToArray(rs.getValues().get(i));
                }
                dummyLocationData.getColumn("Speed").update(dummyLocationData.getRowCount() - 1, String.valueOf(((calculateDistance(start.latitude, start.longitude, end.latitude, end.longitude) * (1000 * 60 * 60)) / ((d2.getTime() - d1.getTime())))));
                dummyLocationData.getColumn("PhoneLogTime").update(dummyLocationData.getRowCount() - 1, String.valueOf(d2.getTime()));
                //  speedList.add((float) ((calculateDistance(start.latitude, start.longitude, end.latitude, end.longitude) * (1000 * 60 * 60)) / ((d2.getTime() - d1.getTime()))));
                temPoints.add(end);
            }
        }

    }

//
//    public void GenrateTemPoints(int last, int secondLast) {
//        if (locationData == null || locationData.getRowCount() <= last || locationData.getRowCount() <= secondLast) {
//            return;
//        }
//
//        Date d1 = DateUtils.getDateFromDateTime(locationData.getColumnValue("PhoneLogTime", last));
//        Date d2 = DateUtils.getDateFromDateTime(locationData.getColumnValue("PhoneLogTime", secondLast));
//
//        if (d1 == null || d2 == null) {
//            return;
//        }
//
//        String[] parts = locationData.getColumnValue("GPSCoordinates", last).split(",");
//        if (parts.length < 2) {
//            return;
//        }
//        double lat1 = Double.parseDouble(parts[0]);
//        double lng1 = Double.parseDouble(parts[1]);
//        LatLng start = new LatLng(lat1, lng1);
//
//        String[] parts1 = locationData.getColumnValue("GPSCoordinates", secondLast).split(",");
//        if (parts1.length < 2) {
//            return;
//        }
//        double lat2 = Double.parseDouble(parts1[0]);
//        double lng2 = Double.parseDouble(parts1[1]);
//        LatLng end = new LatLng(lat2, lng2);
//
//        temPoints.add(start);
//
//        long time1 = d1.getTime();
//        long time2 = d2.getTime();
//
//        if (time1 >= time2) {
//            return; // Handle invalid time scenario (optional)
//        }
//
//        long timediff = (time2 - time1) / 1000; // Time difference in seconds
//
//        for (int i = 1; i < timediff; i++) {
//            double fraction = (double) i / timediff;
//            LatLng interpolatedPoint = interpolate((float) fraction, start, end);
//            temPoints.add(interpolatedPoint);
//        }
//
//        temPoints.add(end);
//    }


    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        double dlon = lon2Rad - lon1Rad;
        double dlat = lat2Rad - lat1Rad;
        double a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(dlon / 2) * Math.sin(dlon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS_KM * c;

        return distance;
    }


    @Override
    public void OnDBResult(ColumnWiseResultHashMap columnWiseResultHashMap, int i, SQLQueryResult sqlQueryResult, String s, String s1) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hexProgressDialog.dismiss();
            }
        });
        if (i == GET_LOCATION_DATA) {
            if (sqlQueryResult.getErrorCode().get() == 0) {
                locationData = columnWiseResultHashMap;
                locationData.addResultColumn("Speed");
                for (int j = 0; j < locationData.getRowCount(); j++) {
                    locationData.getColumn("Speed").addValueToArray("-1");
                }

//                for (int j = columnWiseResultHashMap.getRowCount()-1; j >0 ; j--) {
//                    GenrateTemPoints(j, j -1);
//                }
                createDummyData();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        seekBar.setMax(temPoints.size() - 1);
                        drawPolyline(temPoints);
                        animateMarker(temPoints, 1);

                    }
                });
            }
        }
    }


    private void animateMarker(final List<LatLng> polylinePoints, int start) {
        if (polylinePoints.size() < 2) return;

        if (animatedMarker != null) {
            animatedMarker.remove();
        }


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                animatedMarker = gMap.addMarker(new MarkerOptions()
                        .position(polylinePoints.get(0))
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.carm2))
                        .flat(true)
                        .anchor(0.5f, 0.5f));
                gMap.moveCamera(CameraUpdateFactory.newLatLng(polylinePoints.get(0)));
                gMap.moveCamera(CameraUpdateFactory.zoomTo(15));
            }
        });


        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = start; i < polylinePoints.size() - 1; i++) {
                    if (isRunning != true) {
                        return;
                    }
                    runningIndex = i;
                    int finalI = i;
                    //distancesum += calculateDistance(polylinePoints.get(finalI - 1).latitude, polylinePoints.get(finalI - 1).longitude, polylinePoints.get(finalI).latitude, polylinePoints.get(finalI).longitude);
                    runOnUiThread(new Runnable() {
                        @SuppressLint("SetTextI18n")
                        @Override
                        public void run() {
                            animatedMarker.setPosition(polylinePoints.get(finalI));
                            animatedMarker.setAnchor(0.5f, 0.5f);
                            seekBar.setProgress(finalI);

                            currentLocationTxt.setText(dummyLocationData.getColumnValue("Message", finalI));
                            if (Float.parseFloat(dummyLocationData.getColumnValue("Speed", finalI)) >= 1) {
                                cardView.setVisibility(View.VISIBLE);
                                int speed = (int) Float.parseFloat(dummyLocationData.getColumnValue("Speed", finalI));
                                speedTxt.setText("" + speed);
                            } else {
                                cardView.setVisibility(View.GONE);
                            }

                            Date d1 = new Date(Long.parseLong(dummyLocationData.getColumnValue("PhoneLogTime", finalI)));
                            // float speed= (((float) calculateDistance(polylinePoints.get(finalI - 1).latitude, polylinePoints.get(finalI - 1).longitude, polylinePoints.get(finalI).latitude, polylinePoints.get(finalI).longitude)*(1000*60*60))/(d1.getTime()));
                            //  Log.d("speed", String.valueOf(speedList.get(finalI)));
                            timeTxt.setText(sdf.format(d1));
                            batteryTxt.setText(dummyLocationData.getColumnValue("BatteryPercentage", finalI) + "%");
                            if (Integer.parseInt(dummyLocationData.getColumnValue("BatteryPercentage", finalI)) <= 70 && Integer.parseInt(dummyLocationData.getColumnValue("BatteryPercentage", finalI)) > 60) {
                                battery.setImageResource(R.drawable.halfbattery);
                            } else if (Integer.parseInt(dummyLocationData.getColumnValue("BatteryPercentage", finalI)) <= 30 && Integer.parseInt(dummyLocationData.getColumnValue("BatteryPercentage", finalI)) > 0) {
                                battery.setImageResource(R.drawable.lowbattery);
                            } else if (Integer.parseInt(dummyLocationData.getColumnValue("BatteryPercentage", finalI)) > 70) {
                                battery.setImageResource(R.drawable.fullbattery);
                            }


                            ///  distanceTxt.setText(String.valueOf(distancesum) + " KM");

                            //                            bearingBetweenLocations(polylinePoints.get(finalI), polylinePoints.get(finalI+1));
                            float bearing = (float) bearingBetweenLocations(polylinePoints.get(finalI - 1), polylinePoints.get(finalI));
                            if (!Float.isNaN(bearing)) {
                                animatedMarker.setRotation(bearing);
                            }
                            gMap.moveCamera(CameraUpdateFactory.newLatLng(polylinePoints.get(finalI)));
                        }
                    });
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

            }

        }).start();


//                if(index%5==0) {
//
//                }


//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//
//                    }
//                },1500);

//
//        float bearing = (float) bearingBetweenLocations(startPosition, endPosition);
//        if (!Float.isNaN(bearing)) {
//            animatedMarker.setRotation(bearing);
//        }
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

    @Override
    public void onMultipleDBResult(LinkedHashMap<String, ColumnWiseResultHashMap> linkedHashMap, int i, SQLQueryResult sqlQueryResult, String s, String s1) {

    }

    @Override
    public void DBCheck(boolean b) {

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        checkLocationPermission();
        GetLocationData();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }


    private void drawPolyline(List<LatLng> points) {
        PolylineOptions polylineOptions = new PolylineOptions()
                .color(getResources().getColor(R.color.black))
                .geodesic(true)
                .width(10);

        polyline = gMap.addPolyline(polylineOptions.addAll(points));
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }
}