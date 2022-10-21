package org.jannsen.mcd.api.request;

public class BonusPointsRequest extends Request {

    @Override
    public String getUrl() {
        return "https://eu-prod.api.mcd.com/exp/v3/loyalty/customer/bonus/points";
    }
}
