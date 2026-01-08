package com.czx.tencentv3ocr;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class IdentifyResult implements Serializable {
    private int errorCode;
    private String errorMsg;
    @SerializedName("Name") private String name;
    @SerializedName("Sex") private String sex;
    @SerializedName("Nation") private String nation;
    @SerializedName("Birth") private String birth;
    @SerializedName("Address") private String address;
    @SerializedName("IdNum") private String id;

    // Getter & Setter
    public int getErrorCode() { return errorCode; }
    public void setErrorCode(int errorCode) { this.errorCode = errorCode; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }
    public String getNation() { return nation; }
    public void setNation(String nation) { this.nation = nation; }
    public String getBirth() { return birth; }
    public void setBirth(String birth) { this.birth = birth; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
}