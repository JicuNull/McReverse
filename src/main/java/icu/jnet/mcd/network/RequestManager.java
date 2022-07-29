package icu.jnet.mcd.network;

import com.google.api.client.http.HttpRequest;
import icu.jnet.mcd.helper.Utils;

public class RequestManager {

    private final AutoRemoveQueue<HttpRequest> queue = new AutoRemoveQueue<>();

    private static RequestManager instance;

    public static RequestManager getInstance() {
        if(instance == null) {
            instance = new RequestManager();
        }
        return instance;
    }

    public void addRequest(HttpRequest request) {
        queue.add(request);
        while (queue.contains(request)) {
            Utils.waitMill(100);
        }
    }
}
