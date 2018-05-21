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
            System.out.println("unitypatcher test version (https://github.com/synogen/unitypatcher)");
            System.out.println("!! ONLY TEXT ASSETS SUPPORTED RIGHT NOW !!");
            System.out.println();
            System.out.println("Usage:");
            System.out.println();
            System.out.println("    unitypatcher export <assets-file> <path ID>");
            System.out.println("    unitypatcher import <assets-file> <path ID> <file to import>");
            System.out.println("    unitypatcher patch <assets-file> <patch file>");
            System.out.println();
            System.out.println("Notes:");
            System.out.println("Use UABE or another tool to get information on the path ID of a certain asset, only file ID 0 is supported by unitypatcher right now.");
            System.out.println("Patch file format:");
            System.out.println("Three lines, first line contains the path ID, second line the regular expression to search for and third line what to replace it with.");
            System.out.println("Test the regular expression replace in a text editor first for best results.");
            System.out.println("Example:");
            System.out.println("    530");
            System.out.println("    Max level\">\\d+<");
            System.out.println("    Max level\">20<");

        } else {
            AssetFile assetFile = new AssetFile(Paths.get(args[1]));
            assetFile.parse();
            HashMap<UnityIndex, UnityAsset> assets = assetFile.getAssets();

            if (args[0].equalsIgnoreCase("export")) {
                UnityIndex index = indexFromPathIdOrName(args[2], assets);

                UnityAsset asset = assets.get(index);
                if (index.getType() == 5) {
                    Files.write(Paths.get(index.getId() + ".txt"), asset.asTextContent());
                    System.out.println("Exported asset with path ID " + index.getId() + " as text content (" + index.getId() + ".txt)");
                } else {
                    Files.write(Paths.get(index.getId() + ".raw"), asset.asByteArray());
                    System.out.println("Exported asset with path ID " + index.getId() + " as raw content (" + index.getId() + ".raw)");
                }
            } else if (args[0].equalsIgnoreCase("import")) {
                UnityIndex index = indexFromPathIdOrName(args[2], assets);

                UnityAsset asset = assets.get(index);
                if (index.getType() == 5) {
                    byte[] newContent = Files.readAllBytes(Paths.get(args[3]));
                    asset.replaceTextContent(newContent);
                    assetFile.updateOffsetsAndSize();
                    assetFile.save(Paths.get(args[1] + ".modified"));
                    System.out.println("Imported asset with path ID " + index.getId() + " as text content");
                    System.out.println("Modified asset file saved as " + args[1] + ".modified");
                }
            } else if (args[0].equalsIgnoreCase("patch")) {
                List<String> lines = Files.readAllLines(Paths.get(args[2]));
                if (lines.size() >= 3) {
                    String pathId = lines.get(0);
                    String regex = lines.get(1);
                    String replace = lines.get(2);

                    UnityIndex index = indexFromPathIdOrName(pathId, assets);
                    System.out.println("Reading text content from asset " + index.getId());
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
                    assetFile.updateOffsetsAndSize();
                    assetFile.save(Paths.get(args[1] + ".modified"));

                    System.out.println("Creating backup of the original asset file (" + args[1] + ".modbackup)");
                    Files.copy(Paths.get(args[1]), Paths.get(args[1] + ".modbackup"), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Replacing original asset file with modified version")
                    Files.copy(Paths.get(args[1] + ".modified"), Paths.get(args[1]), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            System.out.println("Done.");
        }
    }

    private static UnityIndex indexFromPathIdOrName(String arg, HashMap<UnityIndex, UnityAsset> assets) {
        UnityIndex result = null;
        // find text asset by name if no path ID is given
        if (!isInteger(arg)) {
            for (UnityIndex index : assets.keySet()) {
                if (index.getType().compareTo(5) == 0) {
                    UnityAsset asset = assets.get(index);
                    if (arg.equalsIgnoreCase(asset.getTextName())) {
                        result = index;
                        break;
                    }
                }
            }
        } else {
            Integer pathId = Integer.parseInt(arg);
            result = new ArrayList<>(assets.keySet()).get(pathId - 1);
        }
        return result;
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
