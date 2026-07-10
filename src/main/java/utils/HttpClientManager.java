package utils;

import org.apache.commons.lang3.StringUtils;

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
public class HttpClientManager {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(2);

    private static volatile HttpClientManager instance;

    private final HttpClient directClient;
    private volatile HttpClient proxyAwareClient;

    public static HttpClientManager getInstance() {
        HttpClientManager tmp = instance;
        if (tmp == null) {
            synchronized (HttpClientManager.class) {
                tmp = instance;
                if (tmp == null) {
                    tmp = new HttpClientManager();
                    instance = tmp;
                }
            }
        }
        return tmp;
    }

    private HttpClientManager() {
        directClient = newClientBuilder().build();
        proxyAwareClient = directClient;
    }

    public synchronized void configureProxy(String proxyStr) {
        if (StringUtils.isBlank(proxyStr)) {
            proxyAwareClient = directClient;
            return;
        }

        proxyAwareClient = buildProxyClient(proxyStr);
        LogUtil.notifyInfo("setup proxy success->" + proxyStr);
    }

    private static HttpClient buildProxyClient(String proxyStr) {
        HttpClient.Builder httpClientBuilder = newClientBuilder();
        InetSocketAddress proxyAddress = parseProxyAddress(proxyStr);
        httpClientBuilder.proxy(ProxySelector.of(proxyAddress));
        return httpClientBuilder.build();
    }

    public static void validateProxy(String proxyStr) {
        if (StringUtils.isNotBlank(proxyStr)) {
            parseProxyAddress(proxyStr);
        }
    }

    private static InetSocketAddress parseProxyAddress(String proxyStr) {
        String[] proxyParts = StringUtils.splitPreserveAllTokens(proxyStr, ':');
        if (proxyParts == null || proxyParts.length != 2) {
            throw new IllegalArgumentException("代理格式应为 host:port");
        }
        String host = StringUtils.trim(proxyParts[0]);
        String portText = StringUtils.trim(proxyParts[1]);
        if (StringUtils.isBlank(host) || StringUtils.isBlank(portText)) {
            throw new IllegalArgumentException("代理格式应为 host:port");
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("代理端口必须是数字", e);
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("代理端口必须在 1-65535 之间");
        }
        return InetSocketAddress.createUnresolved(host, port);
    }

    public String get(String url) throws HttpRequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DEFAULT_REQUEST_TIMEOUT)
                .GET()
                .build();
        return getResponseContent(url, request, proxyAwareClient, HttpResponse.BodyHandlers.ofString());
    }

    public String post(String url) throws HttpRequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DEFAULT_REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return getResponseContent(url, request, proxyAwareClient, HttpResponse.BodyHandlers.ofString());
    }

    public String getWithProxy(String url, String proxyStr) throws HttpRequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DEFAULT_REQUEST_TIMEOUT)
                .GET()
                .build();
        HttpClient client = StringUtils.isBlank(proxyStr) ? directClient : buildProxyClient(proxyStr);
        return getResponseContent(url, request, client, HttpResponse.BodyHandlers.ofString());
    }

    public byte[] getBytesDirect(String url, Duration requestTimeout) throws HttpRequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(requestTimeout)
                .GET()
                .build();
        return getResponseContent(url, request, directClient, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static HttpClient.Builder newClientBuilder() {
        return HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL);
    }

    private <T> T getResponseContent(String url, HttpRequest request, HttpClient client,
            HttpResponse.BodyHandler<T> bodyHandler) throws HttpRequestException {
        try {
            HttpResponse<T> response = client.send(request, bodyHandler);
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                String responseBody = response.body() instanceof String body ? body : "";
                throw new HttpStatusException(url, statusCode, responseBody);
            }
            return response.body();
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
