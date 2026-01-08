package com.czx.tencentv3ocr;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

// 外层响应类（匹配接口返回的 Response 节点）
public class OcrApiResponse implements Serializable {
    @SerializedName("Response")
    private IdentifyResult response; // 核心身份证信息节点

    public IdentifyResult getResponse() {
        return response;
    }

    public void setResponse(IdentifyResult response) {
        this.response = response;
    }
}