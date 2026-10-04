package com.fc.v2.model.custom;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 一条线在某一刻的定位回话。
 *
 * <p>四问里的头一问——"一起事故在某一刻按哪条线归层"——回话只许出在服务层那一个出口，
 * 形状就是本类：在场线找着了，{@link #usable} 为真，线代号、立线之日连同三个分界数一起
 * 带回；一条也套不上时，{@link #usable} 为假、{@link #word} 写明"无可用线"，
 * 代号、立线之日两边一律留白（null），不许借旁边一条线的数字糊一个看着合理的答复。</p>
 *
 * @author fuce
 * @date 2026-10-04
 */
public class BcastLinePick implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 回话用字：无可用线（结论栏落这四个字，代号与立线之日两边留白） */
    public static final String WORD_NONE = "无可用线";

    /** 这一刻这一头有没有作数的在场线 */
    private boolean usable;

    /** 结论用字：作数时落线名/代号那一路，套不上时落"无可用线" */
    private String word;

    /** 线管哪一头 1停播分钟 2影响户数 */
    private Integer lineKind;

    /** 作数的线代号；无可用线时留白 */
    private String ruleCode;

    /** 线名 */
    private String ruleName;

    /** 立线之日；无可用线时留白 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date effStart;

    /** 让位之日（不含；留白即一直在场） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date effEnd;

    /** 让位顺位 */
    private Integer priority;

    /** 起算 */
    private BigDecimal th1Max;
    /** 过半 */
    private BigDecimal th2Max;
    /** 封顶 */
    private BigDecimal th3Max;

    public static BcastLinePick none(int lineKind) {
        BcastLinePick p = new BcastLinePick();
        p.usable = false;
        p.word = WORD_NONE;
        p.lineKind = Integer.valueOf(lineKind);
        return p;
    }

    public boolean isUsable() {
        return usable;
    }

    public void setUsable(boolean usable) {
        this.usable = usable;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Integer getLineKind() {
        return lineKind;
    }

    public void setLineKind(Integer lineKind) {
        this.lineKind = lineKind;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Date getEffStart() {
        return effStart;
    }

    public void setEffStart(Date effStart) {
        this.effStart = effStart;
    }

    public Date getEffEnd() {
        return effEnd;
    }

    public void setEffEnd(Date effEnd) {
        this.effEnd = effEnd;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public BigDecimal getTh1Max() {
        return th1Max;
    }

    public void setTh1Max(BigDecimal th1Max) {
        this.th1Max = th1Max;
    }

    public BigDecimal getTh2Max() {
        return th2Max;
    }

    public void setTh2Max(BigDecimal th2Max) {
        this.th2Max = th2Max;
    }

    public BigDecimal getTh3Max() {
        return th3Max;
    }

    public void setTh3Max(BigDecimal th3Max) {
        this.th3Max = th3Max;
    }
}
