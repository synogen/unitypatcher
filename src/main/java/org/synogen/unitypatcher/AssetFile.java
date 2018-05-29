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

    private final Integer BYTES_TO_SCAN = 10240;
    private final String HEADER_STRUCTURE = "i(filesize)i(version)i(headersize)i";
    private final String INDEX_STRUCTURE = "(id)i(unknown)i(offset)i(size)i(type)i";

    private Path file;

    private Integer headerSize;

    private Long filesize;

    private Integer newFilesize;

    private Integer indexOffset;

    @Getter
    private List<UnityIndex> assetIndex;
    @Getter
    private HashMap<UnityIndex, UnityAsset> assets;



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
        assetIndex = getIndexList(channel);
        if (assetIndex != null) {
            System.out.println("Found file index, reading assets...");
            assets = getAllAssets(channel);
        } else {
            System.out.println("No file index found");
        }

        channel.close();
    }

    private List<UnityIndex> getIndexList(SeekableByteChannel channel) throws IOException, FormatInvalid {
        Long filesize = channel.size();

        // try and find the repeating pattern for the asset index
        HashMap<Integer, List<UnityIndex>> indexLists = new LinkedHashMap<>();
        for (int i = 0; i <= BYTES_TO_SCAN; i++) {
            channel.position(i);
            parseStructure(channel, INDEX_STRUCTURE);

            Integer previousId = 0;
            Integer currentId = getInteger("id");
            Integer offset = getInteger("offset");
            Integer size = getInteger("size");
            Integer type = getInteger("type");
            Integer unknown = getInteger("unknown");
            if (currentId == 1 && offset >= 0 && offset < filesize && size >= 0 && size < filesize) {
                List<UnityIndex> indexList = new ArrayList<>();
                do {
                    indexList.add(new UnityIndex(currentId, offset, size, type, unknown));
                    previousId = currentId;
                    parseStructure(channel, INDEX_STRUCTURE);
                    currentId = getInteger("id");
                    offset = getInteger("offset");
                    size = getInteger("size");
                    type = getInteger("type");
                    unknown = getInteger("unknown");
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
            if (index.getSize() > 0) {
                ByteBuffer buffer = ByteBuffer.allocate(index.getSize());
                channel.position(index.getOffset() + headerSize).read(buffer);
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
                index.setOffset(index.getOffset() + offset);
            }
            if (index.getSize().compareTo(asset.getSize()) != 0) {
                offset += asset.getSize() - index.getSize();
                index.setSize(asset.getSize());
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
            Integer paddingLength = index.getOffset() - (previousOffset + previousSize);

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

            previousOffset = index.getOffset();
            previousSize = index.getSize();
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
