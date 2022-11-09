package org.jannsen.mcreverse.api.response;

import org.jannsen.mcreverse.api.response.status.Status;

import java.util.HashMap;

public class OptInResponse extends Response {

    private HashMap<String, String> response;

    public OptInResponse(Status status) {
        super(status);
    }

    public String getResponse() {
        return response != null && response.containsKey("message") ? response.get("message") : "";
    }
}
