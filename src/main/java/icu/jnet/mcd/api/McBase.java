package icu.jnet.mcd.api;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import icu.jnet.mcd.api.request.RefreshRequest;
import icu.jnet.mcd.api.request.Request;
import icu.jnet.mcd.api.response.status.Status;
import icu.jnet.mcd.model.Authorization;
import icu.jnet.mcd.api.response.Response;
import icu.jnet.mcd.api.response.LoginResponse;
import icu.jnet.mcd.model.SensorToken;
import icu.jnet.mcd.model.listener.StateChangeListener;
import icu.jnet.mcd.network.RequestManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.stream.Stream;

import static icu.jnet.mcd.network.RequestManager.addRequest;

class McBase {

    private final SensorToken sensorToken = new SensorToken();
    private static final HttpRequestFactory factory = new NetHttpTransport().createRequestFactory();
    final Authorization auth = new Authorization();

    String email;

    <T extends Response> T queryGet(Request request, Class<T> clazz)  {
        try {
            String url = request.getUrl();
            HttpRequest httpRequest = factory.buildGetRequest(new GenericUrl(url));
            return query(httpRequest, clazz);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return createInstance(clazz);
    }

    <T extends Response> T queryPost(Request request, Class<T> clazz) {
        try {
            String url = request.getUrl();
            HttpContent httpContent = request.getContent();
            HttpRequest httpRequest = factory.buildPostRequest(new GenericUrl(url), httpContent);
            return query(httpRequest, clazz);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return createInstance(clazz);
    }

    <T extends Response> T queryDelete(Request request, Class<T> clazz) {
        try {
            String url = request.getUrl();
            HttpRequest httpRequest = factory.buildDeleteRequest(new GenericUrl(url));
            return query(httpRequest, clazz);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return createInstance(clazz);
    }

    <T extends Response> T queryPut(Request request, Class<T> clazz) {
        try {
            String url = request.getUrl();
            HttpContent httpContent = request.getContent();
            HttpRequest httpRequest = factory.buildPutRequest(new GenericUrl(url), httpContent);
            return query(httpRequest, clazz);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return createInstance(clazz);
    }

    private <T extends Response> T query(HttpRequest request, Class<T> clazz) {
        Gson gson = new Gson();

        if(!needToken(request) || needToken(request) && sensorToken.isValid()) {
            try {
                request.setReadTimeout(8000);
                setRequestHeaders(request);
                addRequest(request);
                return gson.fromJson(request.execute().parseAsString(), clazz);
            } catch (HttpResponseException e) {
                try {
                    Response response = gson.fromJson(e.getContent(), Response.class);
                    if(response != null) {
                        if(response.getStatus().getErrors().stream()
                                .anyMatch(error -> error.getErrorType().equals("JWTTokenExpired"))) { // Authorization expired
                            if(loginRefresh()) {
                                return query(request, clazz);
                            }
                        }
                        return createInstance(clazz, response.getStatus());
                    }
                } catch (JsonSyntaxException e2) {
                    System.out.println(e.getContent());
                }
            } catch (IOException e) {
                //e.printStackTrace();
                sensorToken.setExpired();
            }
        }
        return createInstance(clazz);
    }

    private boolean loginRefresh() {
        LoginResponse login = queryPost(new RefreshRequest(auth.getRefreshToken()), LoginResponse.class);
        if(success(login)) {
            auth.updateRefreshToken(login.getRefreshToken());
            auth.updateAccessToken(login.getAccessToken());
            return true;
        }
        return false;
    }

    private void setRequestHeaders(HttpRequest request) {
        String token = !auth.getAccessToken().isEmpty()
                ? auth.getAccessToken()
                : "Basic NkRFVXlKT0thQm96OFFSRm00OXFxVklWUGowR1V6b0g6NWltaDZOS1UzdjVDVWlmVHZIUTdFeEY4ZXhrbWFOamI=";

        HttpHeaders headers = new HttpHeaders();
        headers.set("mcd-clientid", "6DEUyJOKaBoz8QRFm49qqVIVPj0GUzoH");
        headers.set("authorization", token);
        headers.set("accept-charset", "UTF-8");
        headers.set("content-type", request.getContent() != null ? request.getContent().getType() : "application/json;");
        headers.set("accept-language", "de-DE");
        headers.set("user-agent", "MCDSDK/19.0.60 (Android; 30; de-DE) GMA/7.7");
        headers.set("mcd-sourceapp", "GMA");
        headers.set("mcd-uuid", "ab65a26f-b02c-416d-a5e5-a32df5ba762d"); // Can not be fully random?

        if(needToken(request)) {
            headers.set("mcd-marketid", "DE");
            headers.set("x-acf-sensor-data", sensorToken.getToken());
        }
        request.setHeaders(headers);
    }

    private <T extends Response> T createInstance(Class<T> clazz) {
        return createInstance(clazz, new Status());
    }

    private <T extends Response> T createInstance(Class<T> clazz, Status status) {
        try {
            return clazz.getConstructor(Status.class).newInstance(status);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean needToken(HttpRequest request) {
        String url = request.getUrl().toString();
        return Stream.of("profile", "login", "registration", "activation").anyMatch(url::endsWith);
    }

    public boolean addChangeListener(StateChangeListener listener) {
        return auth.addChangeListener(listener) && sensorToken.addChangeListener(listener);
    }

    public SensorToken getSensorToken() {
        return sensorToken;
    }

    boolean success(Response response) {
        return response.getStatus().getType().equals("Success");
    }
}
