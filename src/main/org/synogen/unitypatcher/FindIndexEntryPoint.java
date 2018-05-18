package org.synogen.unitypatcher;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FindIndexEntryPoint extends Mapping {

    private final Integer BYTES_TO_SCAN = 10240;

    private Path file;

    public FindIndexEntryPoint(Path file) {
        this.file = file;
    }

    public Integer parse() throws IOException, FormatInvalid {
        System.out.println("Trying to find entry point for file index...");
        SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ);
        Long filesize = channel.size();
        HashMap<Integer, List<UnityIndex>> indexLists = new HashMap<>();
        for (int i = 0; i <= BYTES_TO_SCAN; i++) {
            channel.position(i);
            parseStructure(channel, "(id)iii(offset)ii");

            Integer previousId = 0;
            Integer currentId = getInteger("id");
            Integer offset = getInteger("offset");
            if (currentId == 1 && offset > 0 && offset < filesize) {
                List<UnityIndex> indexList = new ArrayList<>();
                do {
                    indexList.add(new UnityIndex(currentId, offset));
                    previousId = currentId;
                    parseStructure(channel, "(id)iii(offset)ii");
                    currentId = getInteger("id");
                    offset = getInteger("offset");
                } while (previousId + 1 == currentId && offset > 0 && offset < filesize);

                if (indexList.size() > 1) {
                    indexLists.put(i, indexList);
                }
            }
        }
        channel.close();

        // TODO remove overlapping lists (remove the one with fewer entries)

        // TODO keep only the biggest list after removing overlaps

        return 0;
    }
}
