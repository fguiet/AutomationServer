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
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Romain Cambier
 */
public class JoinRequestPayload implements Message {

    private final PhyPayload phy;
    private final byte[] appEUI;
    private final byte[] devEUI;
    private final byte[] devNonce;

    protected JoinRequestPayload(PhyPayload _phy, ByteBuffer _raw) throws MalformedPacketException {
        phy = _phy;
        if (_raw.remaining() < 18) {
            throw new MalformedPacketException("could not read joinRequestPayload");
        }
        appEUI = new byte[8];
        devEUI = new byte[8];
        devNonce = new byte[2];
        _raw.get(appEUI);
        _raw.get(devEUI);
        _raw.get(devNonce);
    }

    public byte[] computeMic(byte[] _appKey) throws MalformedPacketException {
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
        binarize(body);

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

    @Override
    public int length() {
        return 18;
    }

    @Override
    public void binarize(ByteBuffer _bb) {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        _bb.put(appEUI);
        _bb.put(devEUI);
        _bb.put(devNonce);
    }

    public byte[] getAppEUI() {
        return appEUI;
    }

    public byte[] getDevEUI() {
        return devEUI;
    }

    public byte[] getDevNonce() {
        return devNonce;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private JoinRequestPayload(PhyPayload _phy, byte[] _appEUI, byte[] _devEUI, byte[] _devNonce) {
        if (_appEUI == null) {
            throw new IllegalArgumentException("Missing appEUI");
        }
        if (_appEUI.length != 8) {
            throw new IllegalArgumentException("Invalid appEUI");
        }
        if (_devEUI == null) {
            throw new IllegalArgumentException("Missing devEUI");
        }
        if (_devEUI.length != 8) {
            throw new IllegalArgumentException("Invalid devEUI");
        }
        if (_devNonce == null) {
            throw new IllegalArgumentException("Missing devNonce");
        }
        if (_devNonce.length != 2) {
            throw new IllegalArgumentException("Invalid devNonce");
        }
        phy = _phy;
        appEUI = _appEUI;
        devEUI = _devEUI;
        devNonce = _devNonce;
    }

    public static class Builder implements Message.Builder {

        private byte[] appEUI;
        private byte[] devEUI;
        private byte[] devNonce;
        private boolean used = false;

        private Builder() {

        }

        public Builder setAppEUI(byte[] _appEUI) {
            appEUI = _appEUI;
            return this;
        }

        public Builder setDevEUI(byte[] _devEUI) {
            devEUI = _devEUI;
            return this;
        }

        public Builder setDevNonce(byte[] _devNonce) {
            devNonce = _devNonce;
            return this;
        }

        @Override
        public Message build(PhyPayload _phy) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, MalformedPacketException {
            if (used) {
                throw new RuntimeException("This builder has already been used");
            }
            used = true;
            return new JoinRequestPayload(_phy, appEUI, devEUI, devNonce);
        }

    }

}
