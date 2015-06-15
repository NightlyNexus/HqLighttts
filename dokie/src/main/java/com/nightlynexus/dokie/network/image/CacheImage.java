package com.nightlynexus.dokie.network.image;

import android.graphics.Bitmap;

public interface CacheImage {

    Bitmap get(String key);
    void set(String key, Bitmap bitmap);
    void clear();

    CacheImage NONE = new CacheImage() {

        @Override
        public Bitmap get(String key) {
            return null;
        }

        @Override
        public void set(String key, Bitmap bitmap) {
            // Ignore
        }

        @Override
        public void clear() {
            // Ignore
        }
    };
}
