package org.neo4j.field.boltproxy;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoltBackend extends WebSocketClient {
    private static Logger log = LoggerFactory.getLogger(BoltBackend.class);
    BoltMessageHandler bh = null;

    public BoltBackend(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public BoltBackend(URI serverURI) {
        super(serverURI);
    }

    public BoltBackend(URI serverUri, Map<String, String> httpHeaders) {
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

    public void setHandler(BoltMessageHandler h) {
        bh = h;
    }
}
