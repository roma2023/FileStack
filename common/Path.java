package common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.StringJoiner;

public class Path implements Iterable<String>, Serializable {

    private final String[] components; // Array to store path components

    // Constructor for root directory
    public Path() {
        this.components = new String[0];
    }

    // Constructor to append a component to an existing path
    public Path(Path path, String component) {
        if (component == null || component.isEmpty() || component.contains("/") || component.contains(":")) {
            throw new IllegalArgumentException("Invalid component for path");
        }

        int length = path.components.length;
        this.components = Arrays.copyOf(path.components, length + 1);
        this.components[length] = component;
    }

    public Path(String path) {
        if (path == null || !path.startsWith("/") || path.contains(":")) {
            throw new IllegalArgumentException("Invalid path string");
        }

        // Split the path string by '/'
        String[] pathComponents = path.split("/");

        // Filter out empty components
        List<String> nonEmptyComponents = new ArrayList<>();
        for (String component : pathComponents) {
            if (!component.isEmpty()) {
                nonEmptyComponents.add(component);
            }
        }

        if (nonEmptyComponents.isEmpty()) {
            // Root path
            this.components = new String[0];
        } else {
            this.components = nonEmptyComponents.toArray(new String[0]);
        }
    }

    // Iterator to iterate over path components
    @Override
    public Iterator<String> iterator() {
        return Arrays.asList(components).iterator();
    }

    // Method to list paths of all files in a directory
    public static Path[] list(File directory) throws FileNotFoundException {
        if (!directory.isDirectory())
            throw new IllegalArgumentException("Not a directory");

        List<Path> paths = new ArrayList<>();
        traverseDirectory(directory, paths, "");

        return paths.toArray(new Path[0]);
    }

    private static void traverseDirectory(File directory, List<Path> paths, String currentPath) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                String filePath = currentPath + "/" + file.getName();

                if (file.isDirectory()) {
                    traverseDirectory(file, paths, filePath);
                } else {
                    // Construct a Path object for the file
                    paths.add(new Path(filePath));
                }
            }
        }
    }

    // Check if the path represents the root directory
    public boolean isRoot() {
        return components.length == 0;
    }

    // Get the parent path
    public Path parent() {
        if (this.isRoot()) {
            throw new IllegalArgumentException("Root directory has no parent.");
        }

        // Remove the last component to get the parent path
        String[] parentComponents = Arrays.copyOfRange(components, 0, components.length - 1);
        return new Path("/" + String.join("/", parentComponents));
    }

    // Get the last component of the path
    public String last() {
        if (components.length == 0) {
            throw new IllegalArgumentException("Path represents root directory");
        }

        return components[components.length - 1];
    }

    // Check if the given path is a subpath of this path
    public boolean isSubpath(Path other) {
        String[] otherComponents = other.components;
        int length = otherComponents.length;

        // Check if they are the same path
        if (other.toString().equals(this.toString()))
            return true;

        // Check if the other path is longer or equal to this path
        if (length >= this.components.length)
            return false;

        // Check if each component matches
        for (int i = 0; i < length; i++) {
            if (!this.components[i].equals(otherComponents[i]))
                return false;
        }

        return true;
    }

    // Convert the path to a File object
    public File toFile(File root) {
        File file = root;
        for (String component : components) {
            file = new File(file, component);
        }
        return file;
    }

    // Override equals() method to compare paths for equality
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        Path path = (Path) other;
        return Arrays.equals(components, path.components);
    }

    // Override hashCode() method
    @Override
    public int hashCode() {
        return Arrays.hashCode(components);
    }

    // Override toString() method to convert the path to a string
    @Override
    public String toString() {
        if (components.length == 0) {
            return "/";
        }

        StringBuilder pathString = new StringBuilder();

        for (String component : components) {
            if (!component.isEmpty()) {

                // Append the non-empty component
                pathString.append("/" + component);
            }
        }

        return pathString.toString();
    }
}
