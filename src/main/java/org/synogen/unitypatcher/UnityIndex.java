package org.synogen.unitypatcher;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnityIndex {

    private enum AttributeType {
        STRING, INTEGER
    }

    @Data
    @AllArgsConstructor
    private class Attribute {
        private Integer pos;
        private Integer size;
        private AttributeType type;
    }

    private String structure;

    private HashMap<String, Integer> attributes = new HashMap<>();
    private List<Object> attributeList = new ArrayList<>();

    public UnityIndex(String structure, byte[] content) throws IOException, FormatInvalid {
        this.structure = structure;
        attributes = parseAttributes(structure, content);
    }

    private HashMap<String, Integer> parseAttributes(String structure, byte[] byteContent) throws IOException, FormatInvalid {
        HashMap<String, Integer> result = new HashMap<>();
        Pattern definition = Pattern.compile("(s)|(i)|(\\(.+?\\)[s|i])|(\\[(\\d+|x)(.+?)\\])");
        Matcher matcher = definition.matcher(structure);

        SeekableByteChannel channel = new SeekableInMemoryByteChannel(byteContent);

        attributeList = new ArrayList<>();
        while (matcher.find()) {
            String match = matcher.group();
            if (match.matches("s")) {
                attributeList.add(ByteUtil.readNextString(channel));
            } else if (match.matches("i")) {
                attributeList.add(ByteUtil.readInteger(channel));
            } else if (match.matches("\\(.+\\)[s]")) {
                result.put(match.substring(1, match.length() - 2), attributeList.size());
                attributeList.add(ByteUtil.readNextString(channel));
            } else if (match.matches("\\(.+\\)[i]")) {
                result.put(match.substring(1, match.length() - 2), attributeList.size());
                attributeList.add(ByteUtil.readInteger(channel, ByteOrder.LITTLE_ENDIAN));
            }
        }
        return result;
    }

    public Integer getInteger(String name) {
        return (Integer)attributeList.get(attributes.get(name));
    }

    public void setInteger(String name, Integer number) {
        attributeList.set(attributes.get(name), number);
    }

    public String getString(String name) {
        return (String)attributeList.get(attributes.get(name));
    }

    public void setString(String name, String text) {
        attributeList.set(attributes.get(name), text);
    }

    private ByteBuffer convertString(String text) {
        ByteBuffer result = ByteBuffer.allocate(text.length() + 4);
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.putInt(text.length());
        result.put(text.getBytes());
        result.flip();
        return result;
    }

    private ByteBuffer convertInteger(Integer number) {
        ByteBuffer result = ByteBuffer.allocate(4);
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.putInt(number);
        result.flip();
        return result;
    }

    public ByteBuffer asBuffer() throws IOException {
        SeekableByteChannel channel = new SeekableInMemoryByteChannel();
        for (Object object : attributeList) {
            if (object instanceof String) {
                channel.write(convertString((String)object));
            } else if (object instanceof Integer) {
                channel.write(convertInteger((Integer)object));
            }
        }
        ByteBuffer result = ByteBuffer.allocate(Math.toIntExact(channel.size()));
        channel.position(0);
        channel.read(result);
        result.flip();
        return result;
    }

    @Override
    public int hashCode() {
        return getInteger("id") * 1000 + getInteger("type");
    }
}
