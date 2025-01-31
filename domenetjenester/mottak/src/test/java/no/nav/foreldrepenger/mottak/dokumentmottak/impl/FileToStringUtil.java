package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileToStringUtil {
    public FileToStringUtil() {
        // Tom konstruktør
    }

    public String readFile(String filename) throws URISyntaxException, IOException {
        var path = Paths.get(getClass().getClassLoader().getResource(filename).toURI());
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
