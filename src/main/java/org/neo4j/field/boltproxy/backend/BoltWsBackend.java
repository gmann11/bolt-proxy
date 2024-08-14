package org.neo4j.field.boltproxy.backend;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.neo4j.field.boltproxy.handler.BoltWsMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoltWsBackend extends WebSocketClient {
    private static Logger log = LoggerFactory.getLogger(BoltWsBackend.class);
    BoltWsMessageHandler bh = null;

    public BoltWsBackend(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public BoltWsBackend(URI serverURI) {
        super(serverURI);
    }

    public BoltWsBackend(URI serverUri, Map<String, String> httpHeaders) {
        super(serverUri, httpHeaders);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        log.debug("backedn connection opened");
    }

    @Override
    public void onMessage(String message) {
        log.debug("received message from backend: " + message);
    }

    @Override
    public void onMessage(ByteBuffer message) {
        log.debug("received message from backend: " + message.toString());
        bh.processResponse(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info("connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        log.error("error on backend: " + ex);
    }

    public void setHandler(BoltWsMessageHandler h) {
        bh = h;
    }
}
