package com.example.lyw.androidsocket.bean;

/**
 * Created by lyw on 2017/11/12.
 */

public class FeedbackBean {
    private int stepId;
    private String feedback;

    public FeedbackBean(int stepId, String feedback) {
        this.stepId = stepId;
        this.feedback = feedback;
    }

    public int getStepId() {
        return stepId;
    }

    public void setStepId(int stepId) {
        this.stepId = stepId;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
