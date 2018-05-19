package org.synogen.unitypatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class App {

    public static void main(String[] args) throws IOException, FormatInvalid {
        AssetIndex p = new AssetIndex(Paths.get(args[1]));
        p.parse();
        HashMap<UnityIndex, UnityAsset> assets = p.getAssets();

        if (args[0].equalsIgnoreCase("export")) {
            Integer pathId = Integer.parseInt(args[2]);
            UnityIndex index = new ArrayList<>(assets.keySet()).get(pathId - 1);
            UnityAsset asset = assets.get(index);
            if (index.getType() == 5) {
                Files.write(Paths.get(pathId + ".txt"), asset.asTextContent());
            } else {
                Files.write(Paths.get(pathId + ".raw"), asset.asByteArray());
            }
        }

    }
}
