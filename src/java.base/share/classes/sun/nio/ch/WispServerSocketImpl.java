package sun.nio.ch;

import jdk.internal.access.SharedSecrets;
import jdk.internal.access.WispEngineAccess;

import java.io.IOException;
import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;


// Make a server socket channel be like a socket and yield on block

public class WispServerSocketImpl
{
    private static WispEngineAccess WEA = SharedSecrets.getWispEngineAccess();
    // The channel being adapted
    private ServerSocketChannelImpl ssc = null;

    // Timeout "option" value for accepts
    private volatile int timeout = 0;

    public WispServerSocketImpl() {
    }

    public void bind(SocketAddress local) throws IOException {
        bind(local, 50);
    }

    public void bind(SocketAddress local, int backlog) throws IOException {
        if (local == null)
            local = new InetSocketAddress(0);
        try {
            getChannelImpl().bind(local, backlog);
        } catch (Exception x) {
            Net.translateException(x);
        }
    }

    public InetAddress getInetAddress() {
        if (ssc == null || !ssc.isBound())
            return null;
        return Net.getRevealedLocalAddress(ssc.localAddress()).getAddress();
    }

    public int getLocalPort() {
        if (ssc == null || !ssc.isBound())
            return -1;
        return Net.asInetSocketAddress(ssc.localAddress()).getPort();
    }

    public Socket accept() throws IOException {

        final ServerSocketChannel ch = getChannelImpl();
        try {
            SocketChannel res;

            if (getSoTimeout() > 0) {
                WEA.addTimer(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(getSoTimeout()));
            }

            while ((res = ch.accept()) == null) {
                WEA.registerEvent(ch, SelectionKey.OP_ACCEPT);
                WEA.park(-1);

                if (getSoTimeout() > 0 && WEA.isTimeout()) {
                    throw new SocketTimeoutException("time out");
                }
            }
            res.configureBlocking(false);
            return new Socket(res);

        } catch (Exception x) {
            Net.translateException(x, true);
            return null;
        } finally {
            if (getSoTimeout() > 0) {
                WEA.cancelTimer();
            }
            WEA.unregisterEvent();
        }
    }

    public void close() throws IOException {
        if (ssc != null) {
            ssc.close();
        }
    }

    public ServerSocketChannel getChannel() {
        return ssc;
    }

    public boolean isBound() {
        return ssc != null && ssc.isBound();
    }

    public boolean isClosed() {
        return ssc != null && !ssc.isOpen();
    }

    public void setSoTimeout(int timeout) throws SocketException {
        this.timeout = timeout;
    }

    public int getSoTimeout() throws IOException {
        return timeout;
    }

    public void setReuseAddress(boolean on) throws SocketException {
        try {
            getChannelImpl().setOption(StandardSocketOptions.SO_REUSEADDR, on);
        } catch (IOException x) {
            Net.translateToSocketException(x);
        }
    }

    public boolean getReuseAddress() throws SocketException {
        try {
            return getChannelImpl().getOption(StandardSocketOptions.SO_REUSEADDR);
        } catch (IOException x) {
            Net.translateToSocketException(x);
            return false;       // Never happens
        }
    }

    public String toString() {
        if (!isBound())
            return "ServerSocket[unbound]";
        return "ServerSocket[addr=" + getInetAddress() +
                ",localport=" + getLocalPort()  + "]";
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        if (size <= 0)
            throw new IllegalArgumentException("size can not be 0 or negative");
        try {
            getChannelImpl().setOption(StandardSocketOptions.SO_RCVBUF, size);
        } catch (IOException x) {
            Net.translateToSocketException(x);
        }
    }

    public int getReceiveBufferSize() throws SocketException {
        try {
            return getChannelImpl().getOption(StandardSocketOptions.SO_RCVBUF);
        } catch (IOException x) {
            Net.translateToSocketException(x);
            return -1;          // Never happens
        }
    }

    private ServerSocketChannelImpl getChannelImpl() throws SocketException {
        if (ssc == null) {
            try {
                ssc = (ServerSocketChannelImpl) ServerSocketChannel.open();
                ssc.configureBlocking(false);
            } catch (IOException e) {
                throw new SocketException(e.getMessage());
            }
        }
        return ssc;
    }
}
