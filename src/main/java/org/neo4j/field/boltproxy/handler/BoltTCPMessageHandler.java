package org.neo4j.field.boltproxy.handler;

import org.neo4j.field.boltproxy.backend.BoltTCPBackend;
import java.net.URI;
import java.nio.ByteBuffer;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.neo4j.field.boltproxy.Utils;

public class BoltTCPMessageHandler extends MessageHandler {
    protected BoltTCPBackend bp;
    protected EndPoint endPoint;
    protected Callback callback;
    
    public BoltTCPMessageHandler(Configuration cfg, EndPoint ep, ByteBufferPool p, Callback cb) {
        super(cfg);
        endPoint = ep;
        callback = cb;
        log.debug("Creating new backend tcp connection");
        try {
            this.remoteAddress = ep.getRemoteSocketAddress();
            this.localAddress = ep.getLocalSocketAddress();
            URI be = new URI(configs.getString("boltproxy.backend.bolt", "ws://localhost:7687"));
            bp = new BoltTCPBackend(be.getHost(), be.getPort());
            bp.setHandler(this);
            bp.setPool(p);
            bp.connect();
        } catch (Exception e) {
            log.error("Exception creating backend:", e);
        }
    }
    
    public void processHandshakeResponse(ByteBuffer b) {
        byte[] ba;
        if (b.hasArray() == false) {
            ba = new byte[b.limit()];
            b.get(ba);           
        } else {
            ba = b.array();
        }
        log.info("Preparing message to client: " + Utils.byteArrayToHex(ba));
        inspectMessage(ba);
        log.debug("Endpoint:" + endPoint.isOpen());
        ByteBuffer ts = BufferUtil.toBuffer(ba);
        try {
            log.debug("flushing::" + Utils.byteArrayToHex(BufferUtil.toArray(ts)));
            endPoint.write(Callback.NOOP, ts);
            log.debug("written::" + BufferUtil.toDetailString(ts));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void processResponse(ByteBuffer b) {
        byte[] ba;
        if (b.hasArray() == false) {
            ba = new byte[b.limit()];
            b.get(ba);           
        } else {
            ba = b.array();
        }
        log.info("Preparing message to client: " + Utils.byteArrayToHex(ba));
        inspectMessage(ba);
        log.debug("Endpoint:" + endPoint.isOpen());
        ByteBuffer ts = BufferUtil.toBuffer(ba);
        try {
            log.debug("flushing::" + Utils.byteArrayToHex(BufferUtil.toArray(ts)));
            endPoint.write(callback, ts);
            log.debug("written::" + BufferUtil.toDetailString(ts));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void sendHandshake(byte[] b) {
        log.info("Preparing handshake to backend: " + Utils.byteArrayToHex(b));
        bp.send(b, true);
    }
    
    public void sendMessage(byte[] b) {
        log.info("Preparing message to backend: " + Utils.byteArrayToHex(b));
        byte[] lm = inspectMessage(b);
        if (lm != null) {
            log.debug("Sending updated message to backend: " + Utils.byteArrayToHex(lm));
            bp.send(lm, false);
        } else {
            bp.send(b, false);
        }
    }
}
