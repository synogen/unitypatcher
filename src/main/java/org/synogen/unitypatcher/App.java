package org.synogen.unitypatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {

    public static void main(String[] args) throws IOException, FormatInvalid {
        if (args == null || args.length < 3) {
            System.out.println("unitypatcher 0.0.3 (https://github.com/synogen/unitypatcher)");
            System.out.println("!! ONLY TEXT ASSETS SUPPORTED FOR IMPORT/PATCHING RIGHT NOW !!");
            System.out.println();
            System.out.println("Usage:");
            System.out.println();
            System.out.println("    unitypatcher export <assets-file> <path ID or asset name or *>");
            System.out.println("    unitypatcher import <assets-file> <path ID or asset name> <file to import>");
            System.out.println("    unitypatcher patch <assets-file> <patch file> <more patch files(optional)...>");
            System.out.println();
            System.out.println("Notes:");
            System.out.println("Use UABE or another tool to get information on the path ID of a certain asset, only file ID 0 is supported by unitypatcher right now.");
            System.out.println("Patch file format:");
            System.out.println("Three or more lines, first line contains the path ID, second line the regular expression to search for and third line and all lines after that contain what to replace it with.");
            System.out.println("Test the regular expression replace in a text editor first for best results.");
            System.out.println("Example:");
            System.out.println("    530");
            System.out.println("    Max level\">\\d+<");
            System.out.println("    Max level\">20<");

        } else {
            AssetFile assetFile = new AssetFile(Paths.get(args[1]));
            assetFile.parse();
            HashMap<UnityIndex, UnityAsset> assets = assetFile.getAssets();
            if (assets.isEmpty()) {
                System.out.println("No assets found.");
                return;
            }

            if (args[0].equalsIgnoreCase("export")) {
                UnityIndex index = indexFromPathIdOrName(args[2], assets);
                String prefix = assetFile.getFile().getFileName().toString();
                prefix = prefix.split("\\.")[0];

                if (index == null) {
                    System.out.println("Asset name/path ID not provided or could not be found, exporting all text assets");
                    for (UnityIndex idx : assets.keySet()) {
                        UnityAsset asset = assets.get(idx);
                        if (asset.isTextContent()) {
                            String filename = prefix + "_" + idx.getInteger("id") + ".txt";
                            Files.write(Paths.get(filename), asset.asTextContent());
                            System.out.println("Exported asset with path ID " + idx.getInteger("id") + " (" + asset.getTextName() + ") as text content (" + filename + ")");
                        }
                    }
                } else {
                    UnityAsset asset = assets.get(index);
                    if (asset.isTextContent()) {
                        String filename = prefix + "_" + index.getInteger("id") + ".txt";
                        Files.write(Paths.get(filename), asset.asTextContent());
                        System.out.println("Exported asset with path ID " + index.getInteger("id") + " (" + asset.getTextName() + ") as text content (" + filename + ")");
                    } else {
                        String filename = prefix + "_" + index.getInteger("id") + ".raw";
                        Files.write(Paths.get(filename), asset.asByteArray());
                        System.out.println("Exported asset with path ID " + index.getInteger("id") + " as raw content (" + filename + ")");
                    }
                }
            } else if (args[0].equalsIgnoreCase("import")) {
                UnityIndex index = indexFromPathIdOrName(args[2], assets);

                UnityAsset asset = assets.get(index);
                if (asset.isTextContent()) {
                    byte[] newContent = Files.readAllBytes(Paths.get(args[3]));
                    asset.replaceTextContent(newContent);
                    assetFile.updateOffsetsAndSize();
                    assetFile.save(Paths.get(args[1] + ".modified"));
                    System.out.println("Imported asset with path ID " + index.getInteger("id") + " as text content");
                    System.out.println("Modified asset file saved as " + args[1] + ".modified");
                }
            } else if (args[0].equalsIgnoreCase("patch")) {
                List<String> files = new ArrayList<>();
                for (int i = 2; i < args.length; i++) {
                    files.add(args[i]);
                }
                for (String file : files) {
                    List<String> lines = Files.readAllLines(Paths.get(file));
                    if (lines.size() >= 3) {
                        String pathId = lines.get(0);
                        String regex = lines.get(1);
                        String replace = "";
                        for (int i = 2; i < lines.size(); i++) {
                            replace += lines.get(i) + ((i < lines.size() - 1) ? "\r\n" : "");
                        }

                        UnityIndex index = indexFromPathIdOrName(pathId, assets);
                        System.out.println("Reading text content from asset " + index.getInteger("id"));
                        UnityAsset asset = assets.get(index);

                        System.out.println("Patching content");
                        String textcontent = new String(asset.asTextContent());

                        Pattern definition = Pattern.compile(regex);
                        Matcher matcher = definition.matcher(textcontent);
                        Integer replacementCount = 0;
                        while (matcher.find()) {
                            replacementCount++;
                        }
                        System.out.println("Replacing " + replacementCount + " matches...");
                        textcontent = matcher.replaceAll(replace);

                        System.out.println("Writing content back to file");
                        asset.replaceTextContent(textcontent.getBytes());
                    }
                }


                assetFile.updateOffsetsAndSize();
                assetFile.save(Paths.get(args[1] + ".modified"));

                System.out.println("Creating backup of the original asset file (" + args[1] + ".modbackup)");
                Files.copy(Paths.get(args[1]), Paths.get(args[1] + ".modbackup"), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Replacing original asset file with modified version");
                Files.copy(Paths.get(args[1] + ".modified"), Paths.get(args[1]), StandardCopyOption.REPLACE_EXISTING);
                Files.delete(Paths.get(args[1] + ".modified"));

            }
            System.out.println("Done.");
        }
    }

    private static UnityIndex indexFromPathIdOrName(String arg, HashMap<UnityIndex, UnityAsset> assets) {
        // find text asset by name if no path ID is given
        if (arg == null) {
            return null;
        } else if (!isInteger(arg)) {
            for (UnityIndex index : assets.keySet()) {
                UnityAsset asset = assets.get(index);
                if (asset.isTextContent() && arg.equalsIgnoreCase(asset.getTextName())) {
                    return index;
                }
            }
            return null;
        } else {
            Integer pathId = Integer.parseInt(arg);
            return new ArrayList<>(assets.keySet()).get(pathId - 1);
        }
    }

    private static boolean isInteger(String arg) {
        try {
            Integer.parseInt(arg);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
