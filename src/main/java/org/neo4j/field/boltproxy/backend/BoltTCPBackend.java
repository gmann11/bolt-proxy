package org.neo4j.field.boltproxy.backend;

import com.github.simplenet.Client;
import com.github.simplenet.packet.Packet;
import java.nio.ByteBuffer;
import java.util.HexFormat;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;
import org.neo4j.field.boltproxy.handler.BoltTCPMessageHandler;
import org.neo4j.field.boltproxy.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoltTCPBackend {
    private String ip;
    private int port;
    private boolean connected = false;
    private Logger log;
    private Client client;
    private BoltTCPMessageHandler handler;
    private ByteBufferPool pool;
    
    public BoltTCPBackend (String i, int p) {
        ip = i;
        port = p;
        log = LoggerFactory.getLogger(BoltTCPBackend.class);
        client = new Client();
    }
    
    public void setup() {
        client.onConnect(() -> {
            log.debug("Client {} connected to server", client);
        });
        client.preDisconnect(() -> {
            log.debug("Client {} is about to disconnect", client);
        });
        client.postDisconnect(() -> {
            log.debug("Client {} is about to disconnect", client);        
        });
    }
    
    public void setHandler(BoltTCPMessageHandler mh) {
        handler = mh;
    }
    
    public void setPool(ByteBufferPool p) {
        pool = p;
    }
    
    public void connect() {
        client.connect(ip, port);
        connected=true;
    }
    
    public void send(byte[] tosend, boolean handshake) {
        Packet.builder().putBytes(tosend).queueAndFlush(client);
        log.debug("sending bytes to backend: " + Utils.byteArrayToHex(tosend));
        if (handshake) {
            readHandshake(4);
        } else {
            readMessages();
        }
    }
       
    private void readMessages() {
        ByteBuffer buffer = pool.newByteBuffer(65512, true);
        readMessage(buffer, false);       
    }
    
    //each response consists of zero or more detail messages followed by exactly one summary message
    public void readMessage(ByteBuffer buf, boolean detail) {
        readMessageLength(buf, detail);
    }
    
    public void readHandshake(int num) {
        log.debug("reading {} bytes from backend", num);
        client.readBytes(num, b -> {
            log.debug("b:" + Utils.byteArrayToHex(b));
            handler.processHandshakeResponse(BufferUtil.toBuffer(b));
        });
    }
    
    private void readMessageLength(ByteBuffer buf, boolean detail) {
        client.readBytes(2, b -> {
            log.debug("read msg size:" + Utils.byteArrayToHex(b));
            int len = HexFormat.fromHexDigits(Utils.byteArrayToHex(b));
            log.debug("parsed len:" + len);
            BufferUtil.append(buf, b);
            readMessageBody(len, buf, detail);
        });
    }
    
    // get the type
    private void readMessageBody(int len, ByteBuffer buf, boolean detail) {
        client.readBytes(len, b -> {
            log.debug("read chunk mess len:" + b.length);
            log.debug("read chunk mess:" + Utils.byteArrayToHex(b));
            BufferUtil.append(buf, b);
            byte type = b[1];
            log.debug("type: " + Utils.byteToHex(type));
            readMessageFooter(buf, type, len, detail);
        });
    }
    
    private void readMessageFooter(ByteBuffer buf, byte type, int len, boolean detail) {
        client.readBytes(2, b -> {
            log.debug("read chunk foot:" + Utils.byteArrayToHex(b));
            BufferUtil.append(buf, b);
            if (type == 0x70 && len > 3 && detail == false) {
                log.debug("longer success msg - read another");
                readMessage(buf, detail);
            } else if (type == 0x70 && len > 3 && detail == true) {
                log.debug("Reading complete");  
                handler.processResponse(buf);
            } else if (type == 0x71)  { // detail
                readMessage(buf, true);
            } else { // 7E (IGNORE), 70 (SUCCESS), 7F (FAILURE)
                log.debug("Reading complete");
                handler.processResponse(buf);
            }     
        });
    }
}