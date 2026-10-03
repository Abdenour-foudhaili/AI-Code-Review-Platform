package com.aicodereview.github;

import com.aicodereview.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenCryptoTest {
    private TokenCrypto crypto(String key) {
        AppProperties props = new AppProperties();
        props.getSecurity().setTokenEncryptionKey(key);
        return new TokenCrypto(props);
    }

    @Test
    void roundTrip() {
        TokenCrypto c = crypto("a-long-enough-encryption-key");
        String enc = c.encrypt("gho_secret_token");
        assertFalse(enc.contains("gho_secret_token"));
        assertEquals("gho_secret_token", c.decrypt(enc));
    }

    @Test
    void sameInputGivesDifferentCiphertexts() {
        TokenCrypto c = crypto("a-long-enough-encryption-key");
        assertNotEquals(c.encrypt("token"), c.encrypt("token"));
    }

    @Test
    void wrongKeyCannotDecrypt() {
        String enc = crypto("key-one-key-one-key-one").encrypt("token");
        assertThrows(IllegalStateException.class, () -> crypto("key-two-key-two-key-two").decrypt(enc));
    }

    @Test
    void tamperedCiphertextIsRejected() {
        TokenCrypto c = crypto("a-long-enough-encryption-key");
        String enc = c.encrypt("token");
        char last = enc.charAt(enc.length() - 3);
        String tampered = enc.substring(0, enc.length() - 3) + (last == 'A' ? 'B' : 'A') + enc.substring(enc.length() - 2);
        assertThrows(IllegalStateException.class, () -> c.decrypt(tampered));
    }
}
