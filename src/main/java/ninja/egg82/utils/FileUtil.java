package ninja.egg82.utils;

import java.io.*;
import java.nio.file.Files;

public class FileUtil {
    private FileUtil() { }

    public static File getOrCreateFile(File fileOnDisk) throws IOException { return getOrCreateFile(null, fileOnDisk); }

    public static File getOrCreateFile(String resourcePath, File fileOnDisk) throws IOException {
        if (fileOnDisk == null) {
            throw new IllegalArgumentException("fileOnDisk cannot be null.");
        }

        File parentDir = fileOnDisk.getParentFile();
        if (parentDir.exists() && !parentDir.isDirectory()) {
            Files.delete(parentDir.toPath());
        }
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                throw new IOException("Could not create parent directory structure.");
            }
        }
        if (fileOnDisk.exists() && fileOnDisk.isDirectory()) {
            Files.delete(fileOnDisk.toPath());
        }

        if (!fileOnDisk.exists()) {
            if (resourcePath != null) {
                try (
                        InputStreamReader reader = new InputStreamReader(FileUtil.class.getClassLoader().getResourceAsStream(resourcePath));
                        BufferedReader in = new BufferedReader(reader);
                        FileWriter writer = new FileWriter(fileOnDisk);
                        BufferedWriter out = new BufferedWriter(writer)
                ) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        out.write(line + System.lineSeparator());
                    }
                }
            } else {
                Files.createFile(fileOnDisk.toPath());
            }
        }

        return fileOnDisk;
    }

    public static File getCWD() { return new File(ClassLoader.getSystemClassLoader().getResource(".").getPath()); }
}
