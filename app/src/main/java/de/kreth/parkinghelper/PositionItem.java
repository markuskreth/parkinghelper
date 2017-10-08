package de.kreth.parkinghelper;

import android.location.Location;
import android.support.annotation.NonNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by markus.kreth on 06.10.2017.
 */
public class PositionItem {

    private static AtomicLong lastId = new AtomicLong(1);

    long id;
    String name;
    Location location;

    public static PositionItem create(@NonNull String name, @NonNull Location location) {
        return new PositionItem(lastId.incrementAndGet(), name, location);
    }

    public static PositionItem create(long id, @NonNull String name, @NonNull Location location) {
        if(id > lastId.longValue()) {
            lastId.set(id);
        }
        return new PositionItem(id, name, location);
    }

    private PositionItem(long id, String name, Location location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return name + ": " + location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PositionItem that = (PositionItem) o;

        if (id != that.id) return false;
        if (!name.equals(that.name)) return false;
        return location.equals(that.location);

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + name.hashCode();
        result = 31 * result + location.hashCode();
        return result;
    }
}
