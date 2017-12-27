package com.example.lyw.androidsocket.bean;

/**
 * Created by lyw on 2017/11/12.
 */

public class ResultBean {
    private int stepId;
    private boolean accepted;

    public ResultBean(int stepId, boolean accepted) {
        this.stepId = stepId;
        this.accepted = accepted;
    }

    public int getStepId() {
        return stepId;
    }

    public void setStepId(int stepId) {
        this.stepId = stepId;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
}
