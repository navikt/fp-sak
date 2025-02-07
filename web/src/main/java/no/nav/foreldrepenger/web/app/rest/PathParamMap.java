package no.nav.foreldrepenger.web.app.rest;

import java.util.HashMap;
import java.util.UUID;

public class PathParamMap extends HashMap<String, String> {

    public PathParamMap add(String key, String value) {
        this.put(key, value);
        return this;
    }

    public PathParamMap add(String key, Long value) {
        return add(key, value.toString());
    }

    public PathParamMap add(String key, UUID value) {
        return add(key, value.toString());
    }
}

