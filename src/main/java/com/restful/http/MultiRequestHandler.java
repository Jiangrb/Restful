package com.restful.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.restful.api.ErrorMessage;
import com.restful.api.HttpMethod;
import com.restful.api.RequestHandler;
import com.restful.api.Response;
import com.restful.config.Config;
import com.restful.config.Loader;
import com.restful.exception.HttpException;
import com.restful.exception.ServerCodeException;
import com.restful.exception.ServerException;
import com.restful.util.GsonUtil;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 多实例请求对象
 */
public class MultiRequestHandler extends RequestHandler {

    private static final String HTTP = "http://";
    private static final String TAG = "MultiRequestHandler";
    public static boolean DEBUD = false;

    protected Config config;

    public MultiRequestHandler() {
        // singleton
        config = new Config();//Loader.load("config.properties");
    }

    public MultiRequestHandler(Config config) {
        this.config = config;
    }

    public MultiRequestHandler(String properties) {
        // singleton
        config = Loader.load(properties);
    }

    public static MultiRequestHandler getInstance() {
        return new MultiRequestHandler();
    }

    public static MultiRequestHandler getInstance(String properties) {
        return new MultiRequestHandler(properties);
    }

    public static MultiRequestHandler getInstance(Config config) {
        return new MultiRequestHandler(config);
    }

    @Override
    public Response doUpload(String path, Map<String, String> headers, Map<String, Object> params, String file, long start, long len) throws Exception {
        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "******";

        URL url = new URL(buildUri(path,params));
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setDoInput(true);
        http.setDoOutput(true);
        http.setUseCaches(false);
        http.setRequestMethod("POST");
        http.setRequestProperty("Cookie", getCookie());
        http.setRequestProperty("Connection", "Keep-Alive");
        http.setRequestProperty("Charset", config.charset);
        http.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        if (headers != null && !headers.isEmpty()) {
            for (Entry<String, String> header : headers.entrySet()) {
                http.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        if (DEBUD) {
            System.out.println(TAG + "   //上传开始------------------------------------------------------------------");
            System.out.println(TAG + "   //上传url:" + url);
            System.out.println(TAG + "   //上传headers:" + headers);
            System.out.println(TAG + "   //上传params:" + params);
            System.out.println(TAG + "   //上传file:" + file);
            System.out.println(TAG + "   //上传start:" + start);
            System.out.println(TAG + "   //上传len:" + len);
            System.out.println(TAG + "   //上传结束------------------------------------------------------------------");
        }

        //------------------------------------------------------------------------------------------
        DataOutputStream dos = new DataOutputStream(http.getOutputStream());

        String filename = file.substring(file.lastIndexOf("/") + 1);
        dos.writeBytes(twoHyphens + boundary + end);
        dos.writeBytes("Content-Disposition: form-data; name=\"FileName\"; filename=\"" + filename + "\"" + end);
        dos.writeBytes(end);

        RandomAccessFile randomFile = new RandomAccessFile(file, "r");
        randomFile.seek(start);
        byte[] bytes = new byte[8192]; // 8k
        for (int tmp , readlen = 0 ; (tmp = randomFile.read(bytes)) != -1 ; ) {
            if (readlen + tmp < len) {
                dos.write(bytes, 0, tmp);
                readlen = readlen + tmp;
            } else {
                dos.write(bytes, 0, (int)(len - readlen));//readlen + tmp > len
                break;
            }
        }
        randomFile.close();
        dos.writeBytes(end);

        dos.writeBytes(twoHyphens + boundary + twoHyphens + end);
        dos.flush();
        dos.close();
        //------------------------------------------------------------------------------------------

        if (DEBUD) {
            Response response = getResponse(http);
            System.out.println(TAG + "   //服务器返回------------------------------------------------------------------");
            System.out.println(TAG + "   //返回StatusCode:" + response.getStatusCode());
            System.out.println(TAG + "   //返回Body:" + response.getOrgbody());
            System.out.println(TAG + "   //返回Headers:" + response.getHeaders());
            System.out.println(TAG + "   //服务器结束------------------------------------------------------------------");
            return response;
        }

        return getResponse(http);
    }

    @Override
    public Response doUpload(String path, Map<String, String> headers, Map<String, Object> params , String... files) throws Exception {
        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "******";

        URL url = new URL(buildUri(path,params));
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setDoInput(true);
        http.setDoOutput(true);
        http.setUseCaches(false);
        http.setRequestMethod("POST");
        http.setRequestProperty("Cookie", getCookie());
        http.setRequestProperty("Connection", "Keep-Alive");
        http.setRequestProperty("Charset", config.charset);
        http.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

        if (headers != null && !headers.isEmpty()) {
            for (Entry<String, String> header : headers.entrySet()) {
                http.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        if (DEBUD) {
            System.out.println(TAG + "   //上传开始------------------------------------------------------------------");
            System.out.println(TAG + "   //上传url:" + url);
            System.out.println(TAG + "   //上传headers:" + headers);
            System.out.println(TAG + "   //上传params:" + params);
            System.out.println(TAG + "   //上传files:" + Arrays.toString(files));
            System.out.println(TAG + "   //上传结束------------------------------------------------------------------");
        }

        DataOutputStream dos = new DataOutputStream(http.getOutputStream());

        for (String file : files) {
            String filename = file.substring(file.lastIndexOf("/") + 1);
            dos.writeBytes(twoHyphens + boundary + end);
            dos.writeBytes("Content-Disposition: form-data; name=\"FileName\"; filename=\"" + filename + "\"" + end);
            dos.writeBytes(end);

            FileInputStream fis = new FileInputStream(file);
            byte[] bytes = new byte[8192]; // 8k
            for (int count; (count = fis.read(bytes)) != -1; ) {
                dos.write(bytes, 0, count);
            }
            fis.close();
            dos.writeBytes(end);
        }

        dos.writeBytes(twoHyphens + boundary + twoHyphens + end);
        dos.flush();
        dos.close();

        if (DEBUD) {
            Response response = getResponse(http);
            System.out.println(TAG + "   //服务器返回------------------------------------------------------------------");
            System.out.println(TAG + "   //返回StatusCode:" + response.getStatusCode());
            System.out.println(TAG + "   //返回Body:" + response.getOrgbody());
            System.out.println(TAG + "   //返回Headers:" + response.getHeaders());
            System.out.println(TAG + "   //服务器结束------------------------------------------------------------------");
            return response;
        }

        return getResponse(http);
    }

    /**
     * Make the request to Xively API and return the response string
     *
     * @param method  http request methods
     * @param path    restful app path
     * @param body    objects to be parsed as body for api call
     * @param params  key-value of params for api call
     * @return response string
     */
    public Response doRequest(HttpMethod method, String path, Map<String, String> headers,Object body, Map<String, Object> params) throws Exception {

        URL url = new URL(buildUri(path,params));
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setUseCaches(false);
        http.setRequestMethod(method.name());
        http.setRequestProperty("Charset", config.charset);
        http.setRequestProperty("Cookie", getCookie());
        http.setReadTimeout(config.socketTimeout);
        http.setConnectTimeout(config.connectionTimeout);
        //http.setRequestProperty("Connection", "Keep-Alive");
        //http.setRequestProperty("Content-TaskType", "multipart/form-data;boundary=" + boundary);

        if (headers != null && !headers.isEmpty()) {
            for (Entry<String, String> header : headers.entrySet()) {
                http.setRequestProperty(header.getKey(), header.getValue());
            }
        }

        if (DEBUD) {
            System.out.println(TAG + "   //请求开始------------------------------------------------------------------");
            System.out.println(TAG + "   //请求url:" + url);
            System.out.println(TAG + "   //请求headers:" + headers);
            System.out.println(TAG + "   //请求params:" + params);
            System.out.println(TAG + "   //请求body:" + body);
            System.out.println(TAG + "   //请求结束------------------------------------------------------------------");
        }

        if (HttpMethod.POST.equals(method)) {
            http.setDoOutput(true);
            if (body != null) {
                byte[] entity = getHttpBody(body);
                http.setRequestProperty("Content-Type", config.requestMediaType.contentType);
                http.setRequestProperty("Content-Length", String.valueOf(entity.length));
                OutputStream outStream = http.getOutputStream();
                outStream.write(entity);
                outStream.flush();
                outStream.close();
            } else {
                http.setRequestProperty("Content-Type", config.requestMediaType.contentType);
                http.setRequestProperty("Content-Length", String.valueOf(0));
                OutputStream outStream = http.getOutputStream();
                outStream.close();
            }
        }

        if (DEBUD) {
            Response response = getResponse(http);
            System.out.println(TAG + "   //服务器返回------------------------------------------------------------------");
            System.out.println(TAG + "   //返回StatusCode:" + response.getStatusCode());
            System.out.println(TAG + "   //返回Body:" + response.getOrgbody());
            System.out.println(TAG + "   //返回Headers:" + response.getHeaders());
            System.out.println(TAG + "   //服务器结束------------------------------------------------------------------");
            return response;
        }

        return getResponse(http);
    }

    private byte[] getHttpBody(Object body) throws UnsupportedEncodingException, JsonProcessingException {
        if (body.getClass().equals(byte[].class)) {
            return (byte[])body;
        }
        return getEntity(body).getBytes(config.charset);
    }

    private String buildUri(String path, Map<String, Object> params) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        if (!path.startsWith(HTTP)) {
            String host = config.ip;
            builder.append(HTTP);
            builder.append(config.ip);
            if (config.port != null && config.port.length() > 0) {
                host += ":" + config.port;
                builder.append(':');
                builder.append(config.port);
            }
            builder.append('/');
            builder.append(config.version);
            builder.append(path);
        } else {
            builder.append(path);
        }
        boolean hasparam = path.indexOf('?') >= 0,isfirst = true;
        if (params != null && !params.isEmpty()) {
            for (Entry<String, Object> param : params.entrySet()) {
                if (isfirst && !hasparam) {
                    builder.append('?');
                    isfirst = false;
                } else {
                    builder.append('&');
                }
                builder.append(param.getKey());
                builder.append('=');
                builder.append(URLEncoder.encode(String.valueOf(param.getValue()),"UTF-8"));
            }
        }
        return builder.toString().replace("//","/").replace("http:/",HTTP);
    }

    private String getEntity(Object body) throws JsonProcessingException {
        if (body instanceof JSONObject || body instanceof String) {
            return body.toString();
        } else if (body != null) {
            return GsonUtil.toJson(body);
        } else {
            return "";
        }
    }

    private Response getResponse(HttpURLConnection http) throws IOException {
        InputStream is = http.getInputStream();
        InputStreamReader isr = new InputStreamReader(is, config.charset);
        BufferedReader br = new BufferedReader(isr);
        StringBuffer buffer = new StringBuffer();
        for (String line; (line = br.readLine()) != null; ) {
            buffer.append(line);
            buffer.append('\n');
        }
        is.close();

        Response response = new MultiResponse(http.getResponseCode());
        response.setOrgbody(buffer.toString().replaceAll("new Date\\(|\\+\\d{4}\\)",""));
        Map<String, String> reheaders = new HashMap<>();
        for (Entry<String, List<String>> header : http.getHeaderFields().entrySet()) {
            if (header.getValue() != null && header.getValue().size() > 0) {
                reheaders.put(header.getKey(), header.getValue().get(0));
            }
        }
        if (cookies != null) {
            updateCookie(reheaders.get("Set-Cookie"));
        }
        response.setHeaders(reheaders);

        int statusCode = response.getStatusCode();
        if (http.getResponseCode() != config.successcode) {
            throw new HttpException("HTTP " + statusCode, statusCode, response.getBody());
        }
        return response;
    }

    private class MultiResponse extends Response{

        public MultiResponse(int statusCode) {
            super(statusCode);
        }

        @Override
        public Response parser() {
            if (body == null) {
                if (!config.jsonframework || config.status==null || config.status_ok==null
                        || config.result == null || config.message == null) {
                    return this;
                }
                try {
                    JSONObject object = new JSONObject(this.getOrgbody());
                    if (config.status_ok.equals("" + object.get(config.status))) {
                        if (object.has(config.result)) {
                            this.setBody(object.get(config.result).toString());
                        } else {
                            this.setBody("");
                        }
                    } else {
                        String errormessage = object.get(config.message).toString();
                        try {
                            ErrorMessage message = GsonUtil.toObject(errormessage, config.ErrorMessageClass);
                            throw new ServerCodeException(message);
                        } catch (ServerCodeException e) {
                            throw e;
                        } catch (Throwable e) {
                            throw new ServerException(errormessage);
                        }
                    }
                } catch (ServerException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new ServerException(this.getOrgbody(), e);
                }
                return this;
            }
            return this;
        }
    }

}
