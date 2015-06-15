package com.nightlynexus.dokie.network.object;

import com.squareup.okhttp.Response;

import java.io.IOException;
import java.lang.reflect.Type;

public interface Converter {

    <T> T fromResponse(Response response, Type type) throws IOException;
}
