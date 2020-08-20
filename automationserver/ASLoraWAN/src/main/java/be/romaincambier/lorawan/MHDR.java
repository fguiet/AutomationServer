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
public class MHDR implements Binarizable {

    private final byte mhdr;
    private final PhyPayload phy;

    protected MHDR(PhyPayload _phy, ByteBuffer _raw) {
        phy = _phy;
        mhdr = _raw.get();
    }

    @Override
    public void binarize(ByteBuffer _bb) throws MalformedPacketException {
        _bb.put(mhdr);
    }

    @Override
    public int length() {
        return 1;
    }

    public MType getMType() throws MalformedPacketException {
        return MType.from(mhdr);
    }

    public MajorVersion getMajorVersion() throws MalformedPacketException {
        return MajorVersion.from(mhdr);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    private MHDR(PhyPayload _phy, MType _mType, MajorVersion _majorVersion) {
        if (_mType == null) {
            throw new IllegalArgumentException("Missing mType");
        }
        if (_majorVersion == null) {
            throw new IllegalArgumentException("Missing majorVersion");
        }
        phy = _phy;
        byte tempMhdr = 0;
        tempMhdr += _mType.value() << 5;
        tempMhdr += _majorVersion.value();
        mhdr = tempMhdr;
    }

    public static class Builder {

        private MType mType;
        private MajorVersion majorVersion;
        private boolean used = false;

        private Builder() {

        }

        public Builder setMType(MType _mType) {
            mType = _mType;
            return this;
        }

        public Builder setMajorVersion(MajorVersion _majorVersion) {
            majorVersion = _majorVersion;
            return this;
        }

        protected MHDR build(PhyPayload _phy) throws MalformedPacketException {
            if (used) {
                throw new RuntimeException("This builder has already been used");
            }
            used = true;
            return new MHDR(_phy, mType, majorVersion);
        }

    }

}
