package org.angularjsupgrader.service;

import org.angularjsupgrader.exception.UpgraderException;
import org.angularjsupgrader.model.UpgraderProperties;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Philip Harris on 22/01/2020
 */
public class FileListerServiceImpl {

    public List<String> listJsFilesInDirectory(final String directory) throws UpgraderException {
        final String directoryPath = getClass().getClassLoader().getResource(directory).getPath();
        final File folder = new File(directoryPath);

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

    public String getFileMatchingPath(String filename) throws UpgraderException {
        final URL resource = getClass().getClassLoader().getResource(filename);
        if (resource == null) {
            System.out.println("ERROR: could not find '" + filename + "' in resources folder. Did you set the " + UpgraderProperties.TEMPLATE_PUBLIC_URL_ROOT_PROPERTY + " property in resources/application.properties?");
            return null;
        }
        try {
            final File file = new File(resource.getFile());
            return new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new UpgraderException("Failed to read " + filename, e);
        }
    }

}
