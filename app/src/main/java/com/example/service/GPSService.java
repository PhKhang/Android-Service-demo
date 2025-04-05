package com.example.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.List;

public class GPSService extends Service {
    String GPS_FILTER = "com.example.service.action.GPSFIX";
    Thread serviceThread;
    LocationManager lm;
    GPSListener myLocationListener;

    Boolean isRunning = false;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.e("<<MyGpsService-onStart>>", "I am alive-GPS!");
        serviceThread = new Thread(new Runnable() {
            public void run() {
//                getGPSFix_Version1();// uses NETWORK provider(chạy 1 lần)
                getGPSFix_Version2(); // uses GPS chip provider(loop)
            }// run
        });
        serviceThread.start();
    }// onStart


    private Location getLastKnownLocation() {
        LocationManager mLocationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e("<<MyGpsService>>", "A Permission denied");
                return null; // Exit early if permissions are missing
            }
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    public void getGPSFix_Version1() {
// Get the location manager
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
// work with best provider
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, false);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("<<MyGpsService>>", "Permission denied");
            return;
        }

        if (provider == null) {
            Log.e("<<MyGpsService>>", "No provider found");
            return;
        } else {
            Log.e("<<MyGpsService>>", "Provider found: " + provider);
        }

//        Location location = locationManager.getLastKnownLocation(provider);
            Location location = getLastKnownLocation();
            if (location != null) {
// capture location data sent by current provider
                Log.e("<<MyGpsService>>", "Location not null");
                double latitude = location.getLatitude(), longitude = location.getLongitude();
// assemble data bundle to be broadcasted
                Intent myFilteredResponse = new Intent(GPS_FILTER);
                myFilteredResponse.putExtra("latitude", latitude);
                myFilteredResponse.putExtra("longitude", longitude);
                myFilteredResponse.putExtra("provider", provider);
                Log.e(">>GPS_Service<<", provider + " =>Lat:" + latitude + " lon:" + longitude);
// send the location data out
                sendBroadcast(myFilteredResponse);

            } else {
                Log.e("<<MyGpsService>>", "Location is null");
            }
    }

    public void getGPSFix_Version2() {
        try {
            Looper.prepare();
            lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            myLocationListener = new GPSListener();
            long minTime = 2000; // 2 seconds
            float minDistance = 0; // Update every 2 seconds regardless of distance

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e("<<MyGpsService>>", "Permission denied");
                return;
            } else {
                Log.e("<<MyGpsService>>", "Permission granted");
            }

            // Request updates from GPS_PROVIDER
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, myLocationListener);
            Log.e("<<MyGpsService>>", "Location updates requested");

            // Initial broadcast of last known location
            Location location = getLastKnownLocation();
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                Intent myFilteredResponse = new Intent(GPS_FILTER);
                myFilteredResponse.putExtra("latitude", latitude);
                myFilteredResponse.putExtra("longitude", longitude);
                myFilteredResponse.putExtra("provider", location.getProvider());
                Log.e(">>GPS_Service<<", "Initial: Lat:" + latitude + " lon:" + longitude);
                sendBroadcast(myFilteredResponse);
            }

            Looper.loop();
        } catch (Exception e) {
            Log.e("<<MyGpsService>>", "Error in getGPSFix_Version2: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(" << MyGpsService - onDestroy >>", "I am dead - GPS");
        isRunning = false;
        try {
            lm.removeUpdates(myLocationListener);
            isRunning = false;
        } catch (Exception e) {
        }
    }// onDestroy

    private class GPSListener implements LocationListener {
        public void onLocationChanged(Location location) {
// capture location data sent by current provider
            double latitude = location.getLatitude(), longitude = location.getLongitude();
// assemble data bundle to be broadcasted
            Intent myFilteredResponse = new Intent(GPS_FILTER);
            myFilteredResponse.putExtra("latitude", latitude);
            myFilteredResponse.putExtra("longitude", longitude);
            myFilteredResponse.putExtra("provider", location.getProvider());
            Log.e(" >> GPS_Service <<", "Lat:" + latitude + "lon:" + longitude);
// send the location data out
            sendBroadcast(myFilteredResponse);
        }

        public void onProviderDisabled(String provider) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    ;// GPSListener class
}// MyService3
