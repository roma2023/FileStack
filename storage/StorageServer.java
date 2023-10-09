package storage;

import java.io.*;
import java.net.*;

import common.*;
import rmi.*;
import naming.*;

/**
 * Storage server.
 * 
 * <p>
 * Storage servers respond to client file access requests. The files accessible
 * through a storage server are those accessible under a given directory of the
 * local filesystem.
 */
public class StorageServer implements Storage, Command {

    // Skeleton for Storage interface
    Skeleton<Storage> storageSkeleton;

    // Skeleton for Command interface
    Skeleton<Command> commandSkeleton;

    // Port for client communication
    private static int clientPort = 15440;

    // Port for command communication
    private static int commandPort = 15213;

    // Flag indicating whether to create directories
    private boolean shouldMakeDirectory;

    // Root directory for the storage server
    private File rootDirectory;

    /**
     * Creates a storage server, given a directory on the local filesystem.
     * 
     * @param root Directory on the local filesystem. The contents of this
     *             directory will be accessible through the storage server.
     * @throws NullPointerException If <code>root</code> is <code>null</code>.
     */
    public StorageServer(File root) {

        // Check if the root directory is null
        if (root == null)
            throw new NullPointerException("Root directory cannot be null");

        // Set the root directory
        this.rootDirectory = root;
    }

    /**
     * Starts the storage server and registers it with the given naming
     * server.
     * 
     * @param hostname      The externally-routable hostname of the local host on
     *                      which the storage server is running. This is used to
     *                      ensure that the stub which is provided to the naming
     *                      server by the <code>start</code> method carries the
     *                      externally visible hostname or address of this storage
     *                      server.
     * @param naming_server Remote interface for the naming server with which
     *                      the storage server is to register.
     * @throws UnknownHostException  If a stub cannot be created for the storage
     *                               server because a valid address has not been
     *                               assigned.
     * @throws FileNotFoundException If the directory with which the server was
     *                               created does not exist or is in fact a
     *                               file.
     * @throws RMIException          If the storage server cannot be started, or if
     *                               it
     *                               cannot be registered.
     */
    /**
     * Start the StorageServer with the provided hostname and naming server.
     *
     * @param hostname      The hostname to use for storage and command addresses.
     * @param naming_server The registration naming server.
     * @throws RMIException          if there's an RMI-related exception.
     * @throws UnknownHostException  if the hostname is unknown.
     * @throws FileNotFoundException if the root directory is not found.
     * @throws NullPointerException  if the hostname or naming_server is null.
     */
    public synchronized void start(String hostname, Registration naming_server)
            throws RMIException, UnknownHostException, FileNotFoundException {
        // Check if the root directory exists
        if (!this.rootDirectory.exists())
            throw new FileNotFoundException("Root directory not found");

        // Check if the hostname or naming_server is null
        if (hostname == null || naming_server == null)
            throw new NullPointerException("Hostname or naming server is null");

        InetSocketAddress storageAddress = null;
        InetSocketAddress commandAddress = null;

        // Create InetSocketAddress for storage and command
        storageAddress = new InetSocketAddress(hostname, clientPort++);
        commandAddress = new InetSocketAddress(hostname, commandPort++);

        // Create skeletons for Storage and Command
        this.storageSkeleton = new Skeleton<Storage>(Storage.class, this, storageAddress);
        this.commandSkeleton = new Skeleton<Command>(Command.class, this, commandAddress);

        // Create stubs for Storage and Command
        Storage store = Stub.create(Storage.class, storageAddress);
        Command cmd = Stub.create(Command.class, commandAddress);

        // Start skeletons for Storage and Command
        this.storageSkeleton.start();
        this.commandSkeleton.start();

        Path[] nameserver_registered = null;
        try {
            // Register with the naming server and obtain registered paths
            nameserver_registered = naming_server.register(store, cmd, Path.list(rootDirectory));
        } catch (RMIException e) {
            e.printStackTrace(); // Print stack trace if there's an RMI exception
        }

        // Delete the registered paths
        for (Path p : nameserver_registered) {
            this.delete(p);
        }

        // Delete empty directories
        removeRecursively(rootDirectory);
    }

    /**
     * Recursively remove empty directories starting from the specified root
     * directory.
     *
     * @param startDirectory The directory to start removing from.
     */
    private void removeRecursively(File startDirectory) {
        // If it's a file, return as we only deal with directories
        if (startDirectory.isFile())
            return;

        // If the directory has contents
        if (startDirectory.list().length > 0) {
            // Recur for each file/directory in this directory
            for (File file : startDirectory.listFiles())
                removeRecursively(file);
        }

        // If the directory is empty, delete it
        if (startDirectory.list().length == 0)
            startDirectory.delete();
    }

    /**
     * Stops the storage server.
     * 
     * <p>
     * The server should not be restarted.
     */
    public void stop() {
        try {
            // Stop the command skeleton
            this.commandSkeleton.stop();

            // Stop the storage skeleton
            this.storageSkeleton.stop();

            // Call the 'stopped' method with no error
            stopped(null);
        } catch (Exception e) {
            // Handle any exceptions silently
        }
    }

    /**
     * Called when the storage server has shut down.
     * 
     * @param cause The cause for the shutdown, if any, or <code>null</code> if
     *              the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause) {
        // NOTHING HERE....
    }

    // The following methods are documented in Storage.java.
    /**
     * Get the size of the file specified by the given path.
     *
     * @param file The path to the file.
     * @return The size of the file in bytes.
     * @throws FileNotFoundException If the file does not exist or the path refers
     *                               to a directory.
     */
    public synchronized long size(Path file) throws FileNotFoundException {
        // Check if the input path is null
        if (file == null)
            throw new NullPointerException("Path cannot be null");

        // Convert the path to a File object using the root directory
        File f = file.toFile(this.rootDirectory);

        // Check if the file exists
        if (!f.exists())
            throw new FileNotFoundException("File not found.");

        // Check if the path refers to a directory
        if (f.isDirectory())
            throw new FileNotFoundException("Path is a directory, not a file.");

        // Return the size of the file in bytes
        return f.length();
    }

    @Override
    /**
     * Read a specified portion of a file starting from the given offset.
     *
     * @param file   The path to the file.
     * @param offset The offset within the file to start reading from.
     * @param length The number of bytes to read.
     * @return The bytes read from the file.
     * @throws FileNotFoundException     If the file does not exist or the path
     *                                   refers to a directory.
     * @throws IOException               If an I/O error occurs while reading the
     *                                   file.
     * @throws IndexOutOfBoundsException If the length and offset exceed the file
     *                                   size or if length or offset is negative.
     */
    public synchronized byte[] read(Path file, long offset, int length)
            throws FileNotFoundException, IOException {
        // Check if the input path is null
        if (file == null)
            throw new NullPointerException("Path cannot be null");

        // Convert the path to a File object using the root directory
        File f = file.toFile(this.rootDirectory);

        // Check if the file exists
        if (!f.exists())
            throw new FileNotFoundException("File is not found.");

        // Check if the path refers to a directory
        if (f.isDirectory())
            throw new FileNotFoundException("Directory path is passed for reading.");

        // Check if length and offset exceed the file size
        if (length + offset > this.size(file))
            throw new IndexOutOfBoundsException("Length and offset exceed the file size");

        // Check if length or offset is negative
        if (length < 0 || offset < 0)
            throw new IndexOutOfBoundsException("Length or offset is negative");

        // Read the specified portion of the file
        RandomAccessFile readfile = new RandomAccessFile(f, "r");
        readfile.seek(offset);

        byte[] b = new byte[length];

        int r;
        try {
            r = readfile.read(b);
        } catch (Exception e) {
            readfile.close();
            throw new IOException("An I/O error occurred while reading the file", e);
        }
        readfile.close();

        // Check if the read length is less than expected
        if (r < length)
            throw new IndexOutOfBoundsException("Length exceeds the file");

        return b;
    }

    @Override
    /**
     * Write data to a file starting from the specified offset.
     *
     * @param file   The path to the file.
     * @param offset The offset within the file to start writing from.
     * @param data   The bytes to be written to the file.
     * @throws FileNotFoundException     If the file does not exist or the path
     *                                   refers to a directory.
     * @throws IOException               If an I/O error occurs while writing to the
     *                                   file.
     * @throws NullPointerException      If the path or data is null.
     * @throws IndexOutOfBoundsException If the offset is negative or if the data
     *                                   length is zero.
     */
    public synchronized void write(Path file, long offset, byte[] data)
            throws FileNotFoundException, IOException {
        // Check if the input path or data is null
        if (file == null || data == null)
            throw new NullPointerException("Path or data is null");

        // Convert the path to a File object using the root directory
        File f = file.toFile(this.rootDirectory);

        // Check if the path refers to a directory
        if (f.isDirectory())
            throw new FileNotFoundException("Directory path is passed for writing.");

        // Check if the file exists
        if (!f.exists())
            throw new FileNotFoundException("File is not found.");

        // Check if the offset is negative
        if (offset < 0)
            throw new IndexOutOfBoundsException("Offset is negative");

        // Check if the data length is zero
        if (data.length == 0)
            return;

        // Write data to the file starting from the specified offset
        RandomAccessFile writefile = new RandomAccessFile(f, "rw");
        writefile.seek(offset);
        try {
            writefile.write(data);
        } catch (Exception e) {
            writefile.close();
            throw new IOException("An I/O error occurred while writing to the file", e);
        }
        writefile.close();
    }

    // The following methods are documented in Command.java.
    @Override
    /**
     * Create a file at the specified path.
     *
     * @param file The path to the file to be created.
     * @return True if the file was successfully created, false otherwise.
     */
    public synchronized boolean create(Path file) {
        // Check if the path represents the root directory
        if (file.isRoot())
            return false;

        // Check if a file or directory already exists at the specified path
        if (file.toFile(rootDirectory).exists()) {
            return false;
        }

        // Delete the parent directory if it's a file
        if (file.parent().toFile(rootDirectory).isFile()) {
            delete(file.parent());
        }

        // Attempt to create the parent directories for the specified file
        boolean makeParentDirs = file.parent().toFile(rootDirectory).mkdirs();

        try {
            // Attempt to create a new file at the specified path
            return file.toFile(rootDirectory).createNewFile();
        } catch (IOException e) {
            // Print the stack trace in case of an IOException
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Delete the file or directory at the specified path.
     *
     * @param path The path to the file or directory to be deleted.
     * @return True if the deletion was successful, false otherwise.
     * @throws NullPointerException if the path is null.
     */
    @Override
    public synchronized boolean delete(Path path) {
        // Check if the path is null
        if (path == null)
            throw new NullPointerException("Null Path Cannot be Deleted");

        // Check if the path is the root directory
        if (path.isRoot())
            return false;

        // Check if the path represents a file and exists
        if (path.toFile(rootDirectory).exists() && !path.toFile(rootDirectory).isDirectory())
            return path.toFile(rootDirectory).delete();

        // Call recursive delete for directories
        return recursiveDelete(path.toFile(rootDirectory));
    }

    /**
     * Recursively delete a file or directory.
     *
     * @param file The file or directory to be deleted.
     * @return True if the deletion was successful, false otherwise.
     */
    private boolean recursiveDelete(File file) {
        // Check if the file or directory exists
        if (!file.exists())
            return false;
        else if (file.isDirectory()) {
            // Recursively delete contents of the directory
            for (File subFile : file.listFiles()) {
                if (!recursiveDelete(subFile))
                    return false;
            }
        }
        // Delete the file or empty directory
        return file.delete();
    }
}
