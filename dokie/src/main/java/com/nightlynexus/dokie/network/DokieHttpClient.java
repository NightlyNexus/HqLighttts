package com.nightlynexus.dokie.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.nightlynexus.dokie.network.image.CacheImage;
import com.nightlynexus.dokie.network.image.Transformation;
import com.nightlynexus.dokie.network.object.Converter;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;

public class DokieHttpClient extends OkHttpClient {

    @Nullable private CacheImage mCacheImage;
    @Nullable private Converter mConverter;

    public void setCacheImage(CacheImage cacheImage) {
        mCacheImage = cacheImage;
    }

    public void setConverter(Converter converter) {
        mConverter = converter;
    }

    @Override
    public Call newCall(Request request) {
        return super.newCall(request);
    }

    public <T> CallObject<T> newCallObject(Request request) {
        if (mConverter == null) {
            throw new AssertionError("May not make an object call with a null converter");
        }
        Call call = newCall(request);
        return new CallObject<T>(call, mConverter);
    }

    public static class CallObject<T> {

        @NonNull private final Call mCall;
        @NonNull private final Converter mConverter;

        CallObject(@NonNull Call call, @NonNull Converter converter) {
            mCall = call;
            mConverter = converter;
        }

        public T executeObject(@NonNull Type type) throws IOException {
            return mConverter.fromResponse(mCall.execute(), type);
        }

        public void enqueueObject(
                @NonNull Type type, @NonNull CallbackObject<T> callbackObject) {
            enqueueObject(type, callbackObject, null);
        }

        public void enqueueObject(
                @Nullable final Type type, @NonNull final CallbackObject<T> callbackObject,
                @Nullable Executor executor) {
            if (executor == null) {
                final Handler handler = new Handler();
                executor = new Executor() {

                    @Override
                    public void execute(@NonNull Runnable command) {
                        handler.post(command);
                    }
                };
            }
            final Executor executorFinal = executor;
            mCall.enqueue(new Callback() {

                @Override
                public void onFailure(final Request request, final IOException e) {
                    executorFinal.execute(new Runnable() {

                        @Override
                        public void run() {
                            callbackObject.onFailure(request, e);
                        }
                    });
                }

                @Override
                public void onResponse(final Response response) throws IOException {
                    final T object = mConverter.fromResponse(response, type);
                    executorFinal.execute(new Runnable() {

                        @Override
                        public void run() {
                            callbackObject.onResponse(response, object);
                        }
                    });
                }
            });
        }

        public void cancel() {
            mCall.cancel();
        }

        public boolean isCanceled() {
            return mCall.isCanceled();
        }
    }

    public interface CallbackObject<T> {

        void onFailure(Request request, IOException e);
        void onResponse(Response response, T object);
    }

    public CallImage newCallImage(Request request) {
        Call call = newCall(request);
        if (mCacheImage == null) {
            mCacheImage = CacheImage.NONE;
        }
        return new CallImage(call, request, mCacheImage);
    }

    public static class CallImage {

        @NonNull private final Call mCall;
        @NonNull private final Request mOriginalRequest;
        @NonNull private final CacheImage mCacheImage;

        protected CallImage(
                @NonNull Call call, @NonNull Request originalRequest,
                @NonNull CacheImage cacheImage) {
            mCall = call;
            mOriginalRequest = originalRequest;
            mCacheImage = cacheImage;
        }

        public Bitmap executeImage() throws IOException {
            return executeImage(null);
        }

        public Bitmap executeImage(Transformation transformation) throws IOException {
            String key = getKey(transformation);
            Bitmap bitmapCached = mCacheImage.get(key);
            if (bitmapCached != null) {
                return bitmapCached;
            }
            Response response = mCall.execute();
            return responseToBitmap(response, transformation, key);
        }

        public void enqueueImage(CallbackImage callbackImage) {
            enqueueImage(null, callbackImage);
        }

        public void enqueueImage(
                @Nullable Transformation transformation, @NonNull CallbackImage callbackImage) {
            enqueueImage(transformation, callbackImage, null);
        }

        public void enqueueImage(
                @Nullable final Transformation transformation,
                @NonNull final CallbackImage callbackImage, @Nullable Executor executor) {
            if (executor == null) {
                final Handler handler = new Handler();
                executor = new Executor() {

                    @Override
                    public void execute(@NonNull Runnable command) {
                        handler.post(command);
                    }
                };
            }
            final String key = getKey(transformation);
            final Bitmap bitmapCached = mCacheImage.get(key);
            if (bitmapCached != null) {
                executor.execute(new Runnable() {

                    @Override
                    public void run() {
                        callbackImage.onResponse(null, bitmapCached);
                    }
                });
                return;
            }
            final Executor executorFinal = executor;
            mCall.enqueue(new Callback() {

                @Override
                public void onFailure(final Request request, final IOException e) {
                    executorFinal.execute(new Runnable() {

                        @Override
                        public void run() {
                            callbackImage.onFailure(request, e);
                        }
                    });
                }

                @Override
                public void onResponse(final Response response) throws IOException {
                    final Bitmap bitmap = responseToBitmap(response, transformation, key);
                    executorFinal.execute(new Runnable() {

                        @Override
                        public void run() {
                            callbackImage.onResponse(response, bitmap);
                        }
                    });
                }
            });
        }

        public void cancel() {
            mCall.cancel();
        }

        public boolean isCanceled() {
            return mCall.isCanceled();
        }

        private String getKey(@Nullable Transformation transformation) {
            String key = mOriginalRequest.urlString();
            if (transformation != null) {
                key += "\n" + transformation.key();
            }
            return key;
        }

        private Bitmap responseToBitmap(
                Response response, Transformation transformation, String key) throws IOException {
            Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
            if (transformation != null) {
                bitmap = transformation.transform(bitmap);
            }
            mCacheImage.set(key, bitmap);
            return bitmap;
        }
    }

    public interface CallbackImage {

        void onFailure(Request request, IOException e);
        void onResponse(Response response, Bitmap bitmap);
    }
}
