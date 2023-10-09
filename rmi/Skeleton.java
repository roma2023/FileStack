package rmi;

import java.net.*;
import java.lang.reflect.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * RMI skeleton
 * 
 * <p>
 * A skeleton encapsulates a multithreaded TCP server. The server's clients are
 * intended to be RMI stubs created using the <code>Stub</code> class.
 * 
 * <p>
 * The skeleton class is parametrized by a type variable. This type variable
 * should be instantiated with an interface. The skeleton will accept from the
 * stub requests for calls to the methods of this interface. It will then
 * forward those requests to an object. The object is specified when the
 * skeleton is constructed, and must implement the remote interface. Each
 * method in the interface should be marked as throwing
 * <code>RMIException</code>, in addition to any other exceptions that the user
 * desires.
 * 
 * <p>
 * Exceptions may occur at the top level in the listening and service threads.
 * The skeleton's response to these exceptions can be customized by deriving
 * a class from <code>Skeleton</code> and overriding <code>listen_error</code>
 * or <code>service_error</code>.
 */
public class Skeleton<T> {

    // The server socket for listening to client requests
    public ServerSocket listeningSocket = null;

    // The socket address
    public InetSocketAddress sock_addr = null;

    // The class representing the server's interface
    public Class<T> sclass = null;

    // The thread for handling incoming client connections
    public listeningThreads serverthread = null;

    // Flag indicating whether the server is started
    public boolean isServerStarted = false;

    // The network address
    InetSocketAddress address;

    // The port for the server
    public int port = -1;

    // The server instance
    public T srvr = null;

    /**
     * Creates a <code>Skeleton</code> with no initial server address. The
     * address will be determined by the system when <code>start</code> is
     * called. Equivalent to using <code>Skeleton(null)</code>.
     * 
     * <p>
     * This constructor is for skeletons that will not be used for
     * bootstrapping RMI - those that therefore do not require a well-known
     * port.
     * 
     * @param c      An object representing the class of the interface for which the
     *               skeleton server is to handle method call requests.
     * @param server An object implementing said interface. Requests for method
     *               calls are forwarded by the skeleton to this object.
     * @throws Error                If <code>c</code> does not represent a remote
     *                              interface -
     *                              an interface whose methods are all marked as
     *                              throwing
     *                              <code>RMIException</code>.
     * @throws NullPointerException If either of <code>c</code> or
     *                              <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server) {
        this.address = null;

        // Check if the arguments are null
        if (c == null || server == null)
            throw new NullPointerException("c or server is null");

        // Check if c is an interface
        if (!c.isInterface())
            throw new Error("c is not an interface");

        // Get all the methods for the provided class
        Method[] methods = c.getMethods();

        // Iterate through each method
        for (Method method : methods) {
            // Get the exception types for the current method
            Class<?>[] exceptionTypes = method.getExceptionTypes();
            // Flag to check if RMIException is found for the method
            boolean hasRMIException = false;

            // Check if any exception type contains "RMIException"
            for (Class<?> exceptionType : exceptionTypes) {
                if (exceptionType.getName().contains("RMIException")) {
                    hasRMIException = true;
                    break;
                }
            }

            // If the method doesn't throw RMIException, throw an error
            if (!hasRMIException)
                throw new Error("Method does not throw RMIException");
        }

        // Initialize class and server variables
        this.sclass = c;
        this.srvr = server;
    }

    /**
     * Creates a <code>Skeleton</code> with the given initial server address.
     * 
     * <p>
     * This constructor should be used when the port number is significant.
     * 
     * @param c       An object representing the class of the interface for which
     *                the
     *                skeleton server is to handle method call requests.
     * @param server  An object implementing said interface. Requests for method
     *                calls are forwarded by the skeleton to this object.
     * @param address The address at which the skeleton is to run. If
     *                <code>null</code>, the address will be chosen by the
     *                system when <code>start</code> is called.
     * @throws Error                If <code>c</code> does not represent a remote
     *                              interface -
     *                              an interface whose methods are all marked as
     *                              throwing
     *                              <code>RMIException</code>.
     * @throws NullPointerException If either of <code>c</code> or
     *                              <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server, InetSocketAddress address) {
        this.address = address;

        // Check if the arguments are null
        if (c == null || server == null)
            throw new NullPointerException("c or server is null");

        // Check if c is an interface
        if (!c.isInterface())
            throw new Error("c is not an interface");

        // Get all the methods for the provided class
        Method[] methods = c.getMethods();

        // Iterate through each method
        for (Method method : methods) {
            // Get the exception types for the current method
            Class<?>[] exceptionTypes = method.getExceptionTypes();
            // Flag to check if RMIException is found for the method
            boolean hasRMIException = false;

            // Check if any exception type contains "RMIException"
            for (Class<?> exceptionType : exceptionTypes) {
                if (exceptionType.getName().contains("RMIException")) {
                    hasRMIException = true;
                    break;
                }
            }

            // If the method doesn't throw RMIException, throw an error
            if (!hasRMIException)
                throw new Error("Method does not throw RMIException");
        }

        // Initialize class and server variables
        this.sclass = c;
        this.srvr = server;

        // Set the socket address and port if address is not null
        if (address != null) {
            this.sock_addr = address;
            port = sock_addr.getPort();
        }
    }

    // Method to get the address - Helper function
    InetSocketAddress getAddress() {
        return this.address;
    }

    /**
     * Called when the listening thread exits.
     * 
     * <p>
     * The listening thread may exit due to a top-level exception, or due to a
     * call to <code>stop</code>.
     * 
     * <p>
     * When this method is called, the calling thread owns the lock on the
     * <code>Skeleton</code> object. Care must be taken to avoid deadlocks when
     * calling <code>start</code> or <code>stop</code> from different threads
     * during this call.
     * 
     * <p>
     * The default implementation does nothing.
     * 
     * @param cause The exception that stopped the skeleton, or
     *              <code>null</code> if the skeleton stopped normally.
     */
    protected void stopped(Throwable cause) {
        // NOTHING SO FARRRRRR
    }

    /**
     * Called when an exception occurs at the top level in the listening
     * thread.
     * 
     * <p>
     * The intent of this method is to allow the user to report exceptions in
     * the listening thread to another thread, by a mechanism of the user's
     * choosing. The user may also ignore the exceptions. The default
     * implementation simply stops the server. The user should not use this
     * method to stop the skeleton. The exception will again be provided as the
     * argument to <code>stopped</code>, which will be called later.
     * 
     * @param exception The exception that occurred.
     * @return <code>true</code> if the server is to resume accepting
     *         connections, <code>false</code> if the server is to shut down.
     */
    protected boolean listen_error(Exception exception) {
        // Nothing here
        return false;
    }

    /**
     * Called when an exception occurs at the top level in a service thread.
     * 
     * <p>
     * The default implementation does nothing.
     * 
     * @param exception The exception that occurred.
     */

    protected void service_error(RMIException exception) {

        // Throws the provided RMIException
        try {
            throw exception;

        } // Catch and print the stack trace of the RMIException
        catch (RMIException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the skeleton server.
     * 
     * <p>
     * A thread is created to listen for connection requests, and the method
     * returns immediately. Additional threads are created when connections are
     * accepted. The network address used for the server is determined by which
     * constructor was used to create the <code>Skeleton</code> object.
     * 
     * @throws RMIException When the listening socket cannot be created or
     *                      bound, when the listening thread cannot be created,
     *                      or when the server has already been started and has
     *                      not since stopped.
     */
    public synchronized void start() throws RMIException {

        if (isServerStarted == true)
            throw new RMIException("Server is already Running");

        try {
            // Check if the socket address is null
            if (sock_addr == null) {
                // If null, create a new ServerSocket with a system-assigned port
                listeningSocket = new ServerSocket(0);
                port = listeningSocket.getLocalPort();
                sock_addr = new InetSocketAddress(port);

            } else {
                // If not null, create a ServerSocket with the specified port
                listeningSocket = new ServerSocket(port);
            }

            // Start a new thread to listen for incoming client requests
            serverthread = new listeningThreads();
            serverthread.start();

        } catch (Exception e) {
            // Handle any exceptions by invoking the service_error method with a null
            // argument
            service_error(null);
        }
    }

    /**
     * A thread responsible for listening to incoming client socket connections and
     * managing communication with clients.
     *
     * This thread continuously accepts client socket connections while the server
     * is running.
     * For each accepted client socket, it creates a new instance of
     * {@link SingleClient}
     * and starts a new thread to handle communication with that client.
     */

    public class listeningThreads extends Thread implements Runnable, Serializable {
        public void run() {

            // Set the server status to started
            isServerStarted = true;

            // Continue running while the server is started
            while (isServerStarted) {
                try {
                    // Accept incoming client socket connections
                    Socket ClientSocket = listeningSocket.accept();

                    // Create a SingleClient instance for the accepted client socket
                    ClientHandler newSClient = new ClientHandler(ClientSocket);

                    // Start a new thread to handle communication with the client
                    Thread ClientThread = new Thread(newSClient);
                    ClientThread.start();

                } catch (Exception e) {
                    // Handle any exceptions by invoking the service_error method with a null
                    // argument
                    service_error(null);
                }
            }
        }
    }

    /**
     * A thread responsible for handling communication with a single client.
     *
     * This thread manages communication with a client by setting up input and
     * output streams,
     * reading method name, parameters, and parameter types from the input stream,
     * invoking
     * the corresponding method on the server object, and sending the result back to
     * the client.
     */

    public class ClientHandler implements Runnable, Serializable {

        // Client socket for communication
        Socket ClientSocket = null;

        // Input and output streams for communication with the client
        ObjectInputStream in = null;
        ObjectOutputStream out = null;

        // Method-related information
        String methodName;
        Object[] params;
        Class<?>[] paramTypes;
        Object result, err = null;

        /**
         * Constructor to initialize the ClientHandler with a client socket.
         * 
         * @param ClientSocket The client socket for communication.
         */
        public ClientHandler(Socket ClientSocket) {
            this.ClientSocket = ClientSocket;
        }

        /**
         * Run method that executes when a thread starts.
         */
        public void run() {
            try {

                // Set up input and output streams for communication with the client
                in = new ObjectInputStream(ClientSocket.getInputStream());
                out = new ObjectOutputStream(ClientSocket.getOutputStream());

                // Read method name, parameters, and parameter types from the input stream
                methodName = (String) in.readObject();
                params = (Object[]) in.readObject();
                paramTypes = (Class<?>[]) in.readObject();

                Method method = null;
                try {
                    // Get the method object based on the method name and parameter types
                    method = sclass.getMethod(methodName, paramTypes);
                } catch (Exception e) {
                    // Handle any exceptions and invoke service_error method
                    out.close();
                    in.close();
                    ClientSocket.close();
                    service_error(null);
                }

                try {
                    // Invoke the method on the server object with the specified parameters
                    result = method.invoke(srvr, params);
                } catch (InvocationTargetException e) {
                    // If an exception occurred during method invocation, store it in 'result'
                    result = (Object) e;
                }

                // Send the result back to the client
                out.writeObject(result);
                out.close();
                in.close();
                ClientSocket.close();
            } catch (Exception e) {
                // Handle any exceptions and invoke service_error method
                service_error(null);
            }
        }
    }

    // Other methods in the Client Handler class...
    public void checkServer() {
        if (serverthread != null) {
            try {
                // Wait for the server thread to finish its execution
                serverthread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            serverthread = null;
        }
    }

    /**
     * Stops the skeleton server, if it is already running.
     * 
     * <p>
     * The listening thread terminates. Threads created to service connections
     * may continue running until their invocations of the <code>service</code>
     * method return. The server stops at some later time; the method
     * <code>stopped</code> is called at that point. The server may then be
     * restarted.
     * 
     * @throws InterruptedException
     * @throws IOException
     */

    public synchronized void stop() {
        // Set the flag to indicate that the server is no longer started
        isServerStarted = false;
        try {
            // Close the listening socket to stop accepting new client connections
            listeningSocket.close();
            // Invoke the stopped method to handle server stopping
            stopped(null);
            // Check the server status and wait for the server thread to finish its
            // execution
            checkServer();
        } catch (IOException e) {
            // Handle any IO-related exceptions
            e.printStackTrace();
        }
    }
}
