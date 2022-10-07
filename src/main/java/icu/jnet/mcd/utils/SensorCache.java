package icu.jnet.mcd.utils;

import icu.jnet.mcd.api.McClient;
import icu.jnet.mcd.constants.Action;
import icu.jnet.mcd.utils.listener.ClientActionModel;
import icu.jnet.mcd.utils.listener.ClientStateListener;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SensorCache {

    private static final Queue<String> sensorQueue = new ConcurrentLinkedQueue<>();
    private final ClientActionModel actionModel;

    public SensorCache(ClientActionModel actionModel) {
        this.actionModel = actionModel;
    }

    public String getSensorToken() {
        if(sensorQueue.isEmpty()) {
            addToQueue(actionModel.notifyListener(Action.TOKEN_REQUIRED, String.class));
            verify();
        }
        return sensorQueue.poll();
    }

    private void addToQueue(String token) {
        int usage = !token.isEmpty() ? 15 : 2;
        for(int i = 0; i < usage; i++) {
            sensorQueue.add(token);
        }
    }

    private void verify() {
        McClient client = new McClient();
        client.addStateListener(new ClientStateListener() {
            @Override
            public String tokenRequired() {
                return getSensorToken();
            }
        });
        client.verifyNextToken();
    }
}
