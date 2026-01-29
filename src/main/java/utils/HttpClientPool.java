package utils;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @author by laugh on 2016/3/29.
 */
public class HttpClientPool {

    private static volatile HttpClientPool clientInstance;
    private CloseableHttpClient httpClient;
    private PoolingHttpClientConnectionManager connectionManager;

    public static HttpClientPool getHttpClient() {
        HttpClientPool tmp = clientInstance;
        if (tmp == null) {
            synchronized (HttpClientPool.class) {
                tmp = clientInstance;
                if (tmp == null) {
                    tmp = new HttpClientPool();
                    clientInstance = tmp;
                }
            }
        }
        return tmp;
    }

    private HttpClientPool() {
        buildHttpClient(null);
    }

    public synchronized void buildHttpClient(String proxyStr){
        CloseableHttpClient oldClient = this.httpClient;
        PoolingHttpClientConnectionManager oldManager = this.connectionManager;
        PoolingHttpClientConnectionManager newManager = new PoolingHttpClientConnectionManager(100, TimeUnit.SECONDS);
        newManager.setMaxTotal(200);// 连接池
        newManager.setDefaultMaxPerRoute(100);// 每条通道的并发连接数
        RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(2000).setSocketTimeout(2000).build();
        HttpClientBuilder httpClientBuilder = HttpClients.custom().setConnectionManager(newManager);
        if (proxyStr!=null && !proxyStr.isEmpty()){
            String[] s = proxyStr.split(":");
            if (s.length == 2){
                String host = s[0];
                int port = Integer.parseInt(s[1]);
                httpClientBuilder.setProxy(new HttpHost(host,port));
            }
            LogUtil.notifyInfo("setup proxy success->" + proxyStr);
        }
        httpClient = httpClientBuilder.setDefaultRequestConfig(requestConfig).build();
        connectionManager = newManager;
        if (oldClient != null) {
            try {
                oldClient.close();
            } catch (Exception ignored) {
                // best-effort close to avoid leaking connections
            }
        }
        if (oldManager != null) {
            oldManager.close();
        }
    }

    public String get(String url) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        return getResponseContent(url,httpGet);
    }

    public String post(String url) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        return getResponseContent(url, httpPost);
    }

    private String getResponseContent(String url, HttpRequestBase request) throws Exception {
        HttpResponse response = null;
        try {
            response = httpClient.execute(request);
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new Exception("got an error from HTTP for url : " + URLDecoder.decode(url, StandardCharsets.UTF_8), e);
        } finally {
            if(response != null){
                EntityUtils.consumeQuietly(response.getEntity());
            }
            request.releaseConnection();
        }
    }
}
