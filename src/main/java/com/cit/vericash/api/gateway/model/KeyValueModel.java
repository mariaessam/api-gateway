package com.cit.vericash.api.gateway.model;

public class KeyValueModel {
    private String key;
    private String value;
    private long expiry;

    public long getExpiry() {
        return expiry;
    }
    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
