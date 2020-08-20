/*
 * The MIT License
 *
 * Copyright 2016 Romain Cambier <me@romaincambier.be>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package be.romaincambier.lorawan;

import be.romaincambier.lorawan.exceptions.MalformedPacketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Romain Cambier
 */
public class FRMPayload implements Binarizable {

    private final MACPayload mac;
    private final byte[] payload;

    protected FRMPayload(MACPayload _mac, ByteBuffer _raw) {
        mac = _mac;
        payload = new byte[_raw.remaining() - 4];
        _raw.get(payload);
    }

    public byte[] computeMic(byte[] _nwkSKey) throws MalformedPacketException {
        if (_nwkSKey == null) {
            throw new IllegalArgumentException("Missing nwkSKey");
        }
        if (_nwkSKey.length != 16) {
            throw new IllegalArgumentException("Invalid nwkSKey");
        }
        //size = mhdr + MacPayload + 16 (B0)
        ByteBuffer body = ByteBuffer.allocate(1 + mac.length() + 16);
        body.order(ByteOrder.LITTLE_ENDIAN);

        body.put((byte) 0x49);
        body.put(new byte[]{0x00, 0x00, 0x00, 0x00});
        body.put(mac.getPhyPayload().getMHDR().getMType().getDirection().value());
        body.put(mac.getFhdr().getDevAddr());
        body.putInt(mac.getFhdr().getfCnt());
        body.put((byte) 0x00);
        body.put((byte) (1 + mac.length()));

        mac.getPhyPayload().getMHDR().binarize(body);
        mac.binarize(body);

        AesCmac aesCmac;
        try {
            aesCmac = new AesCmac();
            aesCmac.init(new SecretKeySpec(_nwkSKey, "AES"));
            aesCmac.updateBlock(body.array());
            return Arrays.copyOfRange(aesCmac.doFinal(), 0, 4);
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
            throw new RuntimeException("Could not compute AesCmac", ex);
        }
    }

    @Override
    public void binarize(ByteBuffer _bb) {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        _bb.put(payload);
    }

    public byte[] getClearPayLoad(byte[] _nwkSKey, byte[] _appSKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, MalformedPacketException {
        byte[] key;
        if (mac.getfPort() == 0) {
            if (_nwkSKey == null) {
                throw new IllegalArgumentException("Missing nwkSKey");
            }
            if (_nwkSKey.length != 16) {
                throw new IllegalArgumentException("Invalid nwkSKey");
            }
            key = _nwkSKey;
        } else {
            if (_appSKey == null) {
                throw new IllegalArgumentException("Missing appSKey");
            }
            if (_appSKey.length != 16) {
                throw new IllegalArgumentException("Invalid appSKey");
            }
            key = _appSKey;
        }
        int k = (int) Math.ceil(payload.length / 16.0);
        ByteBuffer a = ByteBuffer.allocate(16 * k);
        a.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 1; i <= k; i++) {
            a.put((byte) 0x01);
            a.put(new byte[]{0x00, 0x00, 0x00, 0x00});
            a.put(mac.getPhyPayload().getMHDR().getMType().getDirection().value());
            a.put(mac.getFhdr().getDevAddr());
            a.putInt(mac.getFhdr().getfCnt());
            a.put((byte) 0x00);
            a.put((byte) i);
        }
        Key aesKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] s = cipher.doFinal(a.array());
        byte[] paddedPayload = new byte[16 * k];
        System.arraycopy(payload, 0, paddedPayload, 0, payload.length);
        byte[] plainPayload = new byte[payload.length];
        for (int i = 0; i < payload.length; i++) {
            plainPayload[i] = (byte) (s[i] ^ paddedPayload[i]);
        }
        return plainPayload;
    }

    private byte[] encryptPayload(byte[] _data, byte[] _nwkSKey, byte[] _appSKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, MalformedPacketException {
        byte[] key;
        if (mac.getfPort() == 0) {
            if (_nwkSKey == null) {
                throw new IllegalArgumentException("Missing nwkSKey");
            }
            if (_nwkSKey.length != 16) {
                throw new IllegalArgumentException("Invalid nwkSKey");
            }
            key = _nwkSKey;
        } else {
            if (_appSKey == null) {
                throw new IllegalArgumentException("Missing appSKey");
            }
            if (_appSKey.length != 16) {
                throw new IllegalArgumentException("Invalid appSKey");
            }
            key = _appSKey;
        }
        int k = (int) Math.ceil(_data.length / 16.0);
        ByteBuffer a = ByteBuffer.allocate(16 * k);
        a.order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 1; i <= k; i++) {
            a.put((byte) 0x01);
            a.put(new byte[]{0x00, 0x00, 0x00, 0x00});
            a.put(mac.getPhyPayload().getMHDR().getMType().getDirection().value());
            a.put(mac.getFhdr().getDevAddr());
            a.putInt(mac.getFhdr().getfCnt());
            a.put((byte) 0x00);
            a.put((byte) i);
        }
        Key aesKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] s = cipher.doFinal(a.array());
        byte[] paddedPayload = new byte[16 * k];
        System.arraycopy(_data, 0, paddedPayload, 0, _data.length);
        byte[] output = new byte[_data.length];
        for (int i = 0; i < _data.length; i++) {
            output[i] = (byte) (s[i] ^ paddedPayload[i]);
        }
        return output;
    }

    public MACPayload getMac() {
        return mac;
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public int length() {
        return payload.length;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private FRMPayload(MACPayload _macPayload, byte[] _payload, byte[] _nwkSKey, byte[] _appSKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, MalformedPacketException {
        if (_payload == null) {
            throw new IllegalArgumentException("Missing payload");
        }
        mac = _macPayload;
        payload = encryptPayload(_payload, _nwkSKey, _appSKey);
    }

    public static class Builder {

        private byte[] payload;
        private byte[] nwkSKey;
        private byte[] appSKey;
        private boolean used;

        private Builder() {

        }

        public Builder setPayload(byte[] _payload) {
            payload = _payload;
            return this;
        }

        public Builder setNwkSKey(byte[] _nwkSKey) {
            nwkSKey = _nwkSKey;
            return this;
        }

        public Builder setAppSKey(byte[] _appSKey) {
            appSKey = _appSKey;
            return this;
        }

        //@Override
        public FRMPayload build(MACPayload _macPayload) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, MalformedPacketException {
            if (used) {
                throw new RuntimeException("This builder has already been used");
            }
            used = true;
            return new FRMPayload(_macPayload, payload, nwkSKey, appSKey);
        }

    }

}
