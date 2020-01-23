package org.angularjsupgrader.service;

import org.angularjsupgrader.exception.UpgraderException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Philip Harris on 22/01/2020
 */
public class DirectoryFileListerServiceImpl {

    public List<String> listJsFilesInDirectory(String directory) throws UpgraderException {
        String directoryPath = getClass().getClassLoader().getResource(directory).getPath();
        File file = new File(directoryPath);
        try (Stream<Path> paths = Files.walk(file.toPath())) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UpgraderException(e);
        }
    }

}
