package de.kreth.parkinghelper;

import android.location.Location;
import android.support.annotation.NonNull;
import android.util.Log;

import com.orm.SugarRecord;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by markus.kreth on 06.10.2017.
 */
public class PositionItem extends SugarRecord<PositionItem> {

    @NonNull String name;
    private double longitude;
    private double latitude;
    String adress = null;

    public static PositionItem create(@NonNull String name, @NonNull Location location) {
        return new PositionItem(name, location);
    }

    public PositionItem() {
    }

    public PositionItem(String name, Location location) {
        this.name = name;
        longitude = location.getLongitude();
        latitude = location.getLatitude();
    }

    public String getName() {
        return name;
    }

    public synchronized void setAdress(String adress) {
        this.adress = adress;
    }

    public String getAdress() {
        return adress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PositionItem item = (PositionItem) o;

        if (Double.compare(item.longitude, longitude) != 0) return false;
        if (Double.compare(item.latitude, latitude) != 0) return false;
        if (!name.equals(item.name)) return false;
        return adress != null ? adress.equals(item.adress) : item.adress == null;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = name.hashCode();
        temp = Double.doubleToLongBits(longitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(latitude);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + (adress != null ? adress.hashCode() : 0);
        return result;
    }

    public String toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("name", name);
            json.put("latitude", latitude);
            json.put("longitude", longitude);
            json.put("longitude", longitude);
            if(adress != null) {
                json.put("adress", adress);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    @Override
    public String toString() {
        return name + ": " + latitude + ":" + longitude;
    }

    public void setLocation(@NonNull Location location) {
        longitude = location.getLongitude();
        latitude = location.getLatitude();
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public static PositionItem fromJson(CharSequence json) {
        PositionItem item;
        try {
            JSONObject jsonObject = new JSONObject(json.toString());
            item = new PositionItem();
            item.name = jsonObject.getString("name");
            item.longitude = jsonObject.getDouble("longitude");
            item.latitude = jsonObject.getDouble("latitude");
            if(jsonObject.has("adress")) {
                item.adress = jsonObject.getString("adress");
            }
        } catch (JSONException e) {
            item = null;
            Log.e(PositionItem.class.getName(), "Error creating object from json", e);
        }
        return item;
    }
}
