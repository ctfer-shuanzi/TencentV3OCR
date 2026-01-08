package com.czx.tencentv3ocr;

import android.util.Log;

import okhttp3.*;

public class ApiRequestSender {
    private static final OkHttpClient client = new OkHttpClient();
    private static final String API_URL = "https://ocr.tencentcloudapi.com";
    private static final String CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String HOST = "ocr.tencentcloudapi.com";
    private static final String Action = "IDCardOCR";
    private static final String Version = "2018-11-19";
    private static final String Region = "ap-guangzhou";

    public static String sendPostRequest(String jsonBody, String authorization,long timeStamp) throws Exception
    {
        Log.d("czx", "实际发送的请求头: ");
        Log.d("czx", "Authorization: " + authorization);
        Log.d("czx", "Content-Type: application/json; charset=utf-8");
        Log.d("czx", "Host: " + HOST);
        Log.d("czx", "X-TC-Action: " + Action);
        Log.d("czx", "X-TC-Version: " + Version);
        Log.d("czx", "X-TC-Timestamp: " + timeStamp);
        Log.d("czx", "X-TC-Region: " + Region);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(jsonBody, MediaType.parse(CONTENT_TYPE)))
                .header("Authorization", authorization)
                .header("Content-Type", CONTENT_TYPE)
                .header("Host", HOST)
                .header("X-TC-Action",Action)
                .header("X-TC-Version",Version)
                .header("X-TC-Timestamp", String.valueOf(timeStamp))
                .header("X-TC-Region",Region)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.d("czx","状态码"+response.code());
                throw new RuntimeException("HTTP错误: " + response.code());
            }
            if (response.body() != null) {
                return response.body().string();
            }else{
                Log.d("czx","响应结果的响应体为空");
                return null;
            }
        }
    }
}