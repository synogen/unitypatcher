package org.synogen.unitypatcher;

import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class AssetFile extends Mapping {

    private final Integer BYTES_TO_SCAN = 102400;
    private final String HEADER_STRUCTURE = "i(filesize)i(version)i(headersize)i";
    private final String INDEX_STRUCTURE_UNITY2017 = "(id)ii(offset)i(size)i(type)i";
    private final Integer INDEX_LENGTH_UNITY2017 = 20;
    private final String INDEX_STRUCTURE_UNITY5 = "(id)ii(offset)i(size)i(type)iii";
    private final Integer INDEX_LENGTH_UNITY5 = 28;

    @Getter
    private Path file;

    private Integer headerSize;

    private Long filesize;

    private Integer newFilesize;

    private Integer indexOffset;

    @Getter
    private List<UnityIndex> assetIndex;
    @Getter
    private HashMap<UnityIndex, UnityAsset> assets = new LinkedHashMap<>();



    public AssetFile(Path file) {
        this.file = file;
    }

    public void parse() throws IOException, FormatInvalid {

        SeekableByteChannel channel = Files.newByteChannel(file, StandardOpenOption.READ);
        filesize = channel.size();
        setByteOrder(ByteOrder.BIG_ENDIAN);
        System.out.println("Reading header");
        parseStructure(channel, HEADER_STRUCTURE);
        headerSize = getInteger("headersize");

        System.out.println("Trying to find entry point for file index...");
        setByteOrder(ByteOrder.LITTLE_ENDIAN);
        assetIndex = getIndexList(channel, INDEX_STRUCTURE_UNITY2017, INDEX_LENGTH_UNITY2017);
        if (assetIndex.isEmpty()) {
            System.out.println("No Unity 2017 index found, trying Unity 5...");
            assetIndex = getIndexList(channel, INDEX_STRUCTURE_UNITY5, INDEX_LENGTH_UNITY5);
        }

        if (!assetIndex.isEmpty()) {
            System.out.println("Found file index with " + assetIndex.size() + " entries, reading assets...");
            assets = getAllAssets(channel);
        } else {
            System.out.println("No file index found");
        }

        channel.close();
    }

    private List<UnityIndex> getIndexList(SeekableByteChannel channel, String indexStructure, Integer indexLength) throws IOException, FormatInvalid {
        Long filesize = channel.size();

        // try and find the repeating pattern for the asset index
        HashMap<Integer, List<UnityIndex>> indexLists = new LinkedHashMap<>();
        for (int i = 0; i <= BYTES_TO_SCAN && i < filesize - indexLength; i++) {
            channel.position(i);

            Integer previousId = 0;
            ByteBuffer buffer = ByteBuffer.allocate(indexLength);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            channel.read(buffer);
            UnityIndex index = new UnityIndex(indexStructure, buffer.array());

            Integer currentId = index.getInteger("id");
            Integer offset = index.getInteger("offset");
            Integer size = index.getInteger("size");
            if (currentId == 1 && offset >= 0 && offset < filesize && size >= 0 && size < filesize) {
                List<UnityIndex> indexList = new ArrayList<>();
                do {
                    indexList.add(index);

                    previousId = currentId;

                    buffer = ByteBuffer.allocate(indexLength);
                    channel.read(buffer);
                    index = new UnityIndex(indexStructure, buffer.array());

                    currentId = index.getInteger("id");
                    offset = index.getInteger("offset");
                    size = index.getInteger("size");
                } while (previousId + 1 == currentId && offset >= 0 && offset < filesize && size >= 0 && size < filesize);

                if (indexList.size() > 1) {
                    indexLists.put(i, indexList);
                }
            }
        }

        // keep only the biggest list
        List<UnityIndex> result = new ArrayList<>();
        Integer biggestSize = 0;
        Integer index = 0;
        for (Integer offset : indexLists.keySet()) {
            List<UnityIndex> indexList = indexLists.get(offset);
            if (indexList.size() > biggestSize) {
                biggestSize = indexList.size();
                result = indexList;
                index = offset;
            }
        }
        indexOffset = index;

        return result;
    }

    public HashMap<UnityIndex, UnityAsset> getAllAssets(SeekableByteChannel channel) throws IOException {
        HashMap<UnityIndex, UnityAsset> assets = new LinkedHashMap<>();
        for (UnityIndex index : assetIndex) {
            if (index.getInteger("size") > 0) {
                ByteBuffer buffer = ByteBuffer.allocate(index.getInteger("size"));
                channel.position(index.getInteger("offset") + headerSize).read(buffer);
                assets.put(index, new UnityAsset(buffer));
            }
        }

        return assets;
    }

    public void updateOffsetsAndSize() {
        Integer offset = 0;
        for (UnityIndex index : assets.keySet()) {
            UnityAsset asset = assets.get(index);
            if (offset != 0) {
                index.setInteger("offset", index.getInteger("offset") + offset);
            }
            if (index.getInteger("size").compareTo(asset.getSize()) != 0) {
                offset += asset.getSize() - index.getInteger("size");
                index.setInteger("size", asset.getSize());
            }
        }
        newFilesize = Math.toIntExact(filesize + offset);
    }

    public void save(Path savefile) throws IOException {
        SeekableByteChannel readChannel = Files.newByteChannel(file, StandardOpenOption.READ);

        if (Files.exists(savefile)) {
            Files.delete(savefile);
        }
        Files.createFile(savefile);

        SeekableByteChannel writeChannel = Files.newByteChannel(savefile, StandardOpenOption.APPEND);

        ByteBuffer intBuffer = ByteBuffer.allocate(4);
        intBuffer.order(ByteOrder.BIG_ENDIAN);
        ByteBuffer skipBuffer = ByteBuffer.allocate(4);

        // copy beginning of the header
        copyBytes(readChannel, writeChannel, 4);

        // update filesize
        readChannel.read(skipBuffer);
        intBuffer.putInt(newFilesize);
        intBuffer.flip();
        writeChannel.write(intBuffer);

        // copy rest of the header
        copyBytes(readChannel, writeChannel, indexOffset - 4 - 4);


        // skip index reading
        skipBuffer = ByteBuffer.allocate(assets.size() * 20);
        readChannel.read(skipBuffer);

        // write new index from assets
        for (UnityIndex index : assets.keySet()) {
            writeChannel.write(index.asBuffer());
        }

        // copy part between index and assets
        copyBytes(readChannel, writeChannel, Math.toIntExact(headerSize - readChannel.position()));

        // write assets
        Integer previousOffset = 0;
        Integer previousSize = 0;
        for (UnityIndex index : assets.keySet()) {
            Integer paddingLength = index.getInteger("offset") - (previousOffset + previousSize);

            // null-byte padding that exists in the original file for unknown reasons
            if (paddingLength > 0) {
                ByteBuffer paddingBuffer = ByteBuffer.allocate(paddingLength);
                for (int i = 0; i < paddingLength; i++) {
                    paddingBuffer.put((byte) 0);
                }
                paddingBuffer.flip();
                writeChannel.write(paddingBuffer);
            }

            UnityAsset asset = assets.get(index);

            writeChannel.write(asset.getOutputBuffer());

            previousOffset = index.getInteger("offset");
            previousSize = index.getInteger("size");
        }

        readChannel.close();
        writeChannel.close();
    }

    private void copyBytes(SeekableByteChannel readChannel, SeekableByteChannel writeChannel, Integer count) throws IOException {
        ByteBuffer copyBuffer = ByteBuffer.allocate(count);

        readChannel.read(copyBuffer);
        copyBuffer.flip();
        writeChannel.write(copyBuffer);
    }
}
