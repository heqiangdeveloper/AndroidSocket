package com.example.lyw.androidsocket.bean;

/**
 * Created by lyw on 2017/11/12.
 */

public class LoginBackBean {
    private String mobileNumber;
    private boolean allowLogin;

    public LoginBackBean(String mobileNumber, boolean allowLogin) {
        this.mobileNumber = mobileNumber;
        this.allowLogin = allowLogin;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public boolean isAllowLogin() {
        return allowLogin;
    }

    public void setAllowLogin(boolean allowLogin) {
        this.allowLogin = allowLogin;
    }
}
