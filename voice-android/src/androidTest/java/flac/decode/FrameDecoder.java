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
 * Some of the code were taking from https://github.com/nayuki/FLAC-library-Java
 */
public final class FrameDecoder {

    private static final int[][] FIXED_PREDICTION_COEFFICIENTS = {
            {},
            {1},
            {2, -1},
            {3, -3, 1},
            {4, -6, 4, -1},
    };

    public FlacBuffer in;
    public int expectedBitPerSecond;
    private int currentBlockSize;

    public FrameDecoder(FlacBuffer in, int bitPerSecond) {
        this.in = in;
        expectedBitPerSecond = bitPerSecond;
        currentBlockSize = -1;
    }

    public FrameBlock readFrame(int outOffset) throws IOException {
        // Parse the frame header to see if one is available
        long startByte = in.getPosition();
        FrameBlock meta = FrameBlock.readFrame(in);
        if (meta == null)  // EOF occurred cleanly
            return null;
        if (meta.bitPerSecond != -1 && meta.bitPerSecond != expectedBitPerSecond)
            throw new InvalidFlacException();

        // Check arguments and read frame header
        currentBlockSize = meta.blockSize;
        // Do the hard work
        decodeSubframes(expectedBitPerSecond, meta.channelAssignment);

        // Read padding and footer
        if (in.readUint((8 - in.getBitPosition()) % 8) != 0)
            throw new InvalidFlacException();
        // skip CRC calculation
        in.readUint(16);

        // Handle frame size and miscellaneous
        long frameSize = in.getPosition() - startByte;
        if (frameSize < 10)
            throw new AssertionError();
        if ((int) frameSize != frameSize)
            throw new InvalidFlacException();
        meta.frameSize = (int) frameSize;
        currentBlockSize = -1;
        return meta;
    }

    private void decodeSubframes(int sampleDepth, int chanAsgn) throws IOException {
        // Check arguments
        if (sampleDepth < 1 || sampleDepth > 32)
            throw new InvalidFlacException();
        if ((chanAsgn >>> 4) != 0)
            throw new InvalidFlacException();

        if (0 <= chanAsgn && chanAsgn <= 7) {
            // Handle 1 to 8 independently coded channels
            int numChannels = chanAsgn + 1;
            for (int ch = 0; ch < numChannels; ch++) {
                decodeSubframe(sampleDepth);
            }

        } else if (8 <= chanAsgn && chanAsgn <= 10) {
            // Handle one of the side-coded stereo methods
            decodeSubframe(sampleDepth + (chanAsgn == 9 ? 1 : 0));
            decodeSubframe(sampleDepth + (chanAsgn == 9 ? 0 : 1));
        } else throw new InvalidFlacException();
    }

    // Reads one subframe from the bit input stream, decodes it, and writes to result[0 : currentBlockSize].
    private void decodeSubframe(int sampleDepth) throws IOException {
        // Check arguments
        if (sampleDepth < 1 || sampleDepth > 33)
            throw new InvalidFlacException();
        // Read header fields
        if (in.readUint(1) != 0)
            throw new InvalidFlacException();
        int type = in.readUint(6);
        int shift = in.readUint(1);
        if (shift == 1) {
            while (in.readUint(1) == 0) {  // Unary coding
                if (shift >= sampleDepth)
                    throw new InvalidFlacException();
                shift++;
            }
        }
        if (!(0 <= shift && shift <= sampleDepth))
            throw new AssertionError();
        sampleDepth -= shift;

        // Read sample data based on type
        if (type == 0)  // Constant coding
            in.readInt(sampleDepth);
        else if (type == 1) {  // Verbatim coding
            for (int i = 0; i < currentBlockSize; i++)
                in.readInt(sampleDepth);
        } else if (8 <= type && type <= 12)
            decodeFixedPredictionSubframe(type - 8, sampleDepth);
        else if (32 <= type && type <= 63)
            decodeLinearPredictiveCodingSubframe(type - 31, sampleDepth);
        else
            throw new InvalidFlacException();
    }


    // Reads from the input stream, performs computation, and writes to result[0 : currentBlockSize].
    private void decodeFixedPredictionSubframe(int predOrder, int sampleDepth) throws IOException {
        // Check arguments
        if (sampleDepth < 1 || sampleDepth > 33)
            throw new IllegalArgumentException();
        if (predOrder < 0 || predOrder >= FIXED_PREDICTION_COEFFICIENTS.length)
            throw new IllegalArgumentException();
        if (predOrder > currentBlockSize)
            throw new InvalidFlacException();

        // Read and compute various values
        for (int i = 0; i < predOrder; i++)  // Non-Rice-coded warm-up samples
            in.readInt(sampleDepth);
        readResiduals(predOrder);
    }

    // Reads from the input stream, performs computation, and writes to result[0 : currentBlockSize].
    private void decodeLinearPredictiveCodingSubframe(int lpcOrder, int sampleDepth) throws IOException {
        // Check arguments
        if (sampleDepth < 1 || sampleDepth > 33)
            throw new IllegalArgumentException();
        if (lpcOrder < 1 || lpcOrder > 32)
            throw new IllegalArgumentException();
        if (lpcOrder > currentBlockSize)
            throw new InvalidFlacException();

        // Read non-Rice-coded warm-up samples
        for (int i = 0; i < lpcOrder; i++)
            in.readInt(sampleDepth);

        // Read parameters for the LPC coefficients
        int precision = in.readUint(4) + 1;
        if (precision == 16)
            throw new InvalidFlacException();
        int shift = in.readInt(5);
        if (shift < 0)
            throw new InvalidFlacException();

        // Read the coefficients themselves
        for (int i = 0; i < lpcOrder; i++)
            in.readInt(precision);

        // Perform the main LPC decoding
        readResiduals(lpcOrder);
    }

    // Reads metadata and Rice-coded numbers from the input stream, storing them in result[warmup : currentBlockSize].
    // The stored numbers are guaranteed to fit in a signed int53 - see the explanation in restoreLpc().
    private void readResiduals(int warmup) throws IOException {
        // Check and handle arguments
        if (warmup < 0 || warmup > currentBlockSize)
            throw new IllegalArgumentException();

        int method = in.readUint(2);
        if (method >= 2)
            throw new InvalidFlacException();
        assert method == 0 || method == 1;
        int paramBits = method == 0 ? 4 : 5;
        int escapeParam = method == 0 ? 0xF : 0x1F;

        int partitionOrder = in.readUint(4);
        int numPartitions = 1 << partitionOrder;
        if (currentBlockSize % numPartitions != 0)
            throw new InvalidFlacException();
        for (int inc = currentBlockSize >>> partitionOrder, partEnd = inc, resultIndex = warmup;
             partEnd <= currentBlockSize; partEnd += inc) {
            int param = in.readUint(paramBits);
            if (param == escapeParam) {
                int numBits = in.readUint(5);
                for (; resultIndex < partEnd; resultIndex++)
                    in.readInt(numBits);
            } else {
                in.readRiceInts(param, resultIndex, partEnd);
                resultIndex = partEnd;
            }
        }
    }

}
