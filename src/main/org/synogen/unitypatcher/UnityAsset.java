package org.synogen.unitypatcher;

import java.nio.ByteBuffer;

public class UnityAsset {

    private ByteBuffer content;

    public UnityAsset(ByteBuffer content) {
        this.content = content;
    }

    public String asString() {
        return new String(this.content.array());
    }
}
