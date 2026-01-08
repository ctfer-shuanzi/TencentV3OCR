package com.czx.tencentv3ocr;

import com.google.gson.Gson;

public class ApiResponseParser {
    private static final Gson GSON = new Gson();

    public static IdentifyResult parseResponse(String jsonResponse) {
        // 先解析外层响应，再获取核心的 IdentifyResult
        OcrApiResponse apiResponse = GSON.fromJson(jsonResponse, OcrApiResponse.class);
        return apiResponse != null ? apiResponse.getResponse() : null;
    }
}