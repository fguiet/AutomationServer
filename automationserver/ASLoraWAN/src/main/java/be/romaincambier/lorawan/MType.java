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

/**
 *
 * @author Romain Cambier
 */
public enum MType {

    JOIN_REQUEST((byte) 0x00, JoinRequestPayload.class, Direction.UP),
    JOIN_ACCEPT((byte) 0x01, JoinAcceptPayload.class, Direction.DOWN),
    UNCONF_DATA_UP((byte) 0x02, MACPayload.class, Direction.UP),
    UNCONF_DATA_DOWN((byte) 0x03, MACPayload.class, Direction.DOWN),
    CONF_DATA_UP((byte) 0x04, MACPayload.class, Direction.UP),
    CONF_DATA_DOWN((byte) 0x05, MACPayload.class, Direction.DOWN),
    RFU((byte) 0x06, null, null),
    PROPRIETARY((byte) 0x07, null, null);

    private MType(byte _value, Class<? extends Message> _mapper, Direction _direction) {
        value = _value;
        mapper = _mapper;
        direction = _direction;
    }

    private final byte value;
    private Class<? extends Message> mapper;
    private final Direction direction;

    public static MType from(byte _mhdr) throws MalformedPacketException {
        byte mType = (byte) ((_mhdr >> 5) & 0x07);
        for (MType v : values()) {
            if (v.value == mType) {
                return v;
            }
        }
        throw new MalformedPacketException("unknown mType");
    }

    public Direction getDirection() {
        return direction;
    }

    public byte value() {
        return value;
    }

    public Class<? extends Message> getMapper() {
        if (mapper == null) {
            throw new RuntimeException("Missing mapper for mType " + name());
        }
        return mapper;
    }

    public void setRfuPayloadMapper(Class<? extends Message> _handler) {
        RFU.mapper = _handler;
    }

    public void setProprietaryPayloadMapper(Class<? extends Message> _handler) {
        PROPRIETARY.mapper = _handler;
    }
}
