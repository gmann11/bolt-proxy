package org.neo4j.field.boltproxy;

import io.netty.buffer.Unpooled;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.neo4j.bolt.json.Serializer;
import org.neo4j.bolt.logger.LogRecord;
import org.neo4j.bolt.message.Message;
import org.neo4j.bolt.unpacker.MessageUnpacker;
import org.neo4j.bolt.value.DriverValueUnpacker;
import org.neo4j.bolt.value.ValueUnpacker;
import org.neo4j.driver.Value;
import static org.neo4j.driver.Values.value;
import org.neo4j.driver.internal.BoltAgent;
import org.neo4j.driver.internal.messaging.common.CommonValuePacker;
import org.neo4j.driver.internal.messaging.encode.HelloMessageEncoder;
import org.neo4j.driver.internal.messaging.request.HelloMessage;
import org.neo4j.driver.internal.packstream.PackOutput;
import org.neo4j.driver.internal.util.io.ByteBufOutput;

public class TestParsing {
    
    @Test
    public void shouldUnpackHelloMessage() throws Exception
    {
        Map<String,Value> at = Map.of( "scheme", value("basic"), "username", value("1"), "secret", value("2") );
        Map<String,String> rt = Map.of( "rout", "test");
        BoltAgent ba = new BoltAgent("MyDriver", "1.2.3", "en", "us");
        HelloMessage requestMessage = new HelloMessage("user_agent",ba,at,rt,true,null);
        var buffer = Unpooled.buffer();
        PackOutput output = new ByteBufOutput(buffer);
        CommonValuePacker cp = new CommonValuePacker(output,true);
        HelloMessageEncoder me = new HelloMessageEncoder();
        me.encode(requestMessage, cp);
        byte[] xx = new byte[1000];
        System.out.println("z:");
        output.writeBytes(xx);
        System.out.println("x" + Utils.byteArrayToHex(buffer.array()));
    }

    @Test
    public void testParseBlock() {
        byte data[] = Utils.hexToByteArray("0094b310d08e0a4d415443482028292052455455524e207b206e616d653a276e6f646573272c20646174613a636f756e74282a29207d20415320726573756c740a554e494f4e20414c4c0a4d415443482028292d5b5d2d3e28292052455455524e207b206e616d653a2772656c6174696f6e7368697073272c20646174613a20636f756e74282a297d20415320726573756c740aa0a000000008b13fa1816ec903e80000");
        System.out.println("len: " + data.length);
        List<byte[]> b = Utils.messages(data);
        System.out.println("ZZ:" + b.size());
        for (int j=0; j < b.size(); j++) {
            System.out.println("ZZZ:" + Utils.byteArrayToHex(b.get(j)));
            ByteBuffer buffer = ByteBuffer.wrap(b.get(j));
            ValueUnpacker valueUnpacker = new DriverValueUnpacker(buffer);
            Message message = MessageUnpacker.unpack(valueUnpacker);
            LogRecord logRecord = new LogRecord(message, null, null);
            String sr = Serializer.toJson(logRecord);
            System.out.println("sr:" + sr);
        }
        assertEquals(2, b.size());
    }
}
