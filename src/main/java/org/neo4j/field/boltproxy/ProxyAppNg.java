package org.neo4j.field.boltproxy;

import org.neo4j.field.boltproxy.handler.BoltWsMessageHandler;
import org.neo4j.field.boltproxy.backend.HttpBackend;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.javalin.Javalin;
import java.util.concurrent.TimeUnit;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.javalin.community.ssl.SslPlugin;
import io.javalin.jetty.JettyServer;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.DetectorConnectionFactory;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ServerConnector;

/**
 * Proxy App for WS (Bolt), TCP (Bolt), and HTTP.
 * TODO - TLS testing
 * TODO - Bolt Routing SSR pass thru
 */
public class ProxyAppNg {   
    static final Cache<String, Object> sessions = Caffeine.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build();
    private static Logger log = LoggerFactory.getLogger(ProxyAppNg.class);
    private static Configuration configs = null;
    
    public static void main(String[] args) throws Exception {
        try {
            configs = new PropertiesConfiguration("boltproxy.conf");         
        } catch (ConfigurationException e) {
            log.error("Can't load configuration file!");
            return;
        }
          
        Javalin app = Javalin.create(config -> {
            config.router.mount(router -> {
                config.jetty.modifyServer(server -> {
                    BoltConnectionFactory bcf = new BoltConnectionFactory(configs);
                    // Detect if TCP connection or WS/HTTP
                    DetectorConnectionFactory tcpDetector = new DetectorConnectionFactory(bcf);
                    HttpConnectionFactory hcf = new HttpConnectionFactory();
                    ServerConnector conn = new ServerConnector(server, tcpDetector, hcf);
                    conn.setPort(7070);
                    server.addConnector(conn);         
                });
                router.ws("/", ws -> {
                    ws.onConnect(ctx -> {
                        log.debug("connection with session:" + ctx.sessionId());
                        BoltWsMessageHandler mh = new BoltWsMessageHandler(ctx, configs);
                        sessions.put(ctx.sessionId(), mh);
                    });
                    ws.onClose(ctx -> {
                        log.info("close");
                    });
                    ws.onBinaryMessage(ctx -> {
                        log.debug("received for session:" + ctx.sessionId() + "-> " + ctx.length() + " off: " + ctx.offset() + " data: " + Utils.byteArrayToHex(ctx.data()));
                        BoltWsMessageHandler mh = (BoltWsMessageHandler)sessions.getIfPresent(ctx.sessionId());
                        mh.setBinaryContext(ctx);
                        mh.sendMessage(ctx.data());
                    });
                });
                router.get("/", ctx -> {
                    log.info("this may be http!");
                    HttpBackend b = new HttpBackend(configs);
                    JSONObject j = b.getJson("/");
                    ctx.json(j);
                });
            });

            if (configs.getBoolean("boltproxy.listener.tls")) {
                config.registerPlugin(new SslPlugin(ssl -> {
                    ssl.securePort = configs.getInt("boltproxy.listener.port");
                    ssl.insecure = false;
                    ssl.pemFromClasspath(configs.getString("boltproxy.listener.cert"), configs.getString("boltproxy.listener.privkey"));
                }));
            }  
        }).start(configs.getInt("boltproxy.listener.port"));
   
        JettyServer jws = app.jettyServer();
        for (Connector c:jws.server().getConnectors()) {
            log.info("Active Connector: " + c);
        }
    }
}