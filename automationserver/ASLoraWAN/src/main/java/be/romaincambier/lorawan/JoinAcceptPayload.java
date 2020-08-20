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
public class JoinAcceptPayload implements Message {

    private final PhyPayload phy;
    private final byte[] encryptedPayload;
    private ClearPayload payload;

    protected JoinAcceptPayload(PhyPayload _phy, ByteBuffer _raw) throws MalformedPacketException {
        phy = _phy;
        if (_raw.remaining() < 12) {
            throw new MalformedPacketException("could not read joinAcceptPayload");
        }
        encryptedPayload = new byte[_raw.remaining() - 4];
        _raw.get(encryptedPayload);
    }

    @Override
    public int length() {
        return encryptedPayload.length;
    }

    @Override
    public void binarize(ByteBuffer _bb) {
        _bb.put(encryptedPayload);
    }

    public ClearPayload getClearPayload(byte[] _appKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if (payload == null) {
            if (_appKey == null) {
                throw new RuntimeException("Missing appKey");
            }
            if (_appKey.length != 16) {
                throw new IllegalArgumentException("Invalid appKey");
            }
            ByteBuffer a = ByteBuffer.allocate(4 + length());
            a.order(ByteOrder.LITTLE_ENDIAN);
            a.put(encryptedPayload);
            a.put(phy.getMic());
            Key aesKey = new SecretKeySpec(_appKey, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] s = cipher.doFinal(a.array());
            payload = new ClearPayload(s);
        }
        return payload;
    }

    private byte[] getEncryptedPayload(byte[] _appKey) throws MalformedPacketException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if (_appKey == null) {
            throw new RuntimeException("Missing appKey");
        }
        if (_appKey.length != 16) {
            throw new IllegalArgumentException("Invalid appKey");
        }
        ByteBuffer a = ByteBuffer.allocate(4 + payload.length());
        a.order(ByteOrder.LITTLE_ENDIAN);
        payload.binarize(a);
        a.put(computeMic(_appKey));
        Key aesKey = new SecretKeySpec(_appKey, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        return cipher.doFinal(a.array());
    }

    public byte[] computeMic(byte[] _appKey) throws MalformedPacketException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if (_appKey == null) {
            throw new RuntimeException("Missing appKey");
        }
        if (_appKey.length != 16) {
            throw new IllegalArgumentException("Invalid appKey");
        }
        //size = mhdr + length()
        ByteBuffer body = ByteBuffer.allocate(1 + length());
        body.order(ByteOrder.LITTLE_ENDIAN);
        phy.getMHDR().binarize(body);
        getClearPayload(_appKey).binarize(body);

        AesCmac aesCmac;
        try {
            aesCmac = new AesCmac();
            aesCmac.init(new SecretKeySpec(_appKey, "AES"));
            aesCmac.updateBlock(body.array());
            return Arrays.copyOfRange(aesCmac.doFinal(), 0, 4);
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException ex) {
            throw new RuntimeException("Could not compute AesCmac", ex);
        }
    }

    public static class ClearPayload implements Binarizable {

        private final byte[] appNonce;
        private final byte[] netId;
        private final byte[] devAddr;
        private final byte dlSettings;
        private final byte rxDelay;
        private final byte[] cfList;

        private ClearPayload(byte[] _raw) {
            ByteBuffer bb = ByteBuffer.wrap(_raw);
            appNonce = new byte[3];
            netId = new byte[3];
            devAddr = new byte[4];
            bb.get(appNonce);
            bb.get(netId);
            bb.get(devAddr);
            dlSettings = bb.get();
            rxDelay = bb.get();
            cfList = new byte[bb.remaining() - 4];
            bb.get(cfList);
        }

        public byte[] getAppNonce() {
            return appNonce;
        }

        public byte[] getNetId() {
            return netId;
        }

        public byte[] getDevAddr() {
            return devAddr;
        }

        public byte getDlSettings() {
            return dlSettings;
        }

        public byte getRxDelay() {
            return rxDelay;
        }

        public byte[] getCfList() {
            return cfList;
        }

        @Override
        public void binarize(ByteBuffer _bb) {
            _bb.put(appNonce);
            _bb.put(netId);
            _bb.put(devAddr);
            _bb.put(dlSettings);
            _bb.put(rxDelay);
            _bb.put(cfList);
        }

        @Override
        public int length() {
            return appNonce.length + netId.length + devAddr.length + 1 + 1 + cfList.length;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        private ClearPayload(byte[] _appNonce, byte[] _netId, byte[] _devAddr, Byte _dlSettings, Byte _rxDelay, byte[] _cfList) {
            if (_appNonce == null) {
                throw new IllegalArgumentException("Missing appNonce");
            }
            if (_appNonce.length != 3) {
                throw new IllegalArgumentException("Invalid appNonce");
            }
            if (_netId == null) {
                throw new IllegalArgumentException("Missing netId");
            }
            if (_netId.length != 3) {
                throw new IllegalArgumentException("Invalid netId");
            }
            if (_devAddr == null) {
                throw new IllegalArgumentException("Missing devAddr");
            }
            if (_devAddr.length != 4) {
                throw new IllegalArgumentException("Invalid devAddr");
            }
            if (_cfList == null) {
                throw new IllegalArgumentException("Missing cfList");
            }
            if (_dlSettings == null) {
                throw new IllegalArgumentException("Missing dlSettings");
            }
            if (_rxDelay == null) {
                throw new IllegalArgumentException("Missing rxDelay");
            }
            appNonce = _appNonce;
            netId = _netId;
            devAddr = _devAddr;
            dlSettings = _dlSettings;
            rxDelay = _rxDelay;
            cfList = _cfList;
        }

        public static class Builder {

            private byte[] appNonce;
            private byte[] netId;
            private byte[] devAddr;
            private Byte dlSettings;
            private Byte rxDelay;
            private byte[] cfList;

            private boolean used = false;

            private Builder() {

            }

            public Builder setAppNonce(byte[] _appNonce) {
                appNonce = _appNonce;
                return this;
            }

            public Builder setNetId(byte[] _netId) {
                netId = _netId;
                return this;
            }

            public Builder setDevAddr(byte[] _devAddr) {
                devAddr = _devAddr;
                return this;
            }

            public Builder setDlSettings(byte _dlSettings) {
                dlSettings = _dlSettings;
                return this;
            }

            public Builder setRxDelay(byte _rxDelay) {
                rxDelay = _rxDelay;
                return this;
            }

            public Builder setCfList(byte[] _cfList) {
                cfList = _cfList;
                return this;
            }

            protected ClearPayload build() {
                if (used) {
                    throw new RuntimeException("This builder has already been used");
                }
                used = true;
                return new ClearPayload(appNonce, netId, devAddr, dlSettings, rxDelay, cfList);
            }

        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private JoinAcceptPayload(PhyPayload _phy, ClearPayload.Builder _payload, byte[] _appKey) throws MalformedPacketException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if (_payload == null) {
            throw new IllegalArgumentException("Missing payload");
        }
        phy = _phy;
        payload = _payload.build();
        encryptedPayload = getEncryptedPayload(_appKey);
    }

    public static class Builder implements Message.Builder {

        private byte[] appKey;
        private ClearPayload.Builder payload;
        private boolean used = false;

        private Builder() {

        }

        public Builder setPayload(ClearPayload.Builder _payload) {
            payload = _payload;
            return this;
        }

        public Builder setAppKey(byte[] _appKey) {
            appKey = _appKey;
            return this;
        }

        @Override
        public JoinAcceptPayload build(PhyPayload _phy) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, MalformedPacketException {
            if (used) {
                throw new RuntimeException("This builder has already been used");
            }
            used = true;
            return new JoinAcceptPayload(_phy, payload, appKey);
        }

    }

}
