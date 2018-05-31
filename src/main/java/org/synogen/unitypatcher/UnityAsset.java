package org.synogen.unitypatcher;

import sun.reflect.generics.tree.ByteSignature;

import java.math.BigDecimal;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UnityAsset extends Mapping {

    private ByteBuffer content;

    public ByteBuffer getOutputBuffer() {
        content.order(ByteOrder.LITTLE_ENDIAN);
        content.position(0);
        return content;
    }

    public UnityAsset(ByteBuffer content) {
        this.content = content;
    }

    public String asString() {
        return new String(this.content.array());
    }

    public byte[] asByteArray() {
        return content.array();
    }

    public String getTextName() {
        content.order(ByteOrder.LITTLE_ENDIAN);
        content.position(0);
        Integer nameLength = content.getInt();
        byte[] textName = new byte[nameLength];
        content.get(textName);
        return new String(textName);
    }

    public byte[] asTextContent() {
        try {
            content.order(ByteOrder.LITTLE_ENDIAN);
            content.position(0);
            Integer nameLength = content.getInt();
            content.position(content.position() + nameLength + paddingFor(nameLength));
            Integer contentLength = content.getInt();
            byte[] textContent = new byte[contentLength];
            content.get(textContent);
            return textContent;
        } catch (BufferUnderflowException e) {
            return "Failed to export text content, invalid size".getBytes();
        }
    }

    public boolean isTextContent() {
        content.order(ByteOrder.LITTLE_ENDIAN);
        content.position(0);
        Integer nameLength = content.getInt();
        if (nameLength > 0 && nameLength <= 300 && nameLength < (content.capacity() - content.position())) {
            byte[] name = new byte[nameLength];
            content.get(name);
            if (textPercentage(name) > 75 && (content.position() + paddingFor(nameLength)) < content.capacity()) {
                content.position(content.position() + paddingFor(nameLength));
                Integer textLength = content.getInt();
                if (textLength > 5 && textLength < 20000000 && textLength <= (content.capacity() - content.position())) {
                    byte[] text = new byte[textLength];
                    content.get(text);
                    if (textPercentage(text) > 75) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Long textPercentage(byte[] text) {
        Integer numText = 0;
        for (int i = 0; i < text.length; i++) {
            switch (text[i]) {
                case 0x09:
                case 0x0A:
                case 0x0B:
                case 0x0D:
                    numText++; break;
                default:
                    if (text[i] > 0x20 && text[i] < 0x7E) {
                        numText++;
                    }
            }
        }
        return Math.round((double)numText / (double)text.length * 100);
    }

    public void replaceTextContent(byte[] newContent) {
        content.order(ByteOrder.LITTLE_ENDIAN);
        content.position(0);
        Integer nameLength = content.getInt();
        byte[] name = new byte[nameLength];
        content.get(name);

        ByteBuffer newBuffer = ByteBuffer.allocate(4 + nameLength + paddingFor(nameLength) + 4 + newContent.length + paddingFor(newContent.length));
        newBuffer.order(ByteOrder.LITTLE_ENDIAN);
        newBuffer.putInt(nameLength);
        newBuffer.put(name);
        newBuffer.put(paddingBytes(nameLength));
        newBuffer.putInt(newContent.length);
        newBuffer.put(newContent);
        newBuffer.put(paddingBytes(newContent.length));

        content = newBuffer;
    }

    public Integer getSize() {
        return content.capacity();
    }

    private Integer paddingFor(Integer length) {
        return length % 4 == 0 ? 0 : 4 - (length % 4);
    }

    private byte[] paddingBytes(Integer length) {
        Integer padding = paddingFor(length);
        byte[] result = new byte[padding];
        for (int i = 0; i < padding; i++) {
            result[i] = 0;
        }
        return result;
    }
}
