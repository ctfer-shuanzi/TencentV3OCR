package com.czx.tencentv3ocr;

import android.annotation.SuppressLint;
import android.util.Log;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.HexFormat;
import java.util.Locale;
import java.util.TimeZone;

public class TencentCloudSigner {

    // ===== 固定常量 =====
    private static final String ALGORITHM = "TC3-HMAC-SHA256";
    private static final String HTTP_METHOD = "POST";
    private static final String CANONICAL_URI = "/";
    private static final String CanonicalQueryString = "";
    private static final String SERVICE = "ocr";
    private static final String HOST = "ocr.tencentcloudapi.com";
    private static final String CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String Action = "IDCardOCR";

    // 必须和 CanonicalHeaders 完全一致
    private static final String SIGNED_HEADERS = "content-type;host;x-tc-action";

    private final String secretId;
    private final String secretKey;
    private final long timestamp;

    public TencentCloudSigner(String secretId, String secretKey,long timestamp) {
        this.secretId = secretId;
        this.secretKey = secretKey;
        this.timestamp = timestamp;
        Log.d("czx", "TencentCloudSigner 使用的 timestamp: " + this.timestamp);
    }

    /**
     * 生成 Authorization 头
     */
    public String generateAuthorization(String payload) {
        try {
            Log.d("czx", "当前系统时间戳（秒级）：" + (System.currentTimeMillis() / 1000));
            Log.d("czx", "当前北京时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            // ===== 1. 时间 =====
            String date = formatDate(timestamp);
            String credentialScope = date + "/" + SERVICE + "/" +"tc3_request";

            // ===== 2. CanonicalRequest =====
            String hashedPayload = lowercase(hexEncode(hashSha256(payload)));
            String canonicalRequest = buildCanonicalRequest(hashedPayload);

            // ===== 3. StringToSign =====
            String stringToSign = buildStringToSign(credentialScope, canonicalRequest);

            // ===== 4. Signature =====
            String signature = calculateSignature(date, stringToSign);

            // ===== 5. Authorization =====
            String Authorization = buildAuthorization(signature, credentialScope);
            Log.d("czx", "credentialScope: " + credentialScope);
            Log.d("czx", "date: " + date);
            Log.d("czx", "StringToSign: " + stringToSign);
            Log.d("czx", "signature (hex): " + signature);
            Log.d("czx", "Authorization: " + Authorization);
            return Authorization;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===== CanonicalRequest =====
    private String buildCanonicalRequest(String hashedPayload) {
        String canonicalHeaders =
                "content-type:" + CONTENT_TYPE + "\n" +
                        "host:" + HOST + "\n" +
                        "x-tc-action:" + Action.toLowerCase() + "\n";

        String CanonicalRequest =
                HTTP_METHOD + "\n" +
                CANONICAL_URI + "\n" +
                CanonicalQueryString + "\n" +
                canonicalHeaders + "\n" +
                SIGNED_HEADERS + "\n" +
                hashedPayload;;

        return CanonicalRequest;
    }

    // ===== StringToSign =====
    private String buildStringToSign(String credentialScope, String canonicalRequest) throws Exception {
        Log.d("czx", "StringToSign使用的timestamp: " + timestamp);
        return ALGORITHM + "\n" +
                timestamp + "\n" +
                credentialScope + "\n" +
                sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8)).toLowerCase();
    }

    // ===== 派生签名 =====
    private String calculateSignature(String date, String stringToSign) throws Exception {
        byte[] kDate = hmacSha256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] kService = hmacSha256(kDate, SERVICE);
        byte[] kSigning = hmacSha256(kService, "tc3_request");
        return bytesToHex(hmacSha256(kSigning, stringToSign));
    }

    // ===== Authorization =====
    private String buildAuthorization(String signature, String credentialScope) {
        return ALGORITHM +
                " Credential=" + secretId + "/" + credentialScope +
                ", SignedHeaders=" + SIGNED_HEADERS +
                ", Signature=" + signature;
    }

    // ===== 工具方法 =====
    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        // 修复：秒级时间戳 × 1000 转为毫秒级
        return sdf.format(new Date(timestamp * 1000));
    }

    private String sha256Hex(byte[] data) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(data);
        return bytesToHex(hash);
    }

    private byte[] hmacSha256(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public static byte[] hashSha256(String data) throws NoSuchAlgorithmException {
        // 将字符串按照 UTF-8 编码转为字节数组
        byte[] bytes = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // 计算 SHA256
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(bytes);
    }
    @SuppressLint("NewApi")
    public static String hexEncode(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
    public static String lowercase(String str) {
        return str.toLowerCase();
    }
}
