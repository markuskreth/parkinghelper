package de.kreth.parkinghelper;

import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by markus.kreth on 08.10.2017.
 */

public class MapCallback implements OnMapReadyCallback {

    public static final int DEFAULT_ZOOM_LEVEL = 17;
    private final PositionItem item;

    public MapCallback(PositionItem item) {
        this.item = item;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(getClass().getName(), "Fetching marker for location " + item);
        initMap(googleMap);
        LatLng latLng = new LatLng(item.getLatitude(), item.getLongitude());
        MarkerOptions opts = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title("Location: " + item.getName())
                .visible(true);
        googleMap.addMarker(opts);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM_LEVEL));
        //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM_LEVEL));

    }

    public static void initMap(GoogleMap googleMap) {
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.clear();
        final UiSettings uiSettings = googleMap.getUiSettings();
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMapToolbarEnabled(true);
    }


}
