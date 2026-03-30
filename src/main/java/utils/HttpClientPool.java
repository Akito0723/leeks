package utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @author by laugh on 2016/3/29.
 */
public class HttpClientPool {

    private static volatile HttpClientPool clientInstance;
    private volatile HttpClient httpClient;

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
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(2000))
                .followRedirects(HttpClient.Redirect.NORMAL);
        if (proxyStr!=null && !proxyStr.isEmpty()){
            String[] s = proxyStr.split(":");
            if (s.length == 2){
                String host = s[0];
                int port = Integer.parseInt(s[1]);
                httpClientBuilder.proxy(ProxySelector.of(new InetSocketAddress(host, port)));
            }
            LogUtil.notifyInfo("setup proxy success->" + proxyStr);
        }
        httpClient = httpClientBuilder.build();
    }

    public String get(String url) throws HttpRequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(2000))
                .GET()
                .build();
        return getResponseContent(url, request);
    }

    public String post(String url) throws HttpRequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(2000))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return getResponseContent(url, request);
    }

    private String getResponseContent(String url, HttpRequest request) throws HttpRequestException {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new HttpStatusException(url, statusCode, response.body());
            }
            return response.body();
        } catch (HttpStatusException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpRequestException("HTTP request interrupted for url : " + URLDecoder.decode(url, StandardCharsets.UTF_8), e);
        } catch (IOException | IllegalArgumentException e) {
            throw new HttpRequestException("got an error from HTTP for url : " + URLDecoder.decode(url, StandardCharsets.UTF_8), e);
        }
    }

    public static class HttpRequestException extends Exception {
        public HttpRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class HttpStatusException extends HttpRequestException {
        private final int statusCode;

        public HttpStatusException(String url, int statusCode, String responseBody) {
            super(buildMessage(url, statusCode, responseBody), null);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }

        private static String buildMessage(String url, int statusCode, String responseBody) {
            String body = responseBody == null ? "" : responseBody;
            String detail = body.length() > 200 ? body.substring(0, 200) : body;
            return "HTTP status error for url : " + URLDecoder.decode(url, StandardCharsets.UTF_8)
                    + ", statusCode=" + statusCode + ", responseBody=" + detail;
        }
    }
}
