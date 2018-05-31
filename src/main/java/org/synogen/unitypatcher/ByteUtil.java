package org.synogen.unitypatcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;

public class ByteUtil {

    private static final Integer MAX_READ_BUFFER = 2097152;
    private static final Integer MAX_SKIP_TIMES = 1024;

    public static Integer readInteger(SeekableByteChannel channel) throws IOException {
        ByteBuffer intValue = ByteBuffer.allocate(4);
        channel.read(intValue);
        return intValue.getInt(0);
    }

    public static Integer readInteger(SeekableByteChannel channel, ByteOrder byteOrder) throws IOException {
        ByteBuffer intValue = ByteBuffer.allocate(4);
        intValue.order(byteOrder);
        channel.read(intValue);
        return intValue.getInt(0);
    }

    public static String readNextString(SeekableByteChannel channel) throws IOException, FormatInvalid {
        Integer length = readInteger(channel);
        if (length <= 0 || length > MAX_READ_BUFFER) {
            throw new FormatInvalid(channel.position() - 4, length);
        }
        ByteBuffer buffer = ByteBuffer.allocate(length);
        channel.read(buffer);
        return new String(buffer.array());
    }

    /**
     * Reads the structure count first and then skips over that structure that many times
     * @param channel
     * @param structure
     * @throws IOException
     */
    public static void skipVariableStructures(SeekableByteChannel channel, String structure) throws IOException, FormatInvalid {
        Integer times = readInteger(channel);
        if (times < 0 || times > MAX_SKIP_TIMES) {
            throw new FormatInvalid(channel.position() - 4, times);
        }
        skipStructureTimes(channel, structure, times);
    }

    public static void skipStructure(SeekableByteChannel channel, String structure) throws IOException, FormatInvalid {
        char[] struct = structure.toCharArray();
        for (int i = 0; i < struct.length; i++) {
            switch (struct[i]) {
                case 's': readNextString(channel); break;
                case 'i': readInteger(channel); break;
            }
        }
    }

    public static void skipStructureTimes(SeekableByteChannel channel, String structure, Integer times) throws IOException, FormatInvalid {
        for (int i = 0; i < times; i++) {
            skipStructure(channel, structure);
        }
    }
}
