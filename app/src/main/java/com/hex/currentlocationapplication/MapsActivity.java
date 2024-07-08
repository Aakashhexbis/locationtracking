package com.hex.currentlocationapplication;

import static com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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
import com.hex.hextools.Api.SQLQueryResult;
import com.hex.hextools.Api.SqlString;
import com.hex.hextools.DateUtils.DateUtils;
import com.hex.hextools.Information.Information;
import com.hex.hextools.Widgets.HexAlertDialog;
import com.hex.hextools.Widgets.HexProgressDialog;
import com.hex.hextools.Widgets.HexTextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, HexApiService.Listener {

    int GET_LOCATION_DATA = Information.getUniqueID();
    HexApiService hexApiService;
    int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    GoogleMap gMap;
    Handler handler;
    SeekBar seekBar;
    TextView timeTxt;
    TextView currentLocationTxt;
    TextView distanceTxt;
    HexProgressDialog hexProgressDialog;
    ColumnWiseResultHashMap columnWiseResultHashMap;
    FusedLocationProviderClient fusedLocationProviderClient;
    private Marker animatedMarker;
    private List<LatLng> path = new ArrayList<>();
    private Polyline polyline;
    List<Double> distances = new ArrayList<>();
    List<String> message = new ArrayList<>();
    List<String> toTime = new ArrayList<>();
    ValueAnimator valueAnimator;
HexTextView Employe;
    ImageView btnPlay;
    DateFormat sdf = new SimpleDateFormat("HH:mm:ss:aa");
    LatLng startLocation;
    double sum = 0;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        seekBar = findViewById(R.id.proogressBar);
        Information.setDB(this, "AakashTraining");
        Information.setApiPath(this, "https:/trackhr.co.in/AakashTraining/api/");
        hexApiService = new HexApiService((Context) this, this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        hexProgressDialog = new HexProgressDialog(MapsActivity.this);
        timeTxt = findViewById(R.id.time);
        currentLocationTxt = findViewById(R.id.currentLocation);
        distanceTxt = findViewById(R.id.distance);
        btnPlay = findViewById(R.id.btnPlay);
      //  Employe = findViewById(R.id.bikeId);
Boolean click;
        currentLocationTxt.setSelected(true);

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (valueAnimator != null && !valueAnimator.isRunning() && seekBar.getProgress() == 0) {
                    valueAnimator.start();

                    setMapUiGuesstures(false);
                    btnPlay.setImageResource(R.drawable.pause2);
                } else {
                    if (valueAnimator.isPaused()) {
                        valueAnimator.resume();
                        btnPlay.setImageResource(R.drawable.pause2);
                        setMapUiGuesstures(false);
                    } else if (seekBar.getProgress() == path.size() - 1) {
                        valueAnimator.start();
                        btnPlay.setImageResource(R.drawable.pause2);
                        setMapUiGuesstures(false);
                    } else {
                        btnPlay.setImageResource(R.drawable.play);
                        valueAnimator.pause();
                        setMapUiGuesstures(true);
                    }
                }

            }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        handler = new Handler();

        checkLocationPermission();
    }

    void GetLocationData() {
        hexProgressDialog.setMessage("Loading...");
        hexProgressDialog.show();
        String query = SqlString.format("GP_Proc_MapActivity_GetLocations");
        hexApiService.execSql(query, GET_LOCATION_DATA, "", "");
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return;
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    public void animateMarker(LatLng startPosition, LatLng endPosition) {
        float bearing = (float) bearingBetweenLocations(startPosition, endPosition);
        if (!Float.isNaN(bearing)) {
            animatedMarker.setRotation(bearing);
        }

        animatedMarker.setPosition(endPosition);

    }


    private void animateMarker(final List<LatLng> polylinePoints) {
        if (polylinePoints.size() < 2) return;

        if (animatedMarker != null) {
            animatedMarker.remove();
        }


        animatedMarker = gMap.addMarker(new MarkerOptions()
                .position(polylinePoints.get(0))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.carm2))
                .flat(true)
                .anchor(0.5f, 0.5f));

        valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(7000 * (polylinePoints.size() - 1)); // Duration in milliseconds
        valueAnimator.setInterpolator(new LinearInterpolator());

        valueAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            Date date;
            int index = (int) (fraction * (polylinePoints.size() - 1));
            // int temIndex=(int) (fraction * (polylinePoints.size() - 1)) - index;
            float segmentFraction = (fraction * (polylinePoints.size() - 1)) - index;

            if (index < polylinePoints.size() - 1) {
                LatLng startPosition = polylinePoints.get(index);
                LatLng endPosition = polylinePoints.get(index + 1);


                LatLng newPosition = interpolate(segmentFraction, startPosition, endPosition);

                animatedMarker.setPosition(newPosition);

                seekBar.setProgress(index + 1);

//                if(index%5==0) {
//
//                }

                gMap.moveCamera(CameraUpdateFactory.newLatLng(newPosition));
//                new Handler().postDelayed(new Runnable() {
//                    @Override
//                    public void run() {
//
//                    }
//                },1500);



                float bearing = (float) bearingBetweenLocations(startPosition, endPosition);
                if (!Float.isNaN(bearing)) {
                    animatedMarker.setRotation(bearing);
                }
            }
        });
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

    private void drawPolyline(List<LatLng> points) {
        PolylineOptions polylineOptions = new PolylineOptions()
                .color(getResources().getColor(R.color.black))
                .geodesic(true)
                .width(10);

        polyline = gMap.addPolyline(polylineOptions.addAll(points));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
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
                this.columnWiseResultHashMap = columnWiseResultHashMap;
                path.clear();
                distances.clear();
                message.clear();
                toTime.clear();

                for (int j = this.columnWiseResultHashMap.getRowCount() - 1; j >= 0; j--) {
                    String[] parts = this.columnWiseResultHashMap.getColumnValue("GPSCoordinates", j).split(",");
                    double lat = Double.parseDouble(parts[0]);
                    double lng = Double.parseDouble(parts[1]);
                    LatLng latLng = new LatLng(lat, lng);
                    path.add(latLng);
                    distances.add(Double.valueOf(this.columnWiseResultHashMap.getColumnValue("Distance", j)));
                    message.add(this.columnWiseResultHashMap.getColumnValue("Message", j));
                    toTime.add(this.columnWiseResultHashMap.getColumnValue("ToTime", j));

                }

                if (!path.isEmpty()) {
                    startLocation = path.get(0);
                    seekBar.setMax(path.size() - 1);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            drawPolyline(path);
                            timeTxt.setText(sdf.format(DateUtils.getDateFromDateTime(toTime.get(0))));
                            distanceTxt.setText(String.valueOf(distances.get(0) / 1000) + " Km");
                            currentLocationTxt.setText(message.get(0));

                        }
                    });


                    seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                            // Date date;
                            if (progress == path.size() - 1) {
                                btnPlay.setImageResource(R.drawable.play);
                                animateMarker(path);
                                setMapUiGuesstures(true);
                            }

                            if (progress < path.size()) {

                                if (valueAnimator != null && !valueAnimator.isRunning() && progress < path.size() - 1) {
                                    animateMarker(path.get(progress), path.get(progress + 1));

                                }


//                                        sum = 0;
//                                        date = null;
//                                        for (int i = 0; i <= progress; i++) {
//                                            sum += distances.get(i);
//                                            date = DateUtils.getDateFromDateTime(toTime.get(i));
//                                        }
//                                distanceTxt.setText(String.valueOf(sum / 1000) + " Km");
//                                      timeTxt.setText(sdf.format(date));


                                final Date[] date = {null};
                                final String[] currentLocation = {null};
                                new AsyncTask<Void, Void, Void>() {

                                    @Override
                                    protected Void doInBackground(Void... voids) {
                                        sum = 0;
                                        date[0] = null;
                                        for (int i = 0; i <= seekBar.getProgress(); i++) {
                                            sum += distances.get(i);
                                            date[0] = DateUtils.getDateFromDateTime(toTime.get(i));
                                            currentLocation[0] = message.get(i);
                                        }
                                        return null;
                                    }

                                    @Override
                                    protected void onPostExecute(Void aVoid) {
                                        distanceTxt.setText(String.valueOf(sum / 1000) + " KM");
                                        timeTxt.setText(sdf.format(date[0]));
                                        currentLocationTxt.setText(currentLocation[0]);
                                    }

                                }.execute();


                            }
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                            if (valueAnimator != null && valueAnimator.isRunning()) {
                                valueAnimator.cancel();
                                btnPlay.setImageResource(R.drawable.play);
                                setMapUiGuesstures(true);
                            }
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {

                            int progress = seekBar.getProgress();
                            if (progress <= path.size() - 1) {
                                valueAnimator.setCurrentFraction((float) (progress) / (float) path.size());
                                // animateMarker(path.get(progress), path.get(progress + 1));
                                if (progress == path.size() - 1) {
                                    btnPlay.setImageResource(R.drawable.play);
                                    setMapUiGuesstures(true);
                                } else {
                                    btnPlay.setImageResource(R.drawable.pause2);
                                    setMapUiGuesstures(false);
                                }
                                valueAnimator.start();


                            }

                        }
                    });
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // gMap.addMarker(new MarkerOptions().position(startLocation).icon(BitmapDescriptorFactory.defaultMarker(HUE_GREEN)).title("Location 1"));
                            gMap.moveCamera(CameraUpdateFactory.newLatLng(startLocation));
                            gMap.animateCamera(CameraUpdateFactory.zoomTo(14.8f));
                            animateMarker(path);
                        }
                    });

                }
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        new HexAlertDialog().getBuilder(MapsActivity.this)

                                .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        GetLocationData();
                                    }
                                })

                                .setNegativeButton("Cencel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        setResult(RESULT_CANCELED);
                                        finish();
                                    }
                                })
                                .setMessage("An Error Occured, Kindly Retry!")
                                .setCancelable(false)
                                .show();
                    }
                });
            }
        }
    }


    public void setMapUiGuesstures(boolean enabled) {
        gMap.getUiSettings().setZoomGesturesEnabled(enabled);
        gMap.getUiSettings().setScrollGesturesEnabled(enabled);
        gMap.getUiSettings().setZoomControlsEnabled(enabled);
        gMap.getUiSettings().setCompassEnabled(enabled);
    }


    @Override
    public void onMultipleDBResult(LinkedHashMap<String, ColumnWiseResultHashMap> linkedHashMap, int i, SQLQueryResult sqlQueryResult, String s, String s1) {

    }

    @Override
    public void DBCheck(boolean b) {

    }
}
