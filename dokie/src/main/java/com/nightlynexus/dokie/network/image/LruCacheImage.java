package com.nightlynexus.dokie.network.image;

import android.graphics.Bitmap;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.HONEYCOMB_MR1;
import static android.os.Build.VERSION_CODES.KITKAT;

public class LruCacheImage implements CacheImage {

    private static final float LOAD_FACTOR = 0.75f;

    private final LinkedHashMap<String, Bitmap> mCache;
    private final int mMaxSize;
    private int mCurrentSize;

    public LruCacheImage(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Max size must be positive");
        }
        mCache = new LinkedHashMap<String, Bitmap>(0, LOAD_FACTOR, true);
        mMaxSize = maxSize;
        mCurrentSize = 0;
    }

    @Override
    public Bitmap get(String key) {
        return mCache.get(key);
    }

    @Override
    public void set(String key, Bitmap bitmap) {
        mCurrentSize += getBitmapBytes(bitmap);
        boolean ableToCache = trim();
        if (ableToCache) {
            mCache.put(key, bitmap);
        }
    }

    @Override
    public void clear() {
        mCache.clear();
        mCurrentSize = 0;
    }

    private boolean trim() {
        while (mCurrentSize > mMaxSize) {
            Iterator<Map.Entry<String, Bitmap>> iterator = mCache.entrySet().iterator();
            if (!iterator.hasNext()) {
                mCurrentSize = 0;
                return false;
            }
            Map.Entry<String, Bitmap> toEvict = iterator.next();
            mCache.remove(toEvict.getKey());
            mCurrentSize -= getBitmapBytes(toEvict.getValue());
        }
        return true;
    }

    private static int getBitmapBytes(Bitmap bitmap) {
        if (SDK_INT >= KITKAT) {
            return bitmap.getAllocationByteCount();
        } else if (SDK_INT >= HONEYCOMB_MR1) {
            return bitmap.getByteCount();
        } else {
            return bitmap.getRowBytes() * bitmap.getHeight();
        }
    }
}
