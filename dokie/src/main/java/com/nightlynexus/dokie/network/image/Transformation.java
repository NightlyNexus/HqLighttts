package com.nightlynexus.dokie.network.image;

import android.graphics.Bitmap;

public interface Transformation {

    Bitmap transform(Bitmap source);

    String key();
}
