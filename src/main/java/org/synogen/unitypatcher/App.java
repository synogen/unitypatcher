package org.synogen.unitypatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class App {

    public static void main(String[] args) throws IOException, FormatInvalid {
        if (args == null || args.length < 3) {
            System.out.println("unitypatcher test version");
            System.out.println("!! ONLY TEXT ASSETS SUPPORTED RIGHT NOW !!");
            System.out.println();
            System.out.println("Usage:");
            System.out.println();
            System.out.println("    unitypatcher export <assets-file> <path ID>");
            System.out.println("    unitypatcher import <assets-file> <path ID> <file to import>");
        } else {
            AssetFile assetFile = new AssetFile(Paths.get(args[1]));
            assetFile.parse();
            HashMap<UnityIndex, UnityAsset> assets = assetFile.getAssets();

            Integer pathId = Integer.parseInt(args[2]);
            UnityIndex index = new ArrayList<>(assets.keySet()).get(pathId - 1);
            UnityAsset asset = assets.get(index);

            if (args[0].equalsIgnoreCase("export")) {
                if (index.getType() == 5) {
                    Files.write(Paths.get(pathId + ".txt"), asset.asTextContent());
                    System.out.println("Exported asset with path ID " + pathId + " as text content (" + pathId + ".txt)");
                } else {
                    Files.write(Paths.get(pathId + ".raw"), asset.asByteArray());
                    System.out.println("Exported asset with path ID " + pathId + " as raw content (" + pathId + ".raw)");
                }
            } else if (args[0].equalsIgnoreCase("import")) {
                if (index.getType() == 5) {
                    byte[] newContent = Files.readAllBytes(Paths.get(args[3]));
                    asset.replaceTextContent(newContent);
                    assetFile.updateOffsetsAndSize();
                    assetFile.save(Paths.get(args[1] + ".modified"));
                    System.out.println("Imported asset with path ID " + pathId + " as text content");
                    System.out.println("Modified asset file saved as " + args[1] + ".modified");
                }

            }
            System.out.println("Done.");
        }
    }
}
