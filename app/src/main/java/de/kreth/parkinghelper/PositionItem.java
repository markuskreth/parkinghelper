package de.kreth.parkinghelper;

import android.location.Location;
import android.support.annotation.NonNull;

import com.orm.SugarRecord;

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

    @Override
    public String toString() {
        return name + ": " + latitude + ":" + longitude;
    }

    public void setLocation(@NonNull Location location) {
        longitude = location.getLongitude();
        latitude = location.getLatitude();
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
