package org.neo4j.field.boltproxy;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.configuration.Configuration;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpBackend {
    Configuration configs = null;
    OkHttpClient client = null;
    private static Logger log = LoggerFactory.getLogger(HttpBackend.class);
    
    public HttpBackend(Configuration c) {
        configs = c;
        client = new OkHttpClient();
    }
    
    public JSONObject getJson(String path) {
        Request request = new Request.Builder()
                .addHeader("Accept", "application/json")
                .url(configs.getString("boltproxy.backend.discovery") + path)
                .build();
        try {
            Response response = client.newCall(request).execute();
            String rs = response.body().string();
            JSONParser p = new JSONParser();
            return (JSONObject)p.parse(rs);
        } catch (Exception e) {
            log.error("Error querying discovery endpoint!", e);
            return null;
        }
    }
    
}
