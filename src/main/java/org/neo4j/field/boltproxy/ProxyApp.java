package org.neo4j.field.boltproxy;

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

// TODO
// - SSL
// - Bolt Routing SSR passthru
public class ProxyApp {   
    static final Cache<String, Object> sessions = Caffeine.newBuilder().expireAfterAccess(30, TimeUnit.MINUTES).build();
    private static Logger log = LoggerFactory.getLogger(ProxyApp.class);
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
                router.ws("/", ws -> {
                    ws.onConnect(ctx -> {
                        log.info("connection with session:" + ctx.sessionId());
                        BoltMessageHandler mh = new BoltMessageHandler(ctx, configs);
                        sessions.put(ctx.sessionId(), mh);
                    });
                    ws.onClose(ctx -> {
                        log.info("close");
                    });
                    ws.onBinaryMessage(ctx -> {
                        log.debug("received for session:" + ctx.sessionId() + "-> " + ctx.length() + " off: " + ctx.offset() + " data: " + Utils.byteArrayToHex(ctx.data()));
                        BoltMessageHandler mh = (BoltMessageHandler)sessions.getIfPresent(ctx.sessionId());
                        mh.setBinaryContext(ctx);
                        mh.sendMessage(ctx.data());
                    });
                });
                // TODO - get from backend
                router.get("/", ctx -> {
                    HttpBackend b = new HttpBackend(configs);
                    JSONObject j = b.getJson("/");
                    ctx.json(j);
                });
            });
        }).start(configs.getInt("boltproxy.listener.port"));
    }
}