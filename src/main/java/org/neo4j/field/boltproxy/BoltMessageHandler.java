package org.neo4j.field.boltproxy;

import io.javalin.websocket.WsBinaryMessageContext;
import io.javalin.websocket.WsConnectContext;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neo4j.bolt.message.Message;
import org.neo4j.bolt.unpacker.MessageUnpacker;
import org.neo4j.bolt.value.DriverValueUnpacker;
import org.neo4j.bolt.value.ValueUnpacker;
import org.neo4j.bolt.json.Serializer;
import org.neo4j.bolt.logger.LogRecord;
import org.neo4j.driver.Value;
import static org.neo4j.driver.Values.value;
import org.neo4j.driver.internal.messaging.common.CommonValuePacker;
import org.neo4j.driver.internal.messaging.encode.LogonMessageEncoder;
import org.neo4j.driver.internal.messaging.request.LogonMessage;
import org.neo4j.driver.internal.packstream.PackOutput;
import org.neo4j.driver.internal.util.io.ByteBufOutput;

public class BoltMessageHandler {
    private static Logger log = LoggerFactory.getLogger(BoltMessageHandler.class);
    private WsBinaryMessageContext bctx = null;
    private WsConnectContext ctx = null;
    private BoltBackend bp = null;
    private Configuration configs = null;
    
    public BoltMessageHandler(WsBinaryMessageContext c) {
        bctx = c;   
    }
    public BoltMessageHandler(WsConnectContext c, Configuration cfg) {
        ctx = c;
        try {
            configs = cfg;
            log.debug("creating new backend connection");
            bp = new BoltBackend(new URI(configs.getString("boltproxy.backend.bolt","ws://localhost:7687")));
            bp.setHandler(this);
            boolean cc = bp.connectBlocking();
            log.info("connected to backend: " + cc);
        } catch (URISyntaxException e) {
            log.error("Exception on URI! ", e);
        } catch (InterruptedException ie) {
            log.error("Interrupted while connecting! ", ie);
        }
    }
    
    public void setConfigs(Configuration c) {
        configs = c;
    }
    
    public void setBinaryContext(WsBinaryMessageContext c) {
        bctx = c;
    }
    
    public void sendMessage(byte[] b) {
        log.info("Preparing message to backend: " + Utils.byteArrayToHex(b));
        byte[] lm = inspectMessage(b);
        if (lm != null) {
            log.debug("Sending updated message to backend: " + Utils.byteArrayToHex(lm));
            bp.send(lm);
        } else {
            bp.send(b);
        }
    }

    public void processResponse(ByteBuffer b) {
        log.info("Preparing message to client: " + Utils.byteArrayToHex(b.array()));
        inspectMessage(b.array());
        ctx.send(b);
    }
    
    /**
     * add some params to a Login message
     */
    byte[] updateAuth(Message message) {
        log.info("updating auth with metadata: " + message.getName());
        // Injecting these parameters into the Auth token.
        Map<String,String> params = Map.of("param1","value1","param2","value2");
        Map<String, Value> md = ((org.neo4j.bolt.message.LogonMessage)message).getMetadata();

        md.put("parameters", value(params));
        LogonMessage nm = new LogonMessage(md);
        var buffer = Unpooled.buffer();
        PackOutput output = new ByteBufOutput(buffer);
        CommonValuePacker cp = new CommonValuePacker(output,true);
        LogonMessageEncoder me = new LogonMessageEncoder();
        try {
            me.encode(nm, cp);
            byte[] p = Utils.prune(buffer.array());
            log.debug("new login::" + Utils.byteArrayToHex(p));
            return p;
        } catch (IOException ie) {
            log.error("error encoding new logon message");
        }
        return new byte[1];
    }

    public byte[] inspectMessage(byte b[]) {
        boolean ret = false;
        if ((b[0]==0x60 && b[1]==0x60 && b[2]==(byte)0xb0 && b[3]==0x17) || (b[0]==0x00 && b[1]==0x00)) {
            log.info("Skip inspection of protocol negotiation frames");
        } else {
            log.debug("New Inspection::" + Utils.byteArrayToHex(b));
            List<byte[]> bm = Utils.messages(b);
            for (int i = 0; i < bm.size(); i++) {
                try {
                    log.debug("Message to process::" + Utils.byteArrayToHex(bm.get(i)));
                    ByteBuffer buffer = ByteBuffer.wrap(bm.get(i));
                    ValueUnpacker valueUnpacker = new DriverValueUnpacker(buffer);
                    Message message = MessageUnpacker.unpack(valueUnpacker);
                    LogRecord logRecord = new LogRecord(message, ctx.session.getRemoteAddress(), ctx.session.getLocalAddress());
                    String sr = Serializer.toJson(logRecord);
                    log.info("Message Processed::" + sr);
                    if (message.getName().equals("LOGON")) {
                        log.info("augmenting login with metadata");
                        byte[] updated = updateAuth(message);
                        bm.set(i, updated);
                        ret = true;
                    }
                } catch (Exception e) {
                    log.error("error parsing bolt message: ", e);
                }
            }
            if (ret == true) {
                byte[] xx = Utils.combine(bm);
                return Utils.combine(bm);
            }
        }
        return null;
    }
}
