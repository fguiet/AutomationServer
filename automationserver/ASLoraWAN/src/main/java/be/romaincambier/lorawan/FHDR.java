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

/**
 *
 * @author Romain Cambier
 */
public class FHDR implements Binarizable {

    private final byte[] devAddr;
    private final byte fCtrl;
    private final short fCnt;
    private final byte[] fOpts;
    private final MACPayload macPayload;

    protected FHDR(MACPayload _macPayload, ByteBuffer _raw) throws MalformedPacketException {
        macPayload = _macPayload;
        if (_raw.remaining() < 7) {
            throw new MalformedPacketException("can not read fhdr");
        }
        devAddr = new byte[4];
        _raw.get(devAddr);
        fCtrl = _raw.get();
        fCnt = _raw.getShort();
        fOpts = new byte[fCtrl & 0xf];
        if (_raw.remaining() < fOpts.length) {
            throw new MalformedPacketException("can not read fOpts");
        }
        _raw.get(fOpts);
    }

    @Override
    public void binarize(ByteBuffer _bb) {
        _bb.put(devAddr);
        _bb.put(fCtrl);
        _bb.putShort(fCnt);
        _bb.put(fOpts);
    }

    public byte[] getDevAddr() {
        return devAddr;
    }

    public byte getfCtrl() {
        return fCtrl;
    }

    public short getfCnt() {
        return fCnt;
    }

    public byte[] getfOpts() {
        return fOpts;
    }

    @Override
    public int length() {
        return devAddr.length + 1 + 2 + fOpts.length;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private FHDR(MACPayload _macPayload, byte[] _devAddr, Byte _fCtrl, Short _fCnt, byte[] _fOpts) {
        if (_devAddr == null) {
            throw new IllegalArgumentException("Missing devAddr");
        }
        if (_devAddr.length != 4) {
            throw new IllegalArgumentException("Invalid devAddr");
        }
        if (_fCtrl == null) {
            throw new IllegalArgumentException("Missing fCtrl");
        }
        if (_fCnt == null) {
            throw new IllegalArgumentException("Missing fCnt");
        }
        if (_fOpts == null) {
            throw new IllegalArgumentException("Missing fOpts");
        }
        if (_fOpts.length != (_fCtrl & 0xf)) {
            throw new IllegalArgumentException("Invalid fOpts");
        }
        macPayload = _macPayload;
        devAddr = _devAddr;
        fCtrl = _fCtrl;
        fCnt = _fCnt;
        fOpts = _fOpts;
    }

    public static class Builder {

        private byte[] devAddr;
        private byte fCtrl;
        private short fCnt;
        private byte[] fOpts;
        private boolean used = false;

        private Builder() {

        }

        public Builder setDevAddr(byte[] _devAddr) {
            devAddr = _devAddr;
            return this;
        }

        public Builder setFCtrl(Byte _fCtrl) {
            fCtrl = _fCtrl;
            return this;
        }

        public Builder setFCnt(Short _fCnt) {
            fCnt = _fCnt;
            return this;
        }

        public Builder setFOpts(byte[] _fOpts) {
            fOpts = _fOpts;
            return this;
        }

        protected FHDR build(MACPayload _macPayload) throws MalformedPacketException {
            if (used) {
                throw new RuntimeException("This builder has already been used");
            }
            used = true;
            return new FHDR(_macPayload, devAddr, fCtrl, fCnt, fOpts);
        }

    }
}
