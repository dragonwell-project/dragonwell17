package sun.nio.ch;

import com.alibaba.wisp.util.io.WispInputStream;
import com.alibaba.wisp.util.io.WispOutputStream;
import jdk.internal.access.SharedSecrets;
import jdk.internal.access.WispEngineAccess;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.TimeUnit;


// Make a socket channel be like a socket and yield on block

public class WispSocketImpl
{
    private static WispEngineAccess WEA = SharedSecrets.getWispEngineAccess();
    // The channel being adapted
    private SocketChannelImpl sc = null;
    // 1 verse 1 related socket
    private Socket so;
    // Timeout "option" value for reads
    protected int timeout = 0;
    private InputStream socketInputStream = null;

    public WispSocketImpl(Socket so) {
        this.so = so;
    }

    public WispSocketImpl(SocketChannel sc, Socket so) {
        this.so = so;
        this.sc = (SocketChannelImpl) sc;
    }

    public SocketChannel getChannel() {
        return sc;
    }

    // Override this method just to protect against changes in the superclass
    //
    public void connect(SocketAddress remote) throws IOException {
        connect(remote, 0);
    }

    public void connect(SocketAddress remote, int timeout) throws IOException {
        if (remote == null)
            throw new IllegalArgumentException("connect: The address can't be null");
        if (timeout < 0)
            throw new IllegalArgumentException("connect: timeout can't be negative");

        final SocketChannel ch = getChannelImpl();
        try {
            if (ch.connect(remote)) return;

            if (timeout > 0)
                WEA.addTimer(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout));

            do {
                WEA.registerEvent(ch, SelectionKey.OP_CONNECT);
                WEA.park(-1);

                if (timeout > 0 && WEA.isTimeout()) {
                    throw new SocketTimeoutException("time out");
                }
            } while (!ch.finishConnect());

        } catch (Exception x) {
            // see AbstractPlainSocketImpl#doConnect
            try {
                Net.translateException(x, true);
            } catch (IOException e) {
                sc.close();
                throw e;
            }
        } finally {
            if (timeout > 0) {
                WEA.cancelTimer();
            }

            if (ch.isBlocking())
                ch.configureBlocking(false);

            WEA.unregisterEvent();
        }
    }

    public void bind(SocketAddress local) throws IOException {
        try {
            getChannelImpl().bind(local);
        } catch (Exception x) {
            Net.translateException(x);
        }
    }

    public InetAddress getInetAddress() {
        SocketAddress remote = sc == null ? null : sc.remoteAddress();
        if (remote == null) {
            return null;
        } else {
            return ((InetSocketAddress)remote).getAddress();
        }
    }

    public InetAddress getLocalAddress() {
        SocketChannelImpl ch = null;
        try {
            ch = getChannelImpl();
        } catch (SocketException e) {
            // return 0.0.0.0
        }
        if (ch != null && ch.isOpen()) {
            SocketAddress local = ch.localAddress();
            if (local != null) {
                return Net.getRevealedLocalAddress(local).getAddress();
            }
        }
        return new InetSocketAddress(0).getAddress();
    }

    public int getPort() {
        SocketAddress remote = sc == null ? null : sc.remoteAddress();
        if (remote == null) {
            return 0;
        } else {
            return ((InetSocketAddress)remote).getPort();
        }
    }

    public int getLocalPort() {
        SocketChannelImpl ch = null;
        try {
            ch = getChannelImpl();
        } catch (SocketException e) {
            // return 0.0.0.0
        }
        SocketAddress local = ch == null ? null : ch.localAddress();
        if (local == null) {
            return -1;
        } else {
            return ((InetSocketAddress)local).getPort();
        }
    }

    @SuppressWarnings("removal")
    public InputStream getInputStream() throws IOException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        if (!isConnected())
            throw new SocketException("Socket is not connected");
        if (isInputShutdown())
            throw new SocketException("Socket input is shutdown");
        if (socketInputStream == null) {
            try {
                socketInputStream = AccessController.doPrivileged(
                    new PrivilegedExceptionAction<InputStream>() {
                        public InputStream run() throws IOException {
                            return new WispInputStream(getChannelImpl(), so);
                        }
                    });
            } catch (java.security.PrivilegedActionException e) {
                throw (IOException)e.getException();
            }
        }
        return socketInputStream;
    }

    @SuppressWarnings("removal")
    public OutputStream getOutputStream() throws IOException {
        if (isClosed())
            throw new SocketException("Socket is closed");
        if (!isConnected())
            throw new SocketException("Socket is not connected");
        if (isOutputShutdown())
            throw new SocketException("Socket output is shutdown");
        try {
            return AccessController.doPrivileged(
                new PrivilegedExceptionAction<OutputStream>() {
                    public OutputStream run() throws IOException {
                        return new WispOutputStream(getChannelImpl(), so);
                    }
                });
        } catch (java.security.PrivilegedActionException e) {
            throw (IOException)e.getException();
        }
    }

    private void setBooleanOption(SocketOption<Boolean> name, boolean value)
        throws SocketException {
        try {
            getChannelImpl().setOption(name, value);
        } catch (IOException x) {
            Net.translateToSocketException(x);
        }
    }

    private void setIntOption(SocketOption<Integer> name, int value)
        throws SocketException {
        try {
            getChannelImpl().setOption(name, value);
        } catch (IOException x) {
            Net.translateToSocketException(x);
        }
    }

    private boolean getBooleanOption(SocketOption<Boolean> name) throws SocketException {
        try {
            return getChannelImpl().getOption(name);
        } catch (IOException x) {
            Net.translateToSocketException(x);
            return false;       // keep compiler happy
        }
    }

    private int getIntOption(SocketOption<Integer> name) throws SocketException {
        try {
            return getChannelImpl().getOption(name);
        } catch (IOException x) {
            Net.translateToSocketException(x);
            return -1;          // keep compiler happy
        }
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        setBooleanOption(StandardSocketOptions.TCP_NODELAY, on);
    }

    public boolean getTcpNoDelay() throws SocketException {
        return getBooleanOption(StandardSocketOptions.TCP_NODELAY);
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        if (!on)
            linger = -1;
        setIntOption(StandardSocketOptions.SO_LINGER, linger);
    }

    public int getSoLinger() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_LINGER);
    }

    public void sendUrgentData(int data) throws IOException {
        int n = getChannelImpl().sendOutOfBandData((byte) data);
        if (n == 0)
            throw new IOException("Socket buffer full");
    }

    public void setOOBInline(boolean on) throws SocketException {
        setBooleanOption(ExtendedSocketOption.SO_OOBINLINE, on);
    }

    public boolean getOOBInline() throws SocketException {
        return getBooleanOption(ExtendedSocketOption.SO_OOBINLINE);
    }

    public void setSoTimeout(int timeout) throws SocketException {
        if (timeout < 0)
            throw new IllegalArgumentException("timeout can't be negative");
        this.timeout = timeout;
    }

    public int getSoTimeout() throws SocketException {
        return timeout;
    }

    public void setSendBufferSize(int size) throws SocketException {
        // size 0 valid for SocketChannel, invalid for Socket
        if (size <= 0)
            throw new IllegalArgumentException("Invalid send size");
        setIntOption(StandardSocketOptions.SO_SNDBUF, size);
    }

    public int getSendBufferSize() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_SNDBUF);
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        // size 0 valid for SocketChannel, invalid for Socket
        if (size <= 0)
            throw new IllegalArgumentException("Invalid receive size");
        setIntOption(StandardSocketOptions.SO_RCVBUF, size);
    }

    public int getReceiveBufferSize() throws SocketException {
        return getIntOption(StandardSocketOptions.SO_RCVBUF);
    }

    public void setKeepAlive(boolean on) throws SocketException {
        setBooleanOption(StandardSocketOptions.SO_KEEPALIVE, on);
    }

    public boolean getKeepAlive() throws SocketException {
        return getBooleanOption(StandardSocketOptions.SO_KEEPALIVE);
    }

    public void setTrafficClass(int tc) throws SocketException {
        setIntOption(StandardSocketOptions.IP_TOS, tc);
    }

    public int getTrafficClass() throws SocketException {
        return getIntOption(StandardSocketOptions.IP_TOS);
    }

    public void setReuseAddress(boolean on) throws SocketException {
        setBooleanOption(StandardSocketOptions.SO_REUSEADDR, on);
    }

    public boolean getReuseAddress() throws SocketException {
        return getBooleanOption(StandardSocketOptions.SO_REUSEADDR);
    }

    public void close() throws IOException {
        if (sc != null) {
            sc.close();
        }
    }

    public void shutdownInput() throws IOException {
        try {
            getChannelImpl().shutdownInput();
        } catch (Exception x) {
            Net.translateException(x);
        }
    }

    public void shutdownOutput() throws IOException {
        try {
            getChannelImpl().shutdownOutput();
        } catch (Exception x) {
            Net.translateException(x);
        }
    }

    public String toString() {
        if (isConnected())
            return "Socket[addr=" + getInetAddress() +
                ",port=" + getPort() +
                ",localport=" + getLocalPort() + "]";
        return "Socket[unconnected]";
    }

    public boolean isConnected() {
        return sc != null && sc.isConnected();
    }

    public boolean isBound() {
        return sc != null && sc.localAddress() != null;
    }

    public boolean isClosed() {
        return sc != null && !sc.isOpen();
    }

    public boolean isInputShutdown() {
        return sc != null && !sc.isInputOpen();
    }

    public boolean isOutputShutdown() {
        return sc != null && !sc.isOutputOpen();
    }

    private SocketChannelImpl getChannelImpl() throws SocketException {
        if (sc == null) {
            try {
                sc = (SocketChannelImpl) SocketChannel.open();
                sc.configureBlocking(false);
            } catch (IOException e) {
                throw new SocketException(e.getMessage());
            }
        }
        return sc;
    }
}
