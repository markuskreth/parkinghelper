package de.kreth.parkinghelper;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.content.ContentValues.TAG;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class FetchAddressIntentService extends IntentService {

    protected ResultReceiver mReceiver;

    public FetchAddressIntentService() {
        super("FetchAddressIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        if (intent != null) {
            String errorMessage = "";

            double longitude = intent.getDoubleExtra(Constants.LOCATION_DATA_EXTRA_LON, 0);
            double latitude = intent.getDoubleExtra(Constants.LOCATION_DATA_EXTRA_LAT, 0);
            mReceiver = intent.getParcelableExtra(Constants.RECEIVER);

            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocation(
                        latitude,
                        longitude,
                        // In this sample, get just a single address.
                        1);
            } catch (IOException ioException) {
                // Catch network or other I/O problems.
                errorMessage = "Service nicht verfügbar";
                Log.e(getClass().getName(), errorMessage, ioException);
            } catch (IllegalArgumentException illegalArgumentException) {
                // Catch invalid latitude or longitude values.
                errorMessage = "ungültige Koordinaten";
                Log.e(getClass().getName(), errorMessage + ". " +
                        "Latitude = " + latitude +
                        ", Longitude = " +
                        longitude, illegalArgumentException);
            }
            if (addresses == null || addresses.size()  == 0) {
                if (errorMessage.isEmpty()) {
                    errorMessage = "Adress not found";
                    Log.e(TAG, errorMessage);
                }
                deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
            } else {
                Address address = addresses.get(0);
                ArrayList<String> addressFragments = new ArrayList<String>();

                // Fetch the address lines using getAddressLine,
                // join them, and send them to the thread.
                for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressFragments.add(address.getAddressLine(i));
                }
                Log.i(TAG, "Adress found");
                deliverResultToReceiver(Constants.SUCCESS_RESULT,
                        TextUtils.join(System.getProperty("line.separator"),
                                addressFragments));
            }
        }
    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
        mReceiver.send(resultCode, bundle);
    }

    public final class Constants {
        public static final int SUCCESS_RESULT = 0;
        public static final int FAILURE_RESULT = 1;
        public static final String PACKAGE_NAME =
                "com.google.android.gms.location.sample.locationaddress";
        public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
        public static final String RESULT_DATA_KEY = PACKAGE_NAME +
                ".RESULT_DATA_KEY";
        public static final String LOCATION_DATA_EXTRA_LON = PACKAGE_NAME +
                ".LOCATION_DATA_EXTRA_LONGITUDE";
        public static final String LOCATION_DATA_EXTRA_LAT = PACKAGE_NAME +
                ".LOCATION_DATA_EXTRA_LATITUDE";
    }
}
