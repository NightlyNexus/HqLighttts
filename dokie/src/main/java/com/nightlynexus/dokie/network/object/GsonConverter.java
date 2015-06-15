package com.nightlynexus.dokie.network.object;

import com.google.gson.Gson;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

public class GsonConverter implements Converter {

    private final Gson gson;

    public GsonConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public <T> T fromResponse(Response response, Type type) throws IOException {
        Reader reader = response.body().charStream();
        try {
            return gson.fromJson(reader, type);
        } finally {
            reader.close();
        }
    }
}
