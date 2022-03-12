package com.radynamics.CryptoIso20022Interop.cryptoledger;

import okhttp3.HttpUrl;

public class NetworkInfo {
    private Network type;
    private HttpUrl url;

    public NetworkInfo(Network type, HttpUrl url) {
        this.type = type;
        this.url = url;
    }

    public Network getType() {
        return type;
    }

    public HttpUrl getUrl() {
        return url;
    }

    public boolean matches(String text) {
        if (type == Network.Live && "main".equalsIgnoreCase(text)) {
            return true;
        }
        if (type == Network.Test && "test".equalsIgnoreCase(text)) {
            return true;
        }
        return false;
    }
}
