package com.hex.currentlocationapplication;

import android.animation.TimeInterpolator;

import com.google.android.gms.maps.model.LatLng;

public interface LatLngInterpolator {
    LatLng interpolate(float fraction, LatLng a, LatLng b);

    class Linear implements LatLngInterpolator, TimeInterpolator {
        @Override
        public LatLng interpolate(float fraction, LatLng a, LatLng b) {
            double lat = (b.latitude - a.latitude) * fraction + a.latitude;
            double lng = (b.longitude - a.longitude) * fraction + a.longitude;
            return new LatLng(lat, lng);
        }

        @Override
        public float getInterpolation(float input) {
            return 0;
        }
    }
}
