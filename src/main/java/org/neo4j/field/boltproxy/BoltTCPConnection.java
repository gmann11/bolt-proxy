package org.neo4j.field.boltproxy;

import org.neo4j.field.boltproxy.handler.BoltTCPMessageHandler;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.IteratingCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoltTCPConnection extends AbstractConnection implements Connection.UpgradeTo {
    private final IteratingCallback pipe = new BoltTCPIteratingCallback();
    private Logger log;
    //private final ConcurrentMap<String, Object> context;
    private final ByteBufferPool bufferPool;
    private final ByteBuffer _buffer;
    private Configuration configs;
    private BoltTCPMessageHandler messageHandler;
    private EndPoint _endPoint;

    public BoltTCPConnection(EndPoint endPoint, Executor executor, ByteBufferPool bufferPool, Configuration cfg) {
        super(endPoint, executor);
        log = LoggerFactory.getLogger(BoltTCPConnection.class);
        this.bufferPool = bufferPool;
        //this.context = context;
        _buffer = bufferPool.acquire(getInputBufferSize(), true);
        configs = cfg;
        _endPoint = endPoint;
    }

    @Override
    public void onOpen() {
        super.onOpen();
        fillInterested();
    }

    @Override
    public void onFillable() {
        log.info("onFillable::" + getInputBufferSize());
        log.debug("pip succceeded:" + pipe.isSucceeded());
        pipe.iterate();
    }

    @Override
    public void onUpgradeTo(ByteBuffer bb) {
        log.debug("onUpgradeTo Current buffer: {}", BufferUtil.toDetailString(bb));
        messageHandler = new BoltTCPMessageHandler(configs, _endPoint, bufferPool, pipe);  
        messageHandler.sendHandshake(BufferUtil.toArray(bb));
    }
    
    public ByteBufferPool getByteBufferPool() {
        return bufferPool;
    }
    
    //public ConcurrentMap<String, Object> getContext() {
    //    return context;
    //}

    private class BoltTCPIteratingCallback extends IteratingCallback {
        private ByteBuffer buffer = null;

        @Override
        protected Action process() throws Throwable {
            log.info("in process");
            if (buffer == null)
                buffer = BufferUtil.allocate(getInputBufferSize(), true);

            while (true) {
                int filled = getEndPoint().fill(buffer);
                log.debug("filled with: " + filled);
                if (filled > 0) {
                        log.debug("received client data - handling");
                        messageHandler.sendMessage(BufferUtil.toArray(buffer));
                        return Action.SCHEDULED;
                }
                else if (filled == 0) {
                    buffer = null;
                    fillInterested();
                    return Action.IDLE;
                } else {
                    log.debug("action succeeded");
                    return Action.SUCCEEDED;
                }
            }
        }

        @Override
        protected void onCompleteSuccess() {
            log.debug("oncomplete success");
            getEndPoint().close();
        }

        @Override
        protected void onCompleteFailure(Throwable cause) {
            log.error("error in client connection:", cause);
            getEndPoint().close(cause);
        }
    }
}
