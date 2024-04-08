package org.neo4j.field.boltproxy;

import java.net.URI;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TestBackend {
    
    @Test
    public void testBackend() throws Exception {
        BoltBackend bp = new BoltBackend(new URI("ws://localhost:7687"));
        assertTrue(bp.connectBlocking());
    }
    
}
