package org.angularjsupgrader.service;

import org.angularjsupgrader.exception.UpgraderException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Philip Harris on 22/01/2020
 */
public class DirectoryFileListerServiceImpl {

    public List<String> listJsFilesInDirectory(final String directory) throws UpgraderException {
        String directoryPath = getClass().getClassLoader().getResource(directory).getPath();
        File folder = new File(directoryPath);

        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            return paths.map(Path::toString)
                    .filter(filepath -> filepath.endsWith(".js"))
                    .map(jsFilePath -> jsFilePath.replace(folder.toString(), "")) // Make the path relative
                    .map(relativePath -> relativePath.replace("\\", "/")) // Get it to work on windows
                    .map(startingWithForwardSlash -> startingWithForwardSlash.substring(1)) // Remove the starting '/'
                    .map(withoutDir -> directory + withoutDir)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UpgraderException(e);
        }
    }

}
