package naming;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

import rmi.*;
import common.*;
import storage.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Naming server.
 * 
 * <p>
 * Each instance of the filesystem is centered on a single naming server. The
 * naming server maintains the filesystem directory tree. It does not store any
 * file data - this is done by separate storage servers. The primary purpose of
 * the naming server is to map each file name (path) to the storage server
 * which hosts the file's contents.
 * 
 * <p>
 * The naming server provides two interfaces, <code>Service</code> and
 * <code>Registration</code>, which are accessible through RMI. Storage servers
 * use the <code>Registration</code> interface to inform the naming server of
 * their existence. Clients use the <code>Service</code> interface to perform
 * most filesystem operations. The documentation accompanying these interfaces
 * provides details on the methods supported.
 * 
 * <p>
 * Stubs for accessing the naming server must typically be created by directly
 * specifying the remote network address. To make this possible, the client and
 * registration interfaces are available at well-known ports defined in
 * <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration {

    // ConcurrentHashMap to map Path to Storage
    private ConcurrentHashMap<Path, Storage> pathToStorageMap;

    // ConcurrentHashMap to map Storage to Command
    private ConcurrentHashMap<Storage, Command> storageToCommandMap;

    // ConcurrentHashMap to track the replica count for each Path
    private ConcurrentHashMap<Path, Integer> replicaCountMap;

    // ConcurrentHashMap to map Path to a set of associated Storage nodes
    private ConcurrentHashMap<Path, HashSet<Storage>> replicaStorageMap;

    // HashSet to store Paths representing files
    private HashSet<Path> filePaths;

    // HashSet to store Paths representing directories
    private HashSet<Path> directoryPaths;

    // Skeleton for the Service interface
    private Skeleton<Service> serviceSkeleton;

    // Skeleton for the Registration interface
    private Skeleton<Registration> registrationSkeleton;

    /**
     * Creates the naming server object.
     * 
     * <p>
     * The naming server is not started.
     */

    /**
     * Constructor for the NamingServer class.
     * Initializes the data structures used to store mappings and paths.
     */
    public NamingServer() {
        // Initialize a map to store mappings of paths to storage
        pathToStorageMap = new ConcurrentHashMap<Path, Storage>();

        // Initialize a map to store mappings of storage to commands
        storageToCommandMap = new ConcurrentHashMap<Storage, Command>();

        // Initialize a set to store file paths
        filePaths = new HashSet<Path>();

        // Initialize a set to store directory paths
        directoryPaths = new HashSet<Path>();

        // Initialize a new path object representing the root directory
        Path initialPath = new Path();

        // Add the root directory path to the set of directory paths
        directoryPaths.add(initialPath);

        // Initialize a map to store replica counts for each path
        replicaCountMap = new ConcurrentHashMap<Path, Integer>();

        // Initialize a map to store replica storage for each path
        replicaStorageMap = new ConcurrentHashMap<Path, HashSet<Storage>>();
    }

    /**
     * Starts the naming server.
     * 
     * <p>
     * After this method is called, it is possible to access the client and
     * registration interfaces of the naming server remotely.
     * 
     * @throws RMIException If either of the two skeletons, for the client or
     *                      registration server interfaces, could not be
     *                      started. The user should not attempt to start the
     *                      server again if an exception occurs.
     */
    public synchronized void start() throws RMIException {
        // Create a skeleton for handling registration requests
        this.registrationSkeleton = new Skeleton<Registration>(Registration.class, this,
                new InetSocketAddress("127.0.0.1", NamingStubs.REGISTRATION_PORT));

        // Create a skeleton for handling service requests
        this.serviceSkeleton = new Skeleton<Service>(Service.class, this,
                new InetSocketAddress("127.0.0.1", NamingStubs.SERVICE_PORT));

        // Start the service skeleton to handle service requests
        serviceSkeleton.start();

        // Start the registration skeleton to handle registration requests
        registrationSkeleton.start();
    }

    /**
     * Stops the naming server.
     * 
     * <p>
     * This method waits for both the client and registration interface
     * skeletons to stop. It attempts to interrupt as many of the threads that
     * are executing naming server code as possible. After this method is
     * called, the naming server is no longer accessible remotely. The naming
     * server should not be restarted.
     */
    public void stop() {
        try {
            // Stop the service skeleton
            this.serviceSkeleton.stop();

            // Stop the registration skeleton
            this.registrationSkeleton.stop();

            // Notify that the server has stopped
            this.stopped(null);
        } catch (Exception e) {
            // Notify with the exception if an error occurs during stopping
            this.stopped(e);
        }
    }

    /**
     * Indicates that the server has completely shut down.
     * 
     * <p>
     * This method should be overridden for error reporting and application
     * exit purposes. The default implementation does nothing.
     * 
     * @param cause The cause for the shutdown, or <code>null</code> if the
     *              shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause) {
    }

    // The following methods are documented in Service.java.
    /**
     * Checks if the given path corresponds to a directory.
     *
     * @param path The path to be checked.
     * @return True if the path corresponds to a directory, false if it corresponds
     *         to a file.
     * @throws FileNotFoundException If the path is not found (neither a directory
     *                               nor a file), or if the input path is null.
     */
    public boolean isDirectory(Path path) throws FileNotFoundException {
        // Check if the input path is null
        if (path == null)
            throw new NullPointerException("Path is null");

        // Check if the path is the root
        if (path.isRoot())
            return true;

        // Check if the path is in the set of directory paths
        if (directoryPaths.contains(path)) {
            return true;
        } else if (filePaths.contains(path)) {
            // Check if the path is in the set of file paths
            return false;
        }

        // Throw exception if the path is not found (neither a directory nor a file)
        throw new FileNotFoundException("Path not found, i.e., it is not a directory nor a file");
    }

    /**
     * Retrieves a list of entries (files and subdirectories) in the specified
     * directory.
     *
     * @param directory The directory path.
     * @return An array of entry names in the directory.
     * @throws FileNotFoundException If the directory does not exist or if the input
     *                               path is null.
     */
    @Override
    public String[] list(Path directory) throws FileNotFoundException {
        // Check if the input path is null
        if (directory == null)
            throw new NullPointerException("Path is null");

        boolean isDirectory = false;
        if (directoryPaths.contains(directory))
            isDirectory = true;

        if (isDirectory) {
            // List of subdirectory paths
            ArrayList<Path> subDirPaths = new ArrayList<Path>();
            // List of resulting entry names
            ArrayList<String> resultList = new ArrayList<String>();

            // List subdirectories within the specified directory
            for (Path path : directoryPaths) {
                if (path.isSubpath(directory))
                    subDirPaths.add(path);
            }

            String directoryStr = directory.toString();
            int directoryStrLength = directoryStr.length();

            for (Path subDirPath : subDirPaths) {
                String subDirStr = subDirPath.toString();
                String relativePath = subDirStr.substring(directoryStrLength);

                int numSlashes = 0;
                for (int i = 0; i < relativePath.length(); i++) {
                    if (relativePath.charAt(i) == '/' && i != 0)
                        numSlashes++;
                }

                if (numSlashes < 1 && !relativePath.isEmpty()) {
                    String entryName = subDirPath.toString().substring(directoryStrLength);
                    entryName = entryName.replace("/", "");
                    resultList.add(entryName);
                }
            }

            // List files within the specified directory
            ArrayList<Path> filePathsInDirectory = new ArrayList<Path>();
            for (Path filePath : filePaths) {
                if (filePath.isSubpath(directory))
                    filePathsInDirectory.add(filePath);
            }

            int fileSize = filePathsInDirectory.size();
            for (int i = 0; i < fileSize; i++) {
                String filePathStr = filePathsInDirectory.get(i).toString();
                String relativePath = filePathStr.substring(directoryStrLength);

                int numSlashes = 0;
                for (int j = 0; j < relativePath.length(); j++) {
                    if (relativePath.charAt(j) == '/' && j != 0)
                        numSlashes++;
                }

                if (numSlashes < 1 && !relativePath.isEmpty()) {
                    String entryName = filePathsInDirectory.get(i).toString().substring(directoryStrLength);
                    entryName = entryName.replace("/", "");
                    resultList.add(entryName);
                }
            }

            int resultListSize = resultList.size();
            String[] resultArray = new String[resultListSize];
            for (int i = 0; i < resultListSize; i++) {
                resultArray[i] = resultList.get(i);
            }
            return resultArray;
        }
        throw new FileNotFoundException("Directory does not exist");
    }

    @Override
    /**
     * Creates a file at the specified path.
     *
     * @param file The path to the file.
     * @return True if the file was created successfully, false otherwise.
     * @throws RMIException          If a remote method invocation fails.
     * @throws FileNotFoundException If the parent directory or the file already
     *                               exists.
     */
    public boolean createFile(Path file)
            throws RMIException, FileNotFoundException {
        // Check whether file is null
        if (file == null) {
            throw new NullPointerException();
        }

        if (file.isRoot()) {
            return false;
        }

        // Check whether file already exists
        if (filePaths.contains(file) || directoryPaths.contains(file)) {
            return false;
        }

        // Check whether the parent directory exists
        if (!directoryPaths.contains(file.parent())) {
            throw new FileNotFoundException();
        }

        try {

            // Pick a random server from the set
            Random rand = new Random();
            int serInd = rand.nextInt(storageToCommandMap.size());
            int ind = 0;

            Iterator<Entry<Storage, Command>> it = storageToCommandMap.entrySet().iterator();

            while (it.hasNext()) {

                if (ind == serInd) {
                    Entry<Storage, Command> entry = (Entry<Storage, Command>) it.next();
                    Command sc = (Command) entry.getValue();
                    sc.create(file);
                    pathToStorageMap.put(file, entry.getKey());
                    filePaths.add(file);
                }
                ind++;
            } // end while

            return true;
        } // end try
        catch (RMIException e) {
            throw new RMIException("RMI EXception");
        }
    }

    /**
     * Creates a directory at the specified path.
     *
     * @param directory The path to the directory.
     * @return True if the directory was created successfully, false otherwise.
     * @throws FileNotFoundException If the parent directory or the directory
     *                               already exists as a file.
     */
    public boolean createDirectory(Path directory) throws FileNotFoundException {
        // Check whether directory is null
        if (directory == null) {
            throw new NullPointerException();
        }
        if (directory.isRoot()) {
            return false;
        }

        // Check whether directory already exists as a directory or file
        if (directoryPaths.contains(directory) || filePaths.contains(directory)) {
            return false;
        }

        // Check whether the parent directory exists
        if (!directoryPaths.contains(directory.parent())) {
            throw new FileNotFoundException();
        }

        // Add the directory to the set of directory paths
        directoryPaths.add(directory);

        return true;
    }

    @Override
    /**
     * Deletes the specified file or directory at the given path.
     *
     * @param path The path to the file or directory.
     * @return True if the file or directory was successfully deleted, false
     *         otherwise.
     * @throws FileNotFoundException If the file or directory does not exist.
     */
    public boolean delete(Path path) throws FileNotFoundException {
        if (path == null)
            throw new NullPointerException("Path is null");
        if (path.isRoot())
            return false;
        if (!filePaths.contains(path) && !directoryPaths.contains(path)) {
            throw new FileNotFoundException("Invalid Path");
        }

        if (filePaths.contains(path)) {
            // Deleting a file

            // Get the set of storages where the file is replicated
            HashSet<Storage> storageSet = replicaStorageMap.get(path);
            Iterator<Storage> iterator = storageSet.iterator();
            int count = 0;

            while (iterator.hasNext()) {
                Storage storage = iterator.next();
                Command command = storageToCommandMap.get(storage);

                if (command == null)
                    throw new FileNotFoundException("Command stub is not found");

                try {
                    boolean deleted = command.delete(path);
                    if (deleted)
                        count++;
                } catch (RMIException e) {
                    e.printStackTrace();
                }
            }

            if (count == storageSet.size()) {
                filePaths.remove(path);
                pathToStorageMap.remove(path);
                replicaStorageMap.remove(path);
                replicaCountMap.remove(path);
                return true;
            }
            return false;
        }

        // Deleting a directory

        // Set to store all storages associated with sub-files
        HashSet<Storage> storageSet = new HashSet<Storage>();

        // Find all sub-files and collect associated storages
        Iterator<Path> fileIterator = filePaths.iterator();

        Path subPath;
        while (fileIterator.hasNext()) {
            subPath = fileIterator.next();
            if (subPath.toString().trim().startsWith(path.toString().trim())) {
                if (replicaStorageMap.containsKey(subPath)) {
                    storageSet.addAll(replicaStorageMap.get(subPath));
                } else if (pathToStorageMap.containsKey(subPath)) {
                    storageSet.add(pathToStorageMap.get(subPath));
                }
            }
        }

        Iterator<Storage> storageIterator = storageSet.iterator();

        int checkCount = 0;
        while (storageIterator.hasNext()) {
            Storage dummyStorage = storageIterator.next();
            Command dummyCommand = storageToCommandMap.get(dummyStorage);
            try {
                boolean deleted = dummyCommand.delete(path);
                if (deleted)
                    checkCount++;
            } catch (RMIException e) {
                e.printStackTrace();
            }
        }
        if (checkCount == storageSet.size()) {
            directoryPaths.remove(path);
            return true;
        }

        return false;
    }

    /**
     * Retrieves the storage associated with the specified file path.
     *
     * @param file The path to the file.
     * @return The storage associated with the file.
     * @throws FileNotFoundException If the file does not exist.
     */
    @Override
    public Storage getStorage(Path file) throws FileNotFoundException {
        if (file == null)
            throw new NullPointerException("File path is null");

        // Check if the file path exists in the naming server
        if (pathToStorageMap.get(file) == null)
            throw new FileNotFoundException("File not found in the naming server");

        // Return the storage associated with the file path
        return pathToStorageMap.get(file);
    }

    // The method register is documented in Registration.java.
    @Override
    /**
     * Registers a storage client and its associated command stub, along with the
     * specified files.
     *
     * @param client_stub  The storage client's stub.
     * @param command_stub The command stub for communicating with the client.
     * @param files        The files to be registered.
     * @return An array of paths representing duplicate files that were not
     *         registered.
     */
    public Path[] register(Storage client_stub, Command command_stub, Path[] files) {
        // Check for null parameters
        if (command_stub == null)
            throw new NullPointerException("Command stub cannot be null");
        if (client_stub == null)
            throw new NullPointerException("Storage client stub cannot be null");
        if (files == null)
            throw new NullPointerException("Files array cannot be null");

        // Check for duplicate registration
        if (storageToCommandMap.containsKey(client_stub))
            throw new IllegalStateException("Duplicate registration attempt");

        // Add the storage client and command stub to the map
        storageToCommandMap.put(client_stub, command_stub);

        // List to store duplicate paths
        ArrayList<Path> duplicatePathsList = new ArrayList<Path>();
        Path[] duplicatePathsArray;

        for (Path file : files) {
            if (!file.isRoot()) {
                if (filePaths.contains(file) || directoryPaths.contains(file)) {
                    // File or directory already exists
                    duplicatePathsList.add(file);
                } else {
                    // Add the new file or directory
                    Iterator<String> iterator = file.iterator();
                    Path parentPath = new Path();

                    while (iterator.hasNext()) {
                        String name = iterator.next();
                        if (iterator.hasNext()) {
                            Path subPath = new Path(parentPath, name);
                            parentPath = subPath;
                            directoryPaths.add(subPath);
                        }
                    }

                    filePaths.add(file);
                    pathToStorageMap.put(file, client_stub);
                }
            }
        }

        int duplicateCount = duplicatePathsList.size();
        duplicatePathsArray = new Path[duplicateCount];

        for (int i = 0; i < duplicateCount; i++) {
            duplicatePathsArray[i] = duplicatePathsList.get(i);
        }

        return duplicatePathsArray;
    }

}
