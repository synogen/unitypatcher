package org.synogen.unitypatcher;

import java.io.IOException;
import java.nio.file.Paths;

public class App {

    public static void main(String[] args) throws IOException, FormatInvalid {
        AssetIndex p = new AssetIndex(Paths.get("sharedassets3.assets"));
        p.parse();
    }
}
