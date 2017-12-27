package com.example.lyw.androidsocket.bean;

/**
 * Created by lyw on 2017/11/12.
 */

public class LoginBean {
    private String mobileNumber;
    private String password;

    public LoginBean(String mobileNumber, String password) {
        this.mobileNumber = mobileNumber;
        this.password = password;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
