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
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 *
 * @author Romain Cambier
 */
public class PhyPayload implements Binarizable {

    private final MHDR mhdr;
    private final Message message;
    private final byte[] mic;

    private PhyPayload(ByteBuffer _raw) throws MalformedPacketException {
        _raw.order(ByteOrder.LITTLE_ENDIAN);
        if (_raw.remaining() < 1) {
            throw new MalformedPacketException("can not read mhdr");
        }
        mhdr = new MHDR(this, _raw);
        Class<? extends Message> mapper = mhdr.getMType().getMapper();
        try {
            message = mapper.getDeclaredConstructor(PhyPayload.class, ByteBuffer.class).newInstance(this, _raw);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException("Could not create Message", ex);
        }
        if (_raw.remaining() < 4) {
            throw new MalformedPacketException("can not read mic");
        }
        mic = new byte[4];
        _raw.get(mic);
    }

    public static PhyPayload parse(ByteBuffer _raw) throws MalformedPacketException {
        return new PhyPayload(_raw);
    }

    @Override
    public void binarize(ByteBuffer _bb) throws MalformedPacketException {
        _bb.order(ByteOrder.LITTLE_ENDIAN);
        mhdr.binarize(_bb);
        message.binarize(_bb);
        _bb.put(mic);
    }

    public MHDR getMHDR() {
        return mhdr;
    }

    public Message getMessage() {
        return message;
    }

    public byte[] getMic() {
        return mic;
    }

    @Override
    public int length() {
        return 1 + message.length() + 4;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private PhyPayload(MHDR.Builder _mhdr, MACPayload.Builder _macPayload) throws MalformedPacketException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if (_mhdr == null) {
            throw new IllegalArgumentException("Missing mhdr");
        }
        if (_macPayload == null) {
            throw new IllegalArgumentException("Missing macPayload");
        }
        mhdr = _mhdr.build(this);
        message = _macPayload.build(this);
        /**
         * @todo: Check mType here ?
         */
        /**
         * @todo: mic ???
         */
        mic = null;
    }

    public static class Builder {

        private MHDR.Builder mhdr;
        private MACPayload.Builder macPayload;
        private boolean used = false;

        private Builder() {

        }

        public Builder setMhdr(MHDR.Builder _mhdr) {
            mhdr = _mhdr;
            return this;
        }

        public Builder setMacPayload(MACPayload.Builder _macPayload) {
            macPayload = _macPayload;
            return this;
        }

        public PhyPayload build() throws MalformedPacketException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
            if (used) {
                throw new RuntimeException("This builder has already been used");
            }
            used = true;
            return new PhyPayload(mhdr, macPayload);
        }

    }

}
