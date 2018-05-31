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
 * Target STREAMINFO only
 * See <a href='https://xiph.org/flac/format.html#stream'>Stream Block</a>
 */
public final class StreamInfo {

    private int minBlockSize;
    private int maxBlockSize;
    private int minFrameSize;
    private int maxFrameSize;
    private int sampleRate;
    private int channels;
    private int bitPerSecond;
    // total sample can 0 for streaming or actual number of sample
    // Decoding in our repo is stream which mean total sample is always 0
    private long totalSample;
    // hash md5 is ignored as current stream does not know total samples

    public StreamInfo(byte[] b) {
        if (b.length != 34)
            throw new InvalidFlacException();
        try {
            FlacBuffer in = new FlacBuffer(b);
            minBlockSize = in.readUint(16);
            maxBlockSize = in.readUint(16);
            minFrameSize = in.readUint(24);
            maxFrameSize = in.readUint(24);
            if (minBlockSize < 16)
                throw new InvalidFlacException();
            if (maxBlockSize > 65535)
                throw new InvalidFlacException();
            if (maxBlockSize < minBlockSize)
                throw new InvalidFlacException();
            if (minFrameSize != 0 && maxFrameSize != 0 && maxFrameSize < minFrameSize)
                throw new InvalidFlacException();
            sampleRate = in.readUint(20);
            if (sampleRate == 0 || sampleRate > 655350)
                throw new InvalidFlacException();
            channels = in.readUint(3) + 1;
            bitPerSecond = in.readUint(5) + 1;
            totalSample = (long) in.readUint(18) << 18 | in.readUint(18);
            // skip hash md5 16byte
            in.seekTo(in.getPosition() + 16);
            // Skip closing the in-memory stream
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public int getMinBlockSize() {
        return minBlockSize;
    }

    public int getMaxBlockSize() {
        return maxBlockSize;
    }

    public int getMinFrameSize() {
        return minFrameSize;
    }

    public int getMaxFrameSize() {
        return maxFrameSize;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    public int getBitPerSecond() {
        return bitPerSecond;
    }

    public long getTotalSample() {
        return totalSample;
    }

    public void setTotalSample(long totalSample) {
        this.totalSample = totalSample;
    }

}
