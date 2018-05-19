package org.synogen.unitypatcher;

import lombok.Data;

@Data
public class UnityIndex {

    public UnityIndex(Integer id, Integer offset, Integer size, Integer type) {
        this.id = id;
        this.offset = offset;
        this.size = size;
        this.type = type;
    }

    private Integer id;
    private Integer offset;
    private Integer size;
    private Integer type;
}
