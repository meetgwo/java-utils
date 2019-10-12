package com.meetgwo.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * http工具类，使用了连接池。
 * 最终将替换掉HttpUtils
 * HTTP 4.5.x官方实例：hc.apache.org/httpcomponents-client-4.5.x/quickstart.html
 * @author guoshouqing on 18-12-26
 */
@Slf4j
public class HttpUtils {

    private final static String CHARSET = "UTF-8";
    private  static CloseableHttpClient httpClient;
    private static RequestConfig requestConfig;

    static {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(10);
        requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(10000) //read timeout
                .build();
        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(cm).build();
    }

    /**
     * form表单请求
     * @param url
     * @param paramsMap form参数对
     * @return
     */
    public static String post(String url, Map<String,String> paramsMap) throws Exception{


            List<BasicNameValuePair> list = new ArrayList<>();
            paramsMap.forEach((key,value)->{
                list.add(new BasicNameValuePair(key,value));
            });

            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(list, CHARSET);

            return post(url, formEntity,null);

    }

    /**
     * 请求体为json数据格式
     * @param url
     * @param json json格式字符串。这里定义成String类型而不定义成具体的JSON对象，以便业务层可以自由选择json框架。
     * @return
     */
    public static String postWithJson(String url,String json ) throws Exception{

            return  postWithJson(url,json,new HashMap<>());
    }

    /**
     * 请求体为json格式
     * @param url
     * @param json json格式字符串。不定义成具体的JSON对象，以便业务层可以自由选择json框架。
     * @return
     */
    public static String postWithJson(String url,String json,HashMap<String, Object> headers) throws Exception {

        if(headers == null) { headers = new HashMap<>(); }


        headers.put("Content-Type", "application/json;charset=UTF-8");

        StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
        return  post(url,entity,headers);
    }

    /**
     *  基础post请求方法，不对业务层开放
     * @param url
     * @param httpEntity
     * @param headers  request header
     * @return
     * @throws Exception 基础工具类的异常应该往上抛
     */
    private static String post(String url, HttpEntity httpEntity,Map<String,Object> headers) throws Exception{

        String respString ;

        CloseableHttpResponse response=null;
        try{
            HttpPost httpPost = new HttpPost(url);

            httpPost.setEntity(httpEntity);

            if(null != headers && !headers.isEmpty()){
                headers.forEach((key,value)->{ httpPost.setHeader(key,(String) value); });
            }
            response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            respString = EntityUtils.toString(entity, CHARSET);
            EntityUtils.consume(entity);

        } catch (Exception exception) {
//            log.error("http请求错误! url:{}",url,exception);
            throw  exception;
        }finally {
            try {
                response.close();
            }catch (Exception ex){
//                log.error("http response关闭异常！",ex);
            }
        }
        return respString;
    }

    /**
     * 请求参数追加在url中。
     * @param url 请求url
     */
    public static  String get(String url) throws Exception{
        return get(url,null,null);
    }

    /**
     *
     * @param url 请求url
     * @param params 请求参数，在内部把map转换成queryString
     * @return
     */
    public static  String get(String url,Map<String,Object> params ) throws Exception{
        return get(url,params,null);
    }

    /**
     * GET请求
     * @param params 请求参数，在内部把map转换成queryString
     * @param headers 请求头，可以不设置
     * @return
     */
    public static String get(String url,Map<String,Object> params,Map<String,Object> headers) throws Exception{

        String respString ;
        CloseableHttpResponse response=null;
        try {
                HttpGet httpGet = new HttpGet();
                URIBuilder uriBuilder = new URIBuilder(url);
                if(params!=null && !params.isEmpty()) {
                    params.forEach((key, value) -> uriBuilder.addParameter(key, String.valueOf(value)));
                }

                if(headers!=null && !headers.isEmpty()){
                    headers.forEach((key,value)->{httpGet.setHeader(key,(String) value);});
                }
                httpGet.setURI(uriBuilder.build());
                response = httpClient.execute(httpGet);
                HttpEntity entity = response.getEntity();
                respString = EntityUtils.toString(entity, CHARSET);
                EntityUtils.consume(entity);
            } catch (Exception exception) {
//                log.error("http请求错误! url:{}",url,exception);
                throw exception;
            }finally {
                try {
                    response.close();
                }catch (Exception ex){
//                    log.error("http response关闭异常！",ex);
                }
            }
            return respString;
    }
    /**
     * 解析url中的参数，并封装成map
     * @param url 带请求参数的url
     * @return
     */
    public static  HashMap<String,String> parseQueryStringToMap(String url){
        HashMap<String,String>  paramPairs=new HashMap<>();

        if(url == null || !url.contains("?")){
            return paramPairs;
        }
        String queryString = StringUtils.substringAfter(url, "?");
        String[] params = queryString.split("&");
        for (String param:params){
            String[] pair = param.split("=");
            if(pair.length == 2) {
                paramPairs.put(pair[0], pair[1]);
            }
        }
        return paramPairs;
    }

    public static String decodeUrl(String url) {
        String decodeUrl = null;
        try {
            decodeUrl = URLDecoder.decode(url, StandardCharsets.UTF_8.toString());
        }catch (Exception ex){
//            log.error("url转码错误！Url:{}",url,ex);
        }
        return decodeUrl;
    }

}




    