package org.neo4j.field.boltproxy;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class TestUtils {
        
    @Test
    public void testCount() {
        byte[] t = new byte[54];
        assertEquals("0036", Utils.byteArrayToHex(Utils.len((short)t.length)));
        System.out.println("hex:" + Utils.byteArrayToHex(Utils.len((short)t.length)));
    }
    
    @Test
    public void testCombine() {
            byte[] data1 = Utils.hexToByteArray("aabbccddeeff");
            byte[] data2 = Utils.hexToByteArray("010203040506");
            byte[] data3 = Utils.hexToByteArray(("070809101112"));
            List<byte[]> x = new ArrayList<byte[]>();
            x.add(data1);
            x.add(data2);
            x.add(data3);
            byte r[] = Utils.combine(x);
            assertEquals(data1.length+data2.length+data3.length + 12, r.length);
            assertEquals("0006aabbccddeeff00000006010203040506000000060708091011120000", Utils.byteArrayToHex(r));
            System.out.println("List:" + Utils.byteArrayToHex(r)); 
    }
}
