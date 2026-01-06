package com.czx.tencentv3ocr;

public class SignatureUtil {
    // ************* 步骤 1：拼接规范请求串 *************
    private static String HTTPRequestMethod = "POST";
    private static String CanonicalURI = "/";
    private static String CanonicalQueryString = "";
    private static String CanonicalHeaders = "content-type:application/json;    charset=utf-8\nhost:ocr.tencentcloudapi.com\n";
    private static String SignedHeaders = "content-type;host";//参与签名的头部信息

    private String dateString;
    private String Service;
    private String Stop;

    /*
    // ************* 步骤 2：拼接待签名字符串 *************
    String credentialScope = dateString + "/" + Service + "/" + Stop;
    private Object CanonicalRequest;
    String hashedCanonicalRequest = HashEncryption(CanonicalRequest);
    String stringToSign = Algorithm + "\n" +
            timestamp + "\n" +
            credentialScope + "\n" +
            hashedCanonicalRequest;

    // ************* 步骤 3：计算签名 *************
    byte[] secretDate = HashHmacSha256Encryption(("TC3" + SecretKey).getBytes("UTF-8"), dateString);
    byte[] secretService = HashHmacSha256Encryption(secretDate, Service);
    byte[] secretSigning = HashHmacSha256Encryption(secretService, Stop);

    // ************* 步骤 4：拼接 Authorization *************
    String authorization = Algorithm + ' ' +
            "Credential=" + SecretId + '/' + credentialScope + ", " +
            "SignedHeaders=" + SignedHeaders + ", " +
            "Signature=" + signature;
    */
}
