package com.viglet.turing.utils;

import org.jetbrains.annotations.NotNull;

public class TurUtils {
    private TurUtils() {
        throw new IllegalStateException("Utility class");
    }
    @NotNull
    public static String getUrlTemplate(String serviceUrl, String id) {
        return serviceUrl.concat("/").concat(id);
    }
}
