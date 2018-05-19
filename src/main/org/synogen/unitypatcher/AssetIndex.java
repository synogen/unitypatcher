package org.synogen.unitypatcher;

import lombok.Getter;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AssetIndex extends Mapping {

    private final Integer BYTES_TO_SCAN = 10240;
    private final String HEADER_STRUCTURE = "i(filesize)i(version)i(headersize)i";
    private final String INDEX_STRUCTURE = "(id)ii(offset)i(size)ii";

    private Path file;

    private Integer headerSize;

    @Getter
    private List<UnityIndex> assetIndex;

    public AssetIndex(Path file) {
        this.file = file;
    }

    public Integer parse() throws IOException, FormatInvalid {

        SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ);
        Long filesize = channel.size();
        setByteOrder(ByteOrder.BIG_ENDIAN);
        System.out.println("Reading header");
        parseStructure(channel, HEADER_STRUCTURE);
        headerSize = getInteger("headersize");

        System.out.println("Trying to find entry point for file index...");
        setByteOrder(ByteOrder.LITTLE_ENDIAN);
        assetIndex = getIndexList(channel);



        channel.close();
        return 0;
    }

    private List<UnityIndex> getIndexList(SeekableByteChannel channel) throws IOException, FormatInvalid {
        Long filesize = channel.size();

        // try and find the repeating pattern for the asset index
        HashMap<Integer, List<UnityIndex>> indexLists = new HashMap<>();
        for (int i = 0; i <= BYTES_TO_SCAN; i++) {
            channel.position(i);
            parseStructure(channel, INDEX_STRUCTURE);

            Integer previousId = 0;
            Integer currentId = getInteger("id");
            Integer offset = getInteger("offset");
            Integer size = getInteger("size");
            if (currentId == 1 && offset >= 0 && offset < filesize && size >= 0 && size < filesize) {
                List<UnityIndex> indexList = new ArrayList<>();
                do {
                    indexList.add(new UnityIndex(currentId, offset));
                    previousId = currentId;
                    parseStructure(channel, INDEX_STRUCTURE);
                    currentId = getInteger("id");
                    offset = getInteger("offset");
                    size = getInteger("size");
                } while (previousId + 1 == currentId && offset >= 0 && offset < filesize && size >= 0 && size < filesize);

                if (indexList.size() > 1) {
                    indexLists.put(i, indexList);
                }
            }
        }

        // keep only the biggest list
        List<UnityIndex> result = new ArrayList<>();
        Integer biggestSize = 0;
        for (List<UnityIndex> indexList : indexLists.values()) {
            if (indexList.size() > biggestSize) {
                biggestSize = indexList.size();
                result = indexList;
            }
        }

        return result;
    }

    public HashMap<UnityIndex, UnityAsset> getAllAssets() {

        return null;
    }
}
