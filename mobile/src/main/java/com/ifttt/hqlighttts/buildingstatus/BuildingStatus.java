package com.ifttt.hqlighttts.buildingstatus;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ifttt.hqlighttts.util.datastructure.sortedintmap.SortedIntMapGettable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;

public class BuildingStatus {

    @IntDef({UNKNOWN, OPEN, CLOSED, CAUTION})
    @Retention(RetentionPolicy.SOURCE)
    public @interface RoomStatus {}

    public static final int UNKNOWN = 0;
    public static final int OPEN = 1;
    public static final int CLOSED = 2;
    public static final int CAUTION = 3;

    @NonNull public final SortedIntMapGettable rooms;
    @Nullable public final Date timestamp;

    public BuildingStatus(@NonNull SortedIntMapGettable rooms, @Nullable Date timestamp) {
        this.rooms = rooms;
        this.timestamp = timestamp;
    }
}
