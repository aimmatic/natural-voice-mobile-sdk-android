/*
Copyright 2018 The AimMatic Authors.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package flac.decode;

import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

/**
 * Flac buffer data, hold a binary data of flac
 * Some of the code were taking from https://github.com/nayuki/FLAC-library-Java
 */
public class FlacBuffer {

    // RICE Decoding
    // See https://xiph.org/flac/format.html#subframe_header with
    // RESIDUAL, RESIDUAL_CODING_METHOD_PARTITIONED_RICE and RICE_PARTITION

    private static final int RICE_DECODING_TABLE_BITS = 13;
    private static final int RICE_DECODING_TABLE_MASK = (1 << RICE_DECODING_TABLE_BITS) - 1;
    private static final byte[][] RICE_DECODING_CONSUMED_TABLES = new byte[31][1 << RICE_DECODING_TABLE_BITS];
    private static final int[][] RICE_DECODING_VALUE_TABLES = new int[31][1 << RICE_DECODING_TABLE_BITS];
    private static final int RICE_DECODING_CHUNK = 4;

    static {
        for (int param = 0; param < RICE_DECODING_CONSUMED_TABLES.length; param++) {
            byte[] consumed = RICE_DECODING_CONSUMED_TABLES[param];
            int[] values = RICE_DECODING_VALUE_TABLES[param];
            for (int i = 0; ; i++) {
                int numBits = (i >>> param) + 1 + param;
                if (numBits > RICE_DECODING_TABLE_BITS)
                    break;
                int bits = ((1 << param) | (i & ((1 << param) - 1)));
                int shift = RICE_DECODING_TABLE_BITS - numBits;
                for (int j = 0; j < (1 << shift); j++) {
                    consumed[(bits << shift) | j] = (byte) numBits;
                    values[(bits << shift) | j] = (i >>> 1) ^ -(i & 1);
                }
            }
            if (consumed[0] != 0)
                throw new AssertionError();
        }
    }

    byte[] byteBuffer;

    private long bufStartPos;
    private int bufferSize;
    private int bufferIndex;

    private long bitBuffer;
    private int bitBufferLen;

    private byte[] data;
    private int offset;

    /**
     * Create flac buffer from a binary
     *
     * @param buf flac binary data
     */
    public FlacBuffer(byte[] buf) {
        byteBuffer = new byte[4096];
        data = buf;
        seekTo(0);
    }

    /**
     * Change or move to a position
     *
     * @param position a new position in buffer
     */
    public void seekTo(long position) {
        offset = (int) position;
        bufStartPos = position;
        Arrays.fill(byteBuffer, (byte) 0);
        bufferSize = 0;
        bufferIndex = 0;
        bitBuffer = 0;
        bitBufferLen = 0;
    }

    /**
     * Get current position of FlacBuffer
     *
     * @return current position
     */
    public long getPosition() {
        return bufStartPos + bufferIndex - (bitBufferLen + 7) / 8;
    }

    /**
     * Get bit position of current reading buffer
     *
     * @return bit position
     */
    public int getBitPosition() {
        return (-bitBufferLen) & 7;
    }

    private void checkByteAligned() {
        if (bitBufferLen % 8 != 0)
            throw new InvalidFlacException();
    }

    int readUint(int n) throws IOException {
        if (n < 0 || n > 32)
            throw new IllegalArgumentException();
        while (bitBufferLen < n) {
            int b = readUnderlying();
            if (b == -1)
                throw new EOFException();
            bitBuffer = (bitBuffer << 8) | b;
            bitBufferLen += 8;
            assert 0 <= bitBufferLen && bitBufferLen <= 64;
        }
        int result = (int) (bitBuffer >>> (bitBufferLen - n));
        if (n != 32) {
            result &= (1 << n) - 1;
            assert (result >>> n) == 0;
        }
        bitBufferLen -= n;
        assert 0 <= bitBufferLen && bitBufferLen <= 64;
        return result;
    }


    int readInt(int n) throws IOException {
        int shift = 32 - n;
        return (readUint(n) << shift) >> shift;
    }

    void readRiceInts(int param, int start, int end) throws IOException {
        if (param < 0 || param > 31)
            throw new IllegalArgumentException();
        long unaryLimit = 1L << (53 - param);

        byte[] consumeTable = RICE_DECODING_CONSUMED_TABLES[param];
        while (true) {
            middle:
            while (start <= end - RICE_DECODING_CHUNK) {
                if (bitBufferLen < RICE_DECODING_CHUNK * RICE_DECODING_TABLE_BITS) {
                    if (bufferIndex <= bufferSize - 8) {
                        fillBitBuffer();
                    } else
                        break;
                }
                for (int i = 0; i < RICE_DECODING_CHUNK; i++, start++) {
                    // Fast decoder
                    int extractedBits = (int) (bitBuffer >>> (bitBufferLen - RICE_DECODING_TABLE_BITS)) & RICE_DECODING_TABLE_MASK;
                    int consumed = consumeTable[extractedBits];
                    if (consumed == 0)
                        break middle;
                    bitBufferLen -= consumed;
                }
            }

            // Slow decoder
            if (start >= end)
                break;
            long val = 0;
            while (readUint(1) == 0) {
                if (val >= unaryLimit) {
                    // At this point, the final decoded value would be so large that the result of the
                    // downstream restoreLpc() calculation would not fit in the output sample's bit depth -
                    // hence why we stop early and throw an exception. However, this check is conservative
                    // and doesn't catch all the cases where the post-LPC result wouldn't fit.
                    throw new InvalidFlacException();
                }
                val++;
            }
            readUint(param);
            start++;
        }
    }

    private void fillBitBuffer() {
        int i = bufferIndex;
        int n = Math.min((64 - bitBufferLen) >>> 3, bufferSize - i);
        byte[] b = byteBuffer;
        if (n > 0) {
            for (int j = 0; j < n; j++, i++)
                bitBuffer = (bitBuffer << 8) | (b[i] & 0xFF);
            bitBufferLen += n << 3;
        } else if (bitBufferLen <= 56) {
            int temp = readUnderlying();
            if (temp == -1)
                throw new InvalidFlacException();
            bitBuffer = (bitBuffer << 8) | temp;
            bitBufferLen += 8;
        }
        assert 8 <= bitBufferLen && bitBufferLen <= 64;
        bufferIndex += n;
    }

    public int readByte() throws IOException {
        checkByteAligned();
        if (bitBufferLen >= 8)
            return readUint(8);
        else {
            assert bitBufferLen == 0;
            return readUnderlying();
        }
    }

    private int readUnderlying() {
        if (bufferIndex >= bufferSize) {
            if (bufferSize == -1)
                return -1;
            bufStartPos += bufferSize;
            bufferSize = readUnderlying(byteBuffer, 0, byteBuffer.length);
            if (bufferSize <= 0)
                return -1;
            bufferIndex = 0;
        }
        assert bufferIndex < bufferSize;
        int temp = byteBuffer[bufferIndex] & 0xFF;
        bufferIndex++;
        return temp;
    }

    private int readUnderlying(byte[] buf, int offset, int count) {
        if (offset < 0 || offset > buf.length || count < 0 || count > buf.length - offset)
            throw new ArrayIndexOutOfBoundsException();
        int n = Math.min(data.length - this.offset, count);
        if (n == 0)
            return -1;
        System.arraycopy(data, this.offset, buf, offset, n);
        this.offset += n;
        return n;
    }


}
