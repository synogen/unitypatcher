package org.synogen.unitypatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class App {

    public static void main(String[] args) throws IOException, FormatInvalid {
        AssetFile assetFile = new AssetFile(Paths.get(args[1]));
        assetFile.parse();
        HashMap<UnityIndex, UnityAsset> assets = assetFile.getAssets();

        Integer pathId = Integer.parseInt(args[2]);
        UnityIndex index = new ArrayList<>(assets.keySet()).get(pathId - 1);
        UnityAsset asset = assets.get(index);

        if (args[0].equalsIgnoreCase("export")) {
            if (index.getType() == 5) {
                Files.write(Paths.get(pathId + ".txt"), asset.asTextContent());
            } else {
                Files.write(Paths.get(pathId + ".raw"), asset.asByteArray());
            }
        } else if (args[0].equalsIgnoreCase("import")) {
            if (index.getType() == 5) {
                byte[] newContent = Files.readAllBytes(Paths.get(args[3]));
                asset.replaceTextContent(newContent);
                assetFile.updateOffsetsAndSize();
                assetFile.save(Paths.get(args[1] + ".modified"));
            }

        }

    }
}
