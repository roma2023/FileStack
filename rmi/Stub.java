package rmi;

import java.net.*;
import java.lang.reflect.Proxy;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.*;

/**
 * RMI stub factory.
 * 
 * <p>
 * RMI stubs hide network communication with the remote server and provide a
 * simple object-like interface to their users. This class provides methods for
 * creating stub objects dynamically, when given pre-defined interfaces.
 * 
 * <p>
 * The network address of the remote server is set when a stub is created, and
 * may not be modified afterwards. Two stubs are equal if they implement the
 * same interface and carry the same remote server address - and would
 * therefore connect to the same skeleton. Stubs are serializable.
 */
public abstract class Stub {
    /**
     * Creates a stub, given a skeleton with an assigned adress.
     * 
     * <p>
     * The stub is assigned the address of the skeleton. The skeleton must
     * either have been created with a fixed address, or else it must have
     * already been started.
     * 
     * <p>
     * This method should be used when the stub is created together with the
     * skeleton. The stub may then be transmitted over the network to enable
     * communication with the skeleton.
     * 
     * @param c        A <code>Class</code> object representing the interface
     *                 implemented by the remote object.
     * @param skeleton The skeleton whose network address is to be used.
     * @return The stub created.
     * @throws IllegalStateException If the skeleton has not been assigned an
     *                               address by the user and has not yet been
     *                               started.
     * @throws UnknownHostException  When the skeleton address is a wildcard and
     *                               a port is assigned, but no address can be
     *                               found for the local host.
     * @throws NullPointerException  If any argument is <code>null</code>.
     * @throws Error                 If <code>c</code> does not represent a remote
     *                               interface
     *                               - an interface in which each method is marked
     *                               as throwing
     *                               <code>RMIException</code>, or if an object
     *                               implementing
     *                               this interface cannot be dynamically created.
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> c, Skeleton<T> skeleton) throws UnknownHostException {
        // Check if arguments are null
        if ((c == null) || (skeleton == null))
            throw new NullPointerException("One or more arguments are NULL");

        // Check if c is an interface
        if (c.isInterface() != true)
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

        // Get the network address from the skeleton
        InetSocketAddress serverAddress = skeleton.sock_addr;

        // Check if the network address is null
        if (serverAddress == null)
            throw new IllegalStateException();

        // Create a proxy instance using the StubInvocationHandler
        Object dynamic = Proxy.newProxyInstance(c.getClassLoader(),
                new Class[] { c },
                new StubInvocationHandler(serverAddress));

        return (T) dynamic;
    }

    /**
     * Creates a stub, given a skeleton with an assigned address and a hostname
     * which overrides the skeleton's hostname.
     * 
     * <p>
     * The stub is assigned the port of the skeleton and the given hostname.
     * The skeleton must either have been started with a fixed port, or else
     * it must have been started to receive a system-assigned port, for this
     * method to succeed.
     * 
     * <p>
     * This method should be used when the stub is created together with the
     * skeleton, but firewalls or private networks prevent the system from
     * automatically assigning a valid externally-routable address to the
     * skeleton. In this case, the creator of the stub has the option of
     * obtaining an externally-routable address by other means, and specifying
     * this hostname to this method.
     * 
     * @param c        A <code>Class</code> object representing the interface
     *                 implemented by the remote object.
     * @param skeleton The skeleton whose port is to be used.
     * @param hostname The hostname with which the stub will be created.
     * @return The stub created.
     * @throws IllegalStateException If the skeleton has not been assigned a
     *                               port.
     * @throws NullPointerException  If any argument is <code>null</code>.
     * @throws Error                 If <code>c</code> does not represent a remote
     *                               interface
     *                               - an interface in which each method is marked
     *                               as throwing
     *                               <code>RMIException</code>, or if an object
     *                               implementing
     *                               this interface cannot be dynamically created.
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> c, Skeleton<T> skeleton, String hostname) {
        // Check if arguments are null
        if ((c == null) || (skeleton == null) || (hostname == null))
            throw new NullPointerException("One or more arguments are NULL");

        // Check if c is an interface
        if (c.isInterface() != true)
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

        // Check if the skeleton has been assigned a port
        if (skeleton.port == -1)
            throw new IllegalStateException("Parameter skeleton has not been assigned a port");

        // Create an InetSocketAddress using the given hostname and skeleton's port
        InetSocketAddress serverAddress = new InetSocketAddress(hostname, skeleton.port);

        // Check if the InetSocketAddress has a valid address
        if (serverAddress.getAddress() == null)
            throw new IllegalStateException("Skeleton has not been assigned a port");

        // Create a proxy instance using the StubInvocationHandler
        Object dynamic = Proxy.newProxyInstance(c.getClassLoader(),
                new Class[] { c },
                new StubInvocationHandler(serverAddress));

        return (T) dynamic;
    }

    /**
     * Creates a stub, given the address of a remote server.
     * 
     * <p>
     * This method should be used primarily when bootstrapping RMI. In this
     * case, the server is already running on a remote host but there is
     * not necessarily a direct way to obtain an associated stub.
     * 
     * @param c       A <code>Class</code> object representing the interface
     *                implemented by the remote object.
     * @param address The network address of the remote skeleton.
     * @return The stub created.
     * @throws NullPointerException If any argument is <code>null</code>.
     * @throws Error                If <code>c</code> does not represent a remote
     *                              interface
     *                              - an interface in which each method is marked as
     *                              throwing
     *                              <code>RMIException</code>, or if an object
     *                              implementing
     *                              this interface cannot be dynamically created.
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> c, InetSocketAddress address) {

        // Check if arguments are null
        if ((c == null) || (address == null))
            throw new NullPointerException("One or more arguments are NULL");

        // Check if c is an interface
        if (c.isInterface() != true)
            throw new Error("c is not an interface");

        // Check if the InetSocketAddress has a valid address
        if (address.getAddress() == null)
            throw new NullPointerException("One of the Argument passed is NULL");

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

        // Create a proxy instance using the StubInvocationHandler
        Object dynamic = Proxy.newProxyInstance(c.getClassLoader(),
                new Class[] { c },
                new StubInvocationHandler(address));

        return (T) dynamic;
    }

    /**
     * An invocation handler for RMI stubs.
     *
     * <p>
     * This class provides handling of method invocations on RMI stubs. It allows
     * handling of remote method calls, as well as local method calls like handling
     * equals, hashCode, and toString methods locally.
     */

    private static class StubInvocationHandler implements InvocationHandler, Serializable {
        InetSocketAddress srvrAddress;

        // Constructor that initializes srvrAddress
        public StubInvocationHandler(InetSocketAddress srvrAddress) {
            this.srvrAddress = srvrAddress;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                boolean isRemoteFlag = false;
                Class[] exceptionTypes = method.getExceptionTypes();

                // Check if any exception type is RMIException
                for (Class e : exceptionTypes) {
                    if (e.equals(RMIException.class)) {
                        isRemoteFlag = true;
                    }
                }

                // Invoke a remote method if it's a remote call
                if (isRemoteFlag == true) {
                    return invokeRemote(proxy, method, args);
                }

                // Handle equals, hashCode, and toString methods locally
                if (method.getName().equals("equals")) {
                    return equals(proxy, method, args);
                }

                // Handle hashCode method
                StubInvocationHandler handler = (StubInvocationHandler) Proxy.getInvocationHandler(proxy);
                if (method.getName().equals("hashCode")) {
                    return handler.srvrAddress.hashCode() + proxy.getClass().hashCode();
                }

                // Handle toString method
                if (method.getName().contains("toString")) {
                    return "Remote address: " + this.srvrAddress.toString() +
                            "    ;     Remote class: " + this.getClass().getName() +
                            "    ;     SimpleInterface: " + this.getClass().getInterfaces().toString() + "\n";
                }
                return null;
            } catch (Exception e) {
                throw e;
            }
        }

        // Handle the equals method
        private Object equals(Object proxy, Method method, Object[] args) {
            if (args.length != 1) {
                return Boolean.FALSE;
            }

            Object obj = args[0];
            if (obj == null)
                return Boolean.FALSE;

            // Check proxy class and class type
            if (!Proxy.isProxyClass(obj.getClass()) || (!proxy.getClass().equals(obj.getClass())))
                return Boolean.FALSE;

            // Check handler type and remote address
            InvocationHandler handler = Proxy.getInvocationHandler(obj);
            if (!(handler instanceof StubInvocationHandler)
                    || (!srvrAddress.equals(((StubInvocationHandler) handler).srvrAddress)))
                return Boolean.FALSE;

            return Boolean.TRUE;
        }

        // Invoke a remote method
        private Object invokeRemote(Object proxy, Method method, Object[] args) throws Throwable {
            Socket socket = null;
            Integer result = null;
            Object obj = null;
            ObjectOutputStream out = null;
            ObjectInputStream in = null;

            try {
                // Create a socket and connect to the remote server
                socket = new Socket();
                socket.connect(srvrAddress);

                // Initialize ObjectOutputStream to send data
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();

                // Write method name, arguments, and parameter types to the output stream
                out.writeObject(method.getName());
                out.writeObject(args);
                out.writeObject(method.getParameterTypes());

                // Initialize ObjectInputStream to receive data from the server
                in = new ObjectInputStream(socket.getInputStream());
                obj = in.readObject();

                // Close the socket
                socket.close();
            } catch (Exception e) {
                // Print the stack trace and throw a custom RMIException
                e.printStackTrace();
                throw new RMIException(e);
            }

            // Check if the received object is an InvocationTargetException
            if (obj instanceof InvocationTargetException) {
                Throwable ob = ((InvocationTargetException) obj).getTargetException();
                throw ob;
            } else {
                // Close the socket, ObjectOutputStream, ObjectInputStream, and return the
                // object
                socket.close();
                out.close();
                in.close();
                return obj;
            }
        }

    }
}