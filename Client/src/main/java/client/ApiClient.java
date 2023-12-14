package client;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

/**
 * @author xiaorui
 */
public class ApiClient {
    public static CloseableHttpClient generateHttpClient() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(6000);
        connectionManager.setDefaultMaxPerRoute(5000);

//        RequestConfig requestConfig = RequestConfig.custom()
//                .setConnectTimeout(5000)   // Connection timeout
//                .setSocketTimeout(5000)    // Response timeout
//                .setConnectionRequestTimeout(1000) // Timeout to get a connection from the pool
//                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .build();
    }
}
