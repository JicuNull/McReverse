package org.jannsen.mcreverse.api.entity.stream;

public class OfferImage extends StreamData {

    public OfferImage(String name, String data) {
        super(name, data);
    }

    public OfferImage(String name, String data, long createdTime) {
        super(name, data, createdTime);
    }
}
