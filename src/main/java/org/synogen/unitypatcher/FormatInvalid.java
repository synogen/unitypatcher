package org.synogen.unitypatcher;

public class FormatInvalid extends Throwable {

    public FormatInvalid(long position, Integer value) {
        super("Unexpected value " + value + " at position " + position);
    }
}
