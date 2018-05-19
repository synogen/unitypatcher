package org.synogen.unitypatcher;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UnityAsset {

    private ByteBuffer content;

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
}
