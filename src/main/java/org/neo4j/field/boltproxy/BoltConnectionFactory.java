package org.neo4j.field.boltproxy;

import java.nio.ByteBuffer;
import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory.Detecting.Detection;
import org.eclipse.jetty.server.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detect if TCP or HTTP/Ws connection.
 */
public class BoltConnectionFactory extends AbstractConnectionFactory implements ConnectionFactory.Detecting {
    private Logger log;
    private Configuration configs;
    
    public BoltConnectionFactory(Configuration cfg) {
        super("Bolt-TCP");
        configs = cfg;
        log = LoggerFactory.getLogger(BoltConnectionFactory.class);
    }

    @Override
    public Connection newConnection(Connector connector, EndPoint endPoint) {
        log.debug("New connection: " + connector);
        BoltTCPConnection connection = new BoltTCPConnection(endPoint, connector.getExecutor(), connector.getByteBufferPool(), configs);
        log.debug("New Bolt-TCP connection: " + connection);
        return connection;
    }  
    
    @Override
    public Detection detect(ByteBuffer buffer) {
        if (buffer.remaining() < 2)
            return Detection.NEED_MORE_BYTES;
        byte test[] = new byte[4];
        buffer.get(test, 0, 4);
        if (Utils.byteArrayToHex(test).equals("6060b017")) {
            log.debug("Detected TCP Connection");
            buffer.position(0);
            return Detection.RECOGNIZED;
        }
        buffer.position(0);
        return Detection.NOT_RECOGNIZED;
    }
}
