package com.test.risk;

/**
 * AI 智能风控 - 风险检查结果（API 返回结构）
 */
public class RiskResult {

    private boolean riskDetected;
    private String riskLevel;
    private double score;
    private String message;
    /** 评估来源：model=AI异常检测模型，rule=规则引擎 */
    private String riskSource;

    public RiskResult() {
    }

    public RiskResult(boolean riskDetected, String riskLevel, double score, String message) {
        this(riskDetected, riskLevel, score, message, "rule");
    }

    public RiskResult(boolean riskDetected, String riskLevel, double score, String message, String riskSource) {
        this.riskDetected = riskDetected;
        this.riskLevel = riskLevel;
        this.score = score;
        this.message = message;
        this.riskSource = riskSource != null ? riskSource : "rule";
    }

    public boolean isRiskDetected() {
        return riskDetected;
    }

    public void setRiskDetected(boolean riskDetected) {
        this.riskDetected = riskDetected;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRiskSource() {
        return riskSource;
    }

    public void setRiskSource(String riskSource) {
        this.riskSource = riskSource;
    }
}
