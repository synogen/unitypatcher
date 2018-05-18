package org.synogen.unitypatcher;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnityMapping extends Mapping {

    private Integer startAt;
    private String structure;



    /**
     *
     * @param startAt At which offset to start reading the file
     * @param structure string definition of the file structure
     *                  s -> a string consisting of an integer for length and then the string itself
     *                  i -> an integer
     *                  [3si] -> a structure 'si' occuring 3 times
     *                  [xsi] -> a structure 'si' occuring x times, the structure is prefaced by an integer that gives the amount of times it occurs
     *                  (fuel)i -> map the next integer to the variable 'fuel'
     */
    public UnityMapping(Integer startAt, String structure) {
        this.startAt = startAt;
        this.structure = structure.replaceAll("\\s","");
    }

    public void parse(Path path) throws IOException, FormatInvalid {
        SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ);
        channel.position(startAt);

        parseStructure(channel, structure);

        channel.close();
    }

}
