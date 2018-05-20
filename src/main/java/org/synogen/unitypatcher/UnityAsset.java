package org.synogen.unitypatcher;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UnityAsset {

    private ByteBuffer content;

    public ByteBuffer getOutputBuffer() {
        content.order(ByteOrder.LITTLE_ENDIAN);
        content.flip();
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

    public byte[] asTextContent() {
        content.order(ByteOrder.LITTLE_ENDIAN);
        content.position(0);
        Integer nameLength = content.getInt();
        content.position(content.position() + nameLength + paddingFor(nameLength));
        Integer contentLength = content.getInt();
        byte[] textContent = new byte[contentLength];
        content.get(textContent);
        return textContent;
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
