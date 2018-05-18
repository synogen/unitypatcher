package org.synogen.unitypatcher;

import java.io.IOException;
import java.nio.file.Paths;

public class App {

    public static void main(String[] args) throws IOException, FormatInvalid {
        FindIndexEntryPoint p = new FindIndexEntryPoint(Paths.get("sharedassets1.assets"));
        p.parse();
    }
}
