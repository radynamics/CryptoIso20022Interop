package com.radynamics.CryptoIso20022Interop.cryptoledger.xrpl.signing.xumm;

import com.radynamics.CryptoIso20022Interop.db.ConfigRepo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseStorage implements Storage {
    private final static Logger log = LogManager.getLogger(DatabaseStorage.class);

    @Override
    public String getAccessToken() {
        try (var repo = new ConfigRepo()) {
            return repo.getXummAccessToken();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void setAccessToken(String value) {
        try (var repo = new ConfigRepo()) {
            repo.setXummAccessToken(value);
            repo.commit();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}