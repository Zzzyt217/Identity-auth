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

    /**
     * 贝叶斯更新后的风险后验概率 P(Risk|Evidence)，范围 [0,1]；
     * 匿名用户或未启用后验路径时为 null。
     */
    private Double posteriorProbability;

    public RiskResult() {
    }

    public RiskResult(boolean riskDetected, String riskLevel, double score, String message) {
        this(riskDetected, riskLevel, score, message, "rule");
    }

    public RiskResult(boolean riskDetected, String riskLevel, double score, String message, String riskSource) {
        this(riskDetected, riskLevel, score, message, riskSource, null);
    }

    public RiskResult(boolean riskDetected, String riskLevel, double score, String message, String riskSource,
                      Double posteriorProbability) {
        this.riskDetected = riskDetected;
        this.riskLevel = riskLevel;
        this.score = score;
        this.message = message;
        this.riskSource = riskSource != null ? riskSource : "rule";
        this.posteriorProbability = posteriorProbability;
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

    public Double getPosteriorProbability() {
        return posteriorProbability;
    }

    public void setPosteriorProbability(Double posteriorProbability) {
        this.posteriorProbability = posteriorProbability;
    }
}
