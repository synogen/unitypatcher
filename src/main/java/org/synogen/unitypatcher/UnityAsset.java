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
        content.position(content.position() + nameLength);
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

        boolean even = (4 + nameLength + 4 + newContent.length) % 2 == 0;
        ByteBuffer newBuffer = ByteBuffer.allocate(4 + nameLength + 4 + newContent.length + (even ? 2 : 1));
        newBuffer.order(ByteOrder.LITTLE_ENDIAN);
        newBuffer.putInt(nameLength);
        newBuffer.put(name);
        newBuffer.putInt(newContent.length);
        newBuffer.put(newContent);

        newBuffer.put(even ? new byte[]{0,0} : new byte[]{0});
        content = newBuffer;
    }

    public Integer getSize() {
        return content.capacity();
    }
}
