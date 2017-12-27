package com.example.lyw.androidsocket.bean;

/**
 * Created by lyw on 2017/11/12.
 */

public class SC01MsgVo {
    private int stepId;
    //private String stepName;
    private String stepCNName;
    private String stepENName;
    private String stepSound;
    private int commandType;
    private int waitTime;

    public String getStepCNName() {
        return stepCNName;
    }

    public void setStepCNName(String stepCNName) {
        this.stepCNName = stepCNName;
    }

    public String getStepENName() {
        return stepENName;
    }

    public void setStepENName(String stepENName) {
        this.stepENName = stepENName;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public int getStepId() {
        return stepId;
    }

    public void setStepId(int stepId) {
        this.stepId = stepId;
    }

    public String getStepSound() {
        return stepSound;
    }

    public void setStepSound(String stepSound) {
        this.stepSound = stepSound;
    }

    public int getCommandType() {
        return commandType;
    }

    public void setCommandType(int commandType) {
        this.commandType = commandType;
    }
}
