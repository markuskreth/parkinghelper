package de.kreth.parkinghelper;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.os.ResultReceiver;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION_FOR_STORAGE = 13;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION_FOR_MAP_INIT = 14;
    private static final CharSequence COPY_LOCATION_ITEM = "COPY_LOCATION_ITEM_KEY";

    private int count = 0;
    private PositionAdapter adapter;
    private FusedLocationProviderClient mFusedLocationClient;
    private ClipboardManager clipboardManager;
    private MapView map;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Log.e(MainActivity.class.getName(), "App failed", e);
            }
        });

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                storeCurrentPosition();
            }
        });

        map = (MapView) findViewById(R.id.mapView);
        map.onCreate(savedInstanceState);
        MapsInitializer.initialize(this);

        listView = (ListView) findViewById(R.id.list_view_position);
        adapter = new PositionAdapter();

        List<PositionItem> positionItems = PositionItem.listAll(PositionItem.class);
        adapter.data.addAll(positionItems);

        Log.i(getClass().getName(), "Loaded " + adapter.data.size() + " items from database");
        listView.setAdapter(adapter);

    }

    @Override
    protected void onResume() {
        super.onResume();

        map.onResume();

        int selectedIndex = listView.getSelectedItemPosition();
        if (selectedIndex >= 0) {
            PositionItem item = adapter.getItem(selectedIndex);
            map.getMapAsync(new MapCallback(item));
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.d(getClass().getName(), "Permission not granted");

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this).setMessage("Location permission needed").setPositiveButton("OK", null).show();
            } else {
                Log.v(getClass().getName(), "Sending permission request");
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION_FOR_MAP_INIT);
            }
            return;
        }
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(createLocationRequest());
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                Log.i(getClass().getName(), "Gps available.");
                initMapLocation();
            }
        });
        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        Log.w(getClass().getName(), "Gps not available, starting User request.");
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MainActivity.this,
                                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION_FOR_MAP_INIT);
                        } catch (IntentSender.SendIntentException sendEx) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.e(getClass().getName(), "Gps not available, unable to start Dialog.");
                        break;
                }
            }
        });

//        Log.i(getClass().getName(), "Gps usable: " + task.getResult().getLocationSettingsStates().isGpsUsable());
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    private void initMapLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        final OnSuccessListener<Location> onSuccessListener = new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                map.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        MapCallback.initMap(googleMap);
                        if (location != null) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                            MarkerOptions opts = new MarkerOptions()
                                    .position(latLng)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                    .title("You are here")
                                    .visible(true);
                            googleMap.addMarker(opts);
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, MapCallback.DEFAULT_ZOOM_LEVEL));
                        }
                    }
                });
            }
        };

        LocationCallback callback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.d(getClass().getName(), "Got locations: " + locationResult.getLocations());
                onSuccessListener.onSuccess(locationResult.getLastLocation());
                mFusedLocationClient.removeLocationUpdates(this);
            }
        };

        mFusedLocationClient.requestLocationUpdates(createLocationRequest(), callback, getMainLooper());

    }

    @NonNull
    private LocationRequest createLocationRequest() {
        return LocationRequest.create().setNumUpdates(1).setInterval(5).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void storeCurrentPosition() {
        Log.d(getClass().getName(), "Fetching and storing Location " + count);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            Log.d(getClass().getName(), "Permission not granted");

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this).setMessage("Location permission needed").setPositiveButton("OK", null).show();
            } else {
                Log.v(getClass().getName(), "Sending permission request");
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION_FOR_STORAGE);
            }
            return;
        }
        Log.v(getClass().getName(), "getting last location");

        final OnSuccessListener<Location> onSuccessListener = new OnSuccessListener<Location>() {

            @Override
            public void onSuccess(final Location location) {
                Log.v(getClass().getName(), "Location fetched: " + location);
                if (location != null) {

                    final View nameDialog = getLayoutInflater().inflate(R.layout.name_dialog, null);
                    final Spinner preselect = nameDialog.findViewById(R.id.spinner);
                    final EditText textView = nameDialog.findViewById(R.id.editText);

                    final ArrayAdapter<String> spAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item);
                    preselect.setAdapter(spAdapter);
                    for (PositionItem pos : adapter.data) {
                        spAdapter.add(pos.getName());
                    }
                    preselect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            textView.setText(spAdapter.getItem(position));
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });

                    new AlertDialog.Builder(MainActivity.this)
                            .setView(nameDialog)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    String txt = textView.getText().toString().trim();
                                    if (txt.length() == 0) {
                                        count++;
                                        txt = "Custom " + count;
                                    }

                                    for (PositionItem item : adapter.data) {
                                        if (item.name.equals(txt)) {
                                            item.setLocation(location);
                                            item.save();
                                            adapter.notifyDataSetChanged();

                                            fetchAdress(item);
                                            return;
                                        }
                                    }

                                    PositionItem item = PositionItem.create(txt, location);
                                    item.save();
                                    adapter.add(item);

                                    fetchAdress(item);

                                }
                            }).show();
                } else {
                    Log.i(getClass().getName(), "Error fetching location, was null");
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Fehler")
                            .setMessage("Koordinaten konnten nicht empfangen werden!")
                            .setPositiveButton("OK", null).show();
                }
            }
        };

        LocationCallback callback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Log.d(getClass().getName(), "Got locations: " + locationResult.getLocations());
                onSuccessListener.onSuccess(locationResult.getLastLocation());
                mFusedLocationClient.removeLocationUpdates(this);
            }
        };
        mFusedLocationClient.requestLocationUpdates(createLocationRequest(), callback, getMainLooper());


    }

    private void fetchAdress(PositionItem item) {
        Intent intent = new Intent(MainActivity.this, FetchAddressIntentService.class);
        intent.putExtra(FetchAddressIntentService.Constants.RECEIVER, new AddressResultReceiver(new Handler(getMainLooper()), item));
        intent.putExtra(FetchAddressIntentService.Constants.LOCATION_DATA_EXTRA_LAT, item.getLatitude());
        intent.putExtra(FetchAddressIntentService.Constants.LOCATION_DATA_EXTRA_LON, item.getLongitude());
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == R.id.import_from_clipboard) {
            ClipData clip = clipboardManager.getPrimaryClip();

            if(clip.getItemCount() == 1) {
                CharSequence json = clip.getItemAt(0).getText();
                PositionItem positionItem  = PositionItem.fromJson(json);
                if(positionItem == null) {
                    Toast.makeText(this, "No location data found", Toast.LENGTH_LONG);
                    return false;
                }
                for (PositionItem p: adapter.data) {
                    if (p.name.equals(positionItem.name)) {
                        p.setLatitude(positionItem.getLatitude());
                        p.setLongitude(positionItem.getLongitude());
                        p.setAdress(positionItem.getAdress());
                        if(p.adress == null || p.adress.trim().isEmpty()) {
                            fetchAdress(p);
                        }
                        p.save();
                        adapter.notifyDataSetChanged();
                        return true;
                    }
                }

                positionItem.save();
                adapter.add(positionItem);

                if(positionItem.adress == null || positionItem.adress.trim().isEmpty()) {
                    fetchAdress(positionItem);
                }
            }else {
                Toast.makeText(this, "No location data found", Toast.LENGTH_LONG);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION_FOR_STORAGE: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    storeCurrentPosition();
                } else {
                    new AlertDialog.Builder(this).setMessage("No Permission for Location, cancel").setPositiveButton("OK", null).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION_FOR_MAP_INIT:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        initMapLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to

                        break;
                    default:
                        break;
                }
                break;
        }
    }
    private class PositionAdapter extends BaseAdapter {

        private final List<PositionItem> data = new ArrayList<>();

        public void add (PositionItem item) {
            data.add(item);
            Log.d(getClass().getName(), "Added item " + item);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public PositionItem getItem(int position) {
            if(position<0 || position>=data.size()) {
                return null;
            }
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            LayoutHolder holder;

            if(convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.position_item_layout, null);
                holder = new LayoutHolder(convertView);

                convertView.setTag(holder);
            } else {
                holder = (LayoutHolder) convertView.getTag();
            }

            final PositionItem item = getItem(position);
            holder.update(item);
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    map.getMapAsync(new MapCallback(item));
                }
            });
            convertView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    PopupMenu m = new PopupMenu(MainActivity.this, v);
                    m.getMenuInflater().inflate(R.menu.item_menu, m.getMenu());
                    m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItemitem) {
                            switch (menuItemitem.getItemId()) {
                                case R.id.delete_item:
                                    item.delete();
                                    adapter.data.remove(item);
                                    adapter.notifyDataSetChanged();
                                    break;
                                case R.id.open_in_maps:
                                    StringBuilder geo = new StringBuilder("geo:");
                                    geo.append(item.getLatitude());
                                    geo.append(',');
                                    geo.append(item.getLongitude());
                                    Uri gmmIntentUri = Uri.parse(geo.toString());
                                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                                    mapIntent.setPackage("com.google.android.apps.maps");
                                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                                        startActivity(mapIntent);
                                    }
                                    break;
                                case R.id.copy_item:
                                    ClipData clip = ClipData.newPlainText(COPY_LOCATION_ITEM, item.toJson());
                                    clipboardManager.setPrimaryClip(clip);
                                    break;
                            }
                            return false;
                        }
                    });
                    m.show();

                    return true;
                }
            });
            return convertView;
        }

        private class LayoutHolder {
            private final TextView caption;
            private final TextView description;

            public LayoutHolder(View convertView) {
                caption = convertView.findViewById(R.id.textViewCaption);
                description = convertView.findViewById(R.id.textViewDescription);
            }

            void update(final PositionItem item) {

                StringBuilder desc = new StringBuilder()
                        .append(item.getLatitude())
                        .append(":")
                        .append(item.getLongitude());

                if(item.adress != null) {
                    desc.append("\n").append(item.getAdress());
                }
                caption.setText(item.getName());
                description.setText(desc);

            }
        }
    }

    public class AddressResultReceiver extends ResultReceiver {

        private final PositionItem item;

        public AddressResultReceiver(Handler handler, PositionItem item) {
            super(handler);
            this.item = item;
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            String mAddressOutput = resultData.getString(FetchAddressIntentService.Constants.RESULT_DATA_KEY);
            item.setAdress(mAddressOutput);
            item.save();

            adapter.notifyDataSetChanged();
            // Show a toast message if an address was found.
            if (resultCode == FetchAddressIntentService.Constants.SUCCESS_RESULT) {
                Toast.makeText(MainActivity.this, "Adress found!", Toast.LENGTH_SHORT);
            }

        }
    }
}
