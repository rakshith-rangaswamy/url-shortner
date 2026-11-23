package com.urlshortener;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/** Values Maven writes into build-info.properties, so versions are declared only in pom.xml. */
final class BuildInfo {

    private static final Properties PROPS = load();

    private BuildInfo() {
    }

    static String swaggerUiVersion() {
        return require("swagger-ui.version");
    }

    private static String require(String key) {
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank() || value.startsWith("${")) {
            throw new IllegalStateException("build-info.properties is missing " + key);
        }
        return value;
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = BuildInfo.class.getResourceAsStream("/build-info.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return props;
    }
}
