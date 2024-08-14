package org.neo4j.field.boltproxy;

import java.util.Map;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

public class TestBoltDriver {
    final static String bolturl = "bolt://localhost:7070";
    final static String user = "neo4j";
    final static String pwd = "password";
    private static Driver driver = null;
    
    @Test
    public void testBolt() {
        // Send a map along with the authentication token.
        AuthToken at = AuthTokens.basic(user, pwd);
        try {
            System.out.println("Driver Start");
            driver = GraphDatabase.driver(bolturl, at);
            System.out.println("Driver Start:" + driver);
            //driver.verifyConnectivity();
            Session sess = driver.session(Session.class, at);
            System.out.println("Sess Start:" + sess);
            Result r = sess.run("RETURN true;");
            System.out.println("Res Start:" + r);
            assertTrue(r.single().get(0).asBoolean());
            sess.close();
        } catch (Exception e) {
            e.printStackTrace();
            assertFalse(true);
        }
    }
}
