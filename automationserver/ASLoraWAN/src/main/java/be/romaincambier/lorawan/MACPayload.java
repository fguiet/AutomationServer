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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 *
 * @author cambierr
 */
public class MACPayload implements Message, Binarizable {

    private final FHDR fhdr;
    private final byte fPort;
    private final FRMPayload payload;
    private final PhyPayload phy;

    protected MACPayload(PhyPayload _phy, ByteBuffer _raw) throws MalformedPacketException {
        phy = _phy;
        fhdr = new FHDR(this, _raw);
        fPort = _raw.get();
        payload = new FRMPayload(this, _raw);
    }

    @Override
    public void binarize(ByteBuffer _bb) throws MalformedPacketException {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        fhdr.binarize(_bb);
        if (payload != null) {
            _bb.put(fPort);
            payload.binarize(_bb);
        }
    }

    public FHDR getFhdr() {
        return fhdr;
    }

    public byte getfPort() {
        return fPort;
    }

    public FRMPayload getFRMPayload() {
        return payload;
    }

    public PhyPayload getPhyPayload() {
        return phy;
    }

    @Override
    public int length() {
        return fhdr.length() + ((payload == null) ? 0 : (1 + payload.length()));
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private MACPayload(PhyPayload _phy, FHDR.Builder _fhdr, Byte _fPort, FRMPayload.Builder _payload) throws MalformedPacketException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        phy = _phy;
        if (_fhdr == null) {
            throw new IllegalArgumentException("Missing fhdr");
        }
        if (_fPort == null) {
            throw new IllegalArgumentException("Missing fPort");
        }
        if (_payload == null) {
            throw new IllegalArgumentException("Missing payload");
        }
        fhdr = _fhdr.build(this);
        fPort = _fPort;
        payload = _payload.build(this);
    }

    public static class Builder {

        private FHDR.Builder fhdr;
        private Byte fPort;
        private FRMPayload.Builder payload;
        private boolean used = false;

        private Builder() {

        }

        public Builder setFhdr(FHDR.Builder _fhdr) {
            fhdr = _fhdr;
            return this;
        }

        public Builder setFport(Byte _fPort) {
            fPort = _fPort;
            return this;
        }

        public Builder setPayload(FRMPayload.Builder _payload) {
            payload = _payload;
            return this;
        }

        public MACPayload build(PhyPayload _phy) throws MalformedPacketException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
            if (used) {
                throw new RuntimeException("This builder has already been used");
            }
            used = true;
            return new MACPayload(_phy, fhdr, fPort, payload);
        }

    }

}
