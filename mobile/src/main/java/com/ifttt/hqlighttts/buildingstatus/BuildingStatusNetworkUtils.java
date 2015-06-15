package com.ifttt.hqlighttts.buildingstatus;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import com.ifttt.hqlighttts.util.datastructure.sortedintmap.SortedIntMap;
import com.ifttt.hqlighttts.HqLightttsApp;
import com.nightlynexus.dokie.network.DokieHttpClient.CallObject;
import com.squareup.okhttp.Request;

import android.util.Log;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class BuildingStatusNetworkUtils {

    private static final String URL_BUILDING_STATUS = "https://biff.ifttt.com/bathrooms.json";

    public static CallObject<BuildingStatus> newCallBuildingStatus() {
        return HqLightttsApp.HTTP_CLIENT.newCallObject(new Request.Builder().url(URL_BUILDING_STATUS).build());
    }

    /**
     * BuildingStatusTypeAdapter is a Gson TypeAdapter that has knowledge about parsing the JSON API
     */
    public static class BuildingStatusTypeAdapter extends TypeAdapter<BuildingStatus> {

        private static final String TAG_UNEXPECTED_READING
                = BuildingStatusTypeAdapter.class.getSimpleName() + " unexpected while reading";
        private static final DateFormat DATE_FORMAT_TIMESTAMP
                = new SimpleDateFormat("hh:mm:ss a", Locale.US);
        private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("America/Los_Angeles");
        static {
            DATE_FORMAT_TIMESTAMP.setTimeZone(TIME_ZONE);
        }
        private static final int GUESS_ROOMS = 3;
        private static final String KEY_TIMESTAMP = "ts";
        private static final String VALUE_OPEN = "open";
        private static final String VALUE_CLOSED = "closed";
        private static final String VALUE_CAUTION = "caution";
        private static final String VALUE_UNKNOWN = "unknown";

        @Override
        public void write(JsonWriter out, BuildingStatus value) throws IOException {
            out.beginObject();
            for (int i = 0; i < value.rooms.size(); i++) {
                int key = value.rooms.keyAt(i);
                out.name(Integer.toString(key));
                String valueStatus;
                switch (value.rooms.get(key)) {
                    case BuildingStatus.OPEN:
                        valueStatus = VALUE_OPEN;
                        break;
                    case BuildingStatus.CLOSED:
                        valueStatus = VALUE_CLOSED;
                        break;
                    case BuildingStatus.CAUTION:
                        valueStatus = VALUE_CAUTION;
                        break;
                    default:
                        valueStatus = VALUE_UNKNOWN;
                        break;
                }
                out.value(valueStatus);
            }
            out.name(KEY_TIMESTAMP);
            out.value(DATE_FORMAT_TIMESTAMP.format(value.timestamp));
            out.endObject();
        }

        @Override
        public BuildingStatus read(JsonReader in) throws IOException {

            String timestampString = null;
            SortedIntMap rooms = new SortedIntMap(GUESS_ROOMS); // guess at initial capacity

            in.beginObject();
            while (in.hasNext()) {
                String key = in.nextName();
                if (key.equals(KEY_TIMESTAMP)) {
                    timestampString = in.nextString();
                } else {
                    try {
                        int numLight = Integer.parseInt(key);
                        int roomStatus;
                        switch (in.nextString()) {
                            case VALUE_OPEN:
                                roomStatus = BuildingStatus.OPEN;
                                break;
                            case VALUE_CLOSED:
                                roomStatus = BuildingStatus.CLOSED;
                                break;
                            case VALUE_CAUTION:
                                roomStatus = BuildingStatus.CAUTION;
                                break;
                            default:
                                roomStatus = BuildingStatus.UNKNOWN;
                                break;
                        }
                        rooms.put(numLight, roomStatus);
                    } catch (NumberFormatException e) {
                        // the key was not the timestamp key and was not an integer;
                        // do nothing with the key or its value
                        logUnexpectedParsing("Unexpected key: " + key);
                        in.skipValue();
                    }
                }
            }
            in.endObject();

            Date timestamp;
            if (timestampString == null) {
                logUnexpectedParsing("No timestamp key found.  Expected key: " + KEY_TIMESTAMP);
                timestamp = null;
            } else {
                try {
                    timestamp = parse(timestampString);
                } catch (ParseException e) {
                    e.printStackTrace();
                    logUnexpectedParsing("Error parsing timestamp: " + timestampString);
                    timestamp = null;
                }
            }
            return new BuildingStatus(rooms, timestamp);
        }

        private static Date parse(String string) throws ParseException {
            Date date = DATE_FORMAT_TIMESTAMP.parse(string);
            Calendar calendar = Calendar.getInstance(TIME_ZONE, Locale.US);
            int day = calendar.get(Calendar.DAY_OF_YEAR);
            int year = calendar.get(Calendar.YEAR);
            calendar.setTime(date);
            calendar.set(Calendar.DAY_OF_YEAR, day);
            calendar.set(Calendar.YEAR, year);
            return calendar.getTime();
        }

        private static void logUnexpectedParsing(String msg) {
            Log.e(TAG_UNEXPECTED_READING, msg);
        }
    }

    private BuildingStatusNetworkUtils() {
        throw new AssertionError("No instances");
    }
}
