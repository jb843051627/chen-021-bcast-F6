package com.fc.v2.model.custom;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TBcastCaseGrade;

/**
 * 一起停播事故在某一刻的定档回话。四问只出在服务层那一个出口：
 * 按哪条线归层、当刻一条线也够不着时给不给说法、同顺位几条线听谁的、
 * 两头线合到一处取哪一头——回话全在这一个对象上，屏上不许先落答案再补依据。
 *
 * <p>本段停播分钟数按分钟线的三个分界数看，被影响户数另按户数线看，
 * 两头各自都不越才算归到这一层；哪头越了，{@link #overNotes} 把那一头的
 * 数与其封顶摆出来，不回一句"不够"。一条也套不上时不给结论：
 * {@link #gradeLevel} 留白、{@link #gradeWord} 落"无可用线"，两头线代号、
 * 立线之日四栏一律留白，不借旁边一条线糊答复。</p>
 *
 * @author fuce
 * @date 2026-10-04
 */
public class BcastGradeVerdict implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 结论栏用字：无可用线 */
    public static final String WORD_NO_LINE = "无可用线";
    /** 结论栏用字：某一头越了，不归层 */
    public static final String WORD_OVER = "不归档";
    /** 哪头越了：两头都不越 */
    public static final int OVER_NONE = 0;
    /** 哪头越了：停播分钟头 */
    public static final int OVER_MINUTES = 1;
    /** 哪头越了：被影响户头 */
    public static final int OVER_HOUSEHOLDS = 2;

    /** 停播事故代号 */
    private String bizNo;

    /** 按哪一刻回看 */
    private Date occAt;

    /** 本段停播分钟数 */
    private BigDecimal stopMinutes;

    /** 被影响到的户数 */
    private BigDecimal affectedQty;

    /** 给不给结论：两头线都在场、两头数都报了，才有档可归 */
    private boolean available;

    /** 归到哪一层 1..3；无可用线 / 越头不归层时留白 */
    private Integer gradeLevel;

    /** 结论栏：第一层..第三层、"不归档"或"无可用线" */
    private String gradeWord;

    /** 分钟头：钉的线代号、立线之日、这一头落在第几档（4 为越封顶） */
    private String mRuleCode;
    private Date mEffStart;
    private Integer mLevel;

    /** 户头：钉的线代号、立线之日、这一头落在第几档（4 为越封顶） */
    private String hRuleCode;
    private Date hEffStart;
    private Integer hLevel;

    /** 哪头越了：位标记，1 分钟头 2 户头，3 两头都越 */
    private int overSide = OVER_NONE;

    /** 越头交代：每条把越了那头的数与封顶摆出来 */
    private List<String> overNotes = new ArrayList<String>();

    /** 无可用线时逐句交代：哪头没线、哪头没报数 */
    private List<String> missingNotes = new ArrayList<String>();

    /** 这一趟回话落库的那一笔（线代号、立线之日随它钉死，只插不改） */
    private TBcastCaseGrade saved;

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public Date getOccAt() {
        return occAt;
    }

    public void setOccAt(Date occAt) {
        this.occAt = occAt;
    }

    public BigDecimal getStopMinutes() {
        return stopMinutes;
    }

    public void setStopMinutes(BigDecimal stopMinutes) {
        this.stopMinutes = stopMinutes;
    }

    public BigDecimal getAffectedQty() {
        return affectedQty;
    }

    public void setAffectedQty(BigDecimal affectedQty) {
        this.affectedQty = affectedQty;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getGradeWord() {
        return gradeWord;
    }

    public void setGradeWord(String gradeWord) {
        this.gradeWord = gradeWord;
    }

    public String getmRuleCode() {
        return mRuleCode;
    }

    public void setmRuleCode(String mRuleCode) {
        this.mRuleCode = mRuleCode;
    }

    public Date getmEffStart() {
        return mEffStart;
    }

    public void setmEffStart(Date mEffStart) {
        this.mEffStart = mEffStart;
    }

    public Integer getmLevel() {
        return mLevel;
    }

    public void setmLevel(Integer mLevel) {
        this.mLevel = mLevel;
    }

    public String gethRuleCode() {
        return hRuleCode;
    }

    public void sethRuleCode(String hRuleCode) {
        this.hRuleCode = hRuleCode;
    }

    public Date gethEffStart() {
        return hEffStart;
    }

    public void sethEffStart(Date hEffStart) {
        this.hEffStart = hEffStart;
    }

    public Integer gethLevel() {
        return hLevel;
    }

    public void sethLevel(Integer hLevel) {
        this.hLevel = hLevel;
    }

    public int getOverSide() {
        return overSide;
    }

    public void setOverSide(int overSide) {
        this.overSide = overSide;
    }

    public List<String> getOverNotes() {
        return overNotes;
    }

    public void setOverNotes(List<String> overNotes) {
        this.overNotes = overNotes;
    }

    public List<String> getMissingNotes() {
        return missingNotes;
    }

    public void setMissingNotes(List<String> missingNotes) {
        this.missingNotes = missingNotes;
    }

    public TBcastCaseGrade getSaved() {
        return saved;
    }

    public void setSaved(TBcastCaseGrade saved) {
        this.saved = saved;
    }
}
