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
import java.util.Arrays;

/**
 * Flac metadata decoder.
 */
public class FlacDecoder {

    private FlacBuffer buffer;
    // We only interest in stream header information (STREAMINFO)
    // other header does not concern streaming voice recognition
    // See https://xiph.org/flac/format.html#stream
    private StreamInfo streamInfo;
    // a position of byte that begin the flac frame data
    private long startFramePosition;
    // Frame decoder
    private FrameDecoder frameDecoder;

    /**
     * Create flac decoder from a complete flac binary data
     *
     * @param buf flac binary data
     * @throws IOException if buf is invalid flac binary data
     */
    public FlacDecoder(byte[] buf) throws IOException {
        buffer = new FlacBuffer(buf);
        // Flac file contain a 4 character of 'fLaC'
        // See https://xiph.org/flac/format.html#format_overview
        if (buffer.readUint(32) != 0x664C6143)
            throw new InvalidFlacException();
        startFramePosition = -1;
    }

    /**
     * Get stream info
     *
     * @return a flac stream info header
     */
    public StreamInfo getStreamInfo() {
        return streamInfo;
    }

    /**
     * Decode flac
     *
     * @throws IOException when cannot read from buffer data
     */
    public void decode() throws IOException {
        // loop to decode the header
        while (!decodeHeader()) ;
        // loop to decode the frame data
        // the header block let decode frame
        // There is not actual formula to calculate or verify total sample with the data in byte
        // Flac design to be a compressed data, so each frame it have a different size depend on
        // compression optimization. So the only for verify total sample with frame data is to
        // decode frame data itself.
        // In, streaming case, total sample is unknown so we need to decode all the frame data in
        // order to get total sample.
        // See https://xiph.org/flac/format.html#architecture
        for (int offset = 0; ; ) {
            FrameBlock frame = frameDecoder.readFrame(offset);
            if (frame == null) {
                // The end of frame let assign total sample
                if (streamInfo.getTotalSample() != 0 && streamInfo.getTotalSample() != offset)
                    throw new InvalidFlacException();
                else if (streamInfo.getTotalSample() == 0) {
                    streamInfo.setTotalSample(offset);
                }
                break;
            }
            offset += frame.blockSize;
        }
    }

    // decode header of flac, ignore everything but STREAMINFO
    private boolean decodeHeader() throws IOException {
        if (startFramePosition != -1)
            return true;

        boolean last = buffer.readUint(1) != 0;
        int type = buffer.readUint(7);
        int length = buffer.readUint(24);
        byte[] data = Arrays.copyOfRange(buffer.byteBuffer, (int) buffer.getPosition(), (int) buffer.getPosition() + length);
        buffer.seekTo(buffer.getPosition() + length);

        // STREAMINFO only, otherwise ignored
        // See https://xiph.org/flac/format.html#stream
        if (type == 0) {
            // STREAMINFO is unique, we have more than one STREAMINFO header block
            // then it a corruption flac data
            if (streamInfo != null)
                throw new InvalidFlacException();
            streamInfo = new StreamInfo(data);
        } else {
            // the first header must be STREAMINFO
            if (streamInfo == null)
                throw new InvalidFlacException();
        }

        if (last) {
            // mark the position of frame data
            startFramePosition = buffer.getPosition();
            frameDecoder = new FrameDecoder(buffer, streamInfo.getBitPerSecond());
            buffer.seekTo(startFramePosition);
        }
        return last;
    }

}
