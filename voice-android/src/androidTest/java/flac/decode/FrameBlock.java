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

import java.io.IOException;

/**
 * Target check for only number of frame and frame block size
 * Some of the code were taking from https://github.com/nayuki/FLAC-library-Java
 * See <a href='https://xiph.org/flac/format.html#frame'>Frame Block</a>
 */
public final class FrameBlock {

    public int frameIndex;
    public long sampleOffset;
    public int channels;
    public int channelAssignment;
    public int blockSize;
    public int sampleRate;
    public int bitPerSecond;
    public int frameSize;

    public FrameBlock() {
        frameIndex = -1;
        sampleOffset = -1;
        channels = -1;
        channelAssignment = -1;
        blockSize = -1;
        sampleRate = -1;
        bitPerSecond = -1;
        frameSize = -1;
    }

    public static FrameBlock readFrame(FlacBuffer in) throws IOException {
        int temp = in.readByte();
        if (temp == -1)
            return null;
        FrameBlock result = new FrameBlock();
        result.frameSize = -1;

        // Read sync bits
        int sync = temp << 6 | in.readUint(6);  // Uint14
        if (sync != 0x3FFE)
            throw new InvalidFlacException();

        // Read various simple fields
        if (in.readUint(1) != 0)
            throw new InvalidFlacException();
        int blockStrategy = in.readUint(1);
        int blockSizeCode = in.readUint(4);
        int sampleRateCode = in.readUint(4);
        int chanAsgn = in.readUint(4);
        result.channelAssignment = chanAsgn;
        if (chanAsgn < 8)
            result.channels = chanAsgn + 1;
        else if (8 <= chanAsgn && chanAsgn <= 10)
            result.channels = 2;
        else
            throw new InvalidFlacException();
        result.bitPerSecond = decodeSampleDepth(in.readUint(3));
        if (in.readUint(1) != 0)
            throw new InvalidFlacException();

        // Read and check the frame/sample position field
        long position = readUtf8Integer(in);  // Reads 1 to 7 bytes
        if (blockStrategy == 0) {
            if ((position >>> 31) != 0)
                throw new InvalidFlacException();
            result.frameIndex = (int) position;
            result.sampleOffset = -1;
        } else if (blockStrategy == 1) {
            result.sampleOffset = position;
            result.frameIndex = -1;
        } else
            throw new AssertionError();

        // Read variable-length data for some fields
        result.blockSize = decodeBlockSize(blockSizeCode, in);  // Reads 0 to 2 bytes
        result.sampleRate = decodeSampleRate(sampleRateCode, in);  // Reads 0 to 2 bytes
        // skip CRC, libFlac should take care of it
        in.readUint(8);
        return result;
    }

    private static long readUtf8Integer(FlacBuffer in) throws IOException {
        int head = in.readUint(8);
        int n = Integer.numberOfLeadingZeros(~(head << 24));  // Number of leading 1s in the byte
        assert 0 <= n && n <= 8;
        if (n == 0)
            return head;
        else if (n == 1 || n == 8)
            throw new InvalidFlacException();
        else {
            long result = head & (0x7F >>> n);
            for (int i = 0; i < n - 1; i++) {
                int temp = in.readUint(8);
                if ((temp & 0xC0) != 0x80)
                    throw new InvalidFlacException();
                result = (result << 6) | (temp & 0x3F);
            }
            if ((result >>> 36) != 0)
                throw new AssertionError();
            return result;
        }
    }

    private static int decodeBlockSize(int code, FlacBuffer in) throws IOException {
        if ((code >>> 4) != 0)
            throw new IllegalArgumentException();
        switch (code) {
            case 0:
                throw new InvalidFlacException();
            case 6:
                return in.readUint(8) + 1;
            case 7:
                return in.readUint(16) + 1;
            default:
                int result = searchSecond(BLOCK_SIZE_CODES, code);
                if (result < 1 || result > 65536)
                    throw new AssertionError();
                return result;
        }
    }

    private static int decodeSampleRate(int code, FlacBuffer in) throws IOException {
        if ((code >>> 4) != 0)
            throw new IllegalArgumentException();
        switch (code) {
            case 0:
                return -1;  // Caller should obtain value from stream info metadata block
            case 12:
                return in.readUint(8);
            case 13:
                return in.readUint(16);
            case 14:
                return in.readUint(16) * 10;
            case 15:
                throw new InvalidFlacException();
            default:
                int result = searchSecond(SAMPLE_RATE_CODES, code);
                if (result < 1 || result > 655350)
                    throw new AssertionError();
                return result;
        }
    }

    private static int decodeSampleDepth(int code) {
        if ((code >>> 3) != 0)
            throw new IllegalArgumentException();
        else if (code == 0)
            return -1;  // Caller should obtain value from stream info metadata block
        else {
            int result = searchSecond(BIT_PER_SECOND_CODES, code);
            if (result == -1)
                throw new InvalidFlacException();
            if (result < 1 || result > 32)
                throw new AssertionError();
            return result;
        }
    }

    private static int searchSecond(int[][] table, int key) {
        for (int[] pair : table) {
            if (pair[1] == key)
                return pair[0];
        }
        return -1;
    }


    private static final int[][] BLOCK_SIZE_CODES = {
            {192, 1},
            {576, 2},
            {1152, 3},
            {2304, 4},
            {4608, 5},
            {256, 8},
            {512, 9},
            {1024, 10},
            {2048, 11},
            {4096, 12},
            {8192, 13},
            {16384, 14},
            {32768, 15},
    };

    private static final int[][] BIT_PER_SECOND_CODES = {
            {8, 1},
            {12, 2},
            {16, 4},
            {20, 5},
            {24, 6},
    };

    private static final int[][] SAMPLE_RATE_CODES = {
            {88200, 1},
            {176400, 2},
            {192000, 3},
            {8000, 4},
            {16000, 5},
            {22050, 6},
            {24000, 7},
            {32000, 8},
            {44100, 9},
            {48000, 10},
            {96000, 11},
    };

}
