package org.synogen.unitypatcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Mapping {

    private final Integer MAX_READ_BUFFER = 2048;

    private final Integer MAX_SKIP_TIMES = 256;

    protected HashMap<String, Object> variables = new HashMap<>();

    protected void parseStructure(SeekableByteChannel channel, String structure) throws IOException, FormatInvalid {
        Pattern definition = Pattern.compile("(s)|(i)|(\\(.+?\\)[s|i])|(\\[(\\d+|x)(.+?)\\])");
        Matcher matcher = definition.matcher(structure);

        while (matcher.find()) {
            String match = matcher.group();
            if (match.matches("s")) {
                readNextString(channel);
            } else if (match.matches("i")) {
                readInteger(channel);
            } else if (match.matches("\\(.+\\)[s]")) {
                variables.put(match.substring(1, match.length()-2), readNextString(channel));
            } else if (match.matches("\\(.+\\)[i]")) {
                variables.put(match.substring(1, match.length()-2), readInteger(channel));
            } else if (match.matches("\\[([0-9]+|x).+?\\]")) {
                String number = matcher.group(5);
                String content = matcher.group(6);
                if (number.matches("x")) {
                    skipVariableStructures(channel, content);
                } else {
                    Integer numberI = Integer.valueOf(number);
                    skipStructureTimes(channel, content, numberI);
                }
                // TODO parse through so variable mappings work for substructures as well
                // parseStructure(channel, match.substring(1, match.length()-2));
            }
        }
    }

    /**
     * Reads the structure count first and then skips over that structure that many times
     * @param channel
     * @param structure
     * @throws IOException
     */
    protected void skipVariableStructures(SeekableByteChannel channel, String structure) throws IOException, FormatInvalid {
        Integer times = readInteger(channel);
        if (times < 0 || times > MAX_SKIP_TIMES) {
            throw new FormatInvalid(channel.position() - 4, times);
        }
        skipStructureTimes(channel, structure, times);
    }

    /**
     * Skips over a structure
     * @param channel
     * @param structure structure string using 'i' for integer and 's' for string, so "iss" would read one integer and two variable length strings
     * @throws IOException
     */
    protected void skipStructure(SeekableByteChannel channel, String structure) throws IOException, FormatInvalid {
        char[] struct = structure.toCharArray();
        for (int i = 0; i < struct.length; i++) {
            switch (struct[i]) {
                case 's': readNextString(channel); break;
                case 'i': readInteger(channel); break;
            }
        }
    }

    /**
     * Skips a given structure times 'times'
     * @param channel
     * @param structure
     * @param times
     * @throws IOException
     */
    protected void skipStructureTimes(SeekableByteChannel channel, String structure, Integer times) throws IOException, FormatInvalid {
        for (int i = 0; i < times; i++) {
            skipStructure(channel, structure);
        }
    }

    /**
     * Reads a single four byte integer
     * @param channel
     * @return
     * @throws IOException
     */
    protected static Integer readInteger(SeekableByteChannel channel) throws IOException {
        ByteBuffer intValue = ByteBuffer.allocate(4);
        intValue.order(ByteOrder.LITTLE_ENDIAN);
        channel.read(intValue);
        return intValue.getInt(0);
    }

    /**
     * Reads a variable length string by reading the length in the first four bytes, then reading length * bytes for the string value
     * @param channel
     * @return
     * @throws IOException
     */
    protected String readNextString(SeekableByteChannel channel) throws IOException, FormatInvalid {
        Integer length = readInteger(channel);
        if (length <= 0 || length> MAX_READ_BUFFER) {
            throw new FormatInvalid(channel.position() - 4, length);
        }
        ByteBuffer buffer = ByteBuffer.allocate(length);
        channel.read(buffer);
        return new String(buffer.array());
    }

    public Integer getInteger(String name) {
        if (variables.containsKey(name)) {
            return (Integer)variables.get(name);
        }
        return 0;
    }

    public String getString(String name) {
        if (variables.containsKey(name)) {
            return (String)variables.get(name);
        }
        return "unknown";
    }
}
