package org.neo4j.field.boltproxy;

import org.neo4j.field.boltproxy.backend.BoltWsBackend;
import java.net.URI;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TestBackend {
    
    @Test
    public void testBackend() throws Exception {
        BoltWsBackend bp = new BoltWsBackend(new URI("ws://localhost:7687"));
        assertTrue(bp.connectBlocking());
    }
    
}
