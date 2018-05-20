package org.synogen.unitypatcher;

import lombok.Data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@Data
public class UnityIndex {

    public UnityIndex(Integer id, Integer offset, Integer size, Integer type, Integer unknown) {
        this.id = id;
        this.offset = offset;
        this.size = size;
        this.type = type;
        this.unknown = unknown;
    }

    private Integer id;
    private Integer offset;
    private Integer size;
    private Integer type;
    private Integer unknown;

    public ByteBuffer asBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(20);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(id);
        buffer.putInt(unknown);
        buffer.putInt(offset);
        buffer.putInt(size);
        buffer.putInt(type);
        buffer.flip();
        return buffer;
    }

    @Override
    public int hashCode() {
        return id * 1000 + type;
    }
}
