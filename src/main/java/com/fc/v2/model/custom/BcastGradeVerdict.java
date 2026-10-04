package com.fc.v2.model.custom;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 一起停播事故的归层回话（四问的结论全出服务层那一个出口）。
 *
 * <p>本段停播分钟数按分钟那条线看，被影响到的户数另按户数那条线看，两头各自都不越
 * 自己那条线的封顶，才算归到这一层，{@link #settled} 为真、{@link #grade} 落 1..3
 * （取两头各自落档里更外的那一头）。哪一头套不上在场线、或哪一头的数高过了封顶，
 * 都不归层：{@link #settled} 为假、{@link #grade} 留白，{@link #reason} 写明是哪一头
 * 的什么毛病，越限的把"那头的数/封顶的数"摆出来，不许只回一句"不够"。</p>
 *
 * <p>回话里的线代号、立线之日就是落库钉死的那四个栏位；这一趟没够上也照样留一笔，
 * 与后来补上的一笔各是各的，日后线册换法两笔都不回头改。{@link #recordId} 是这一趟
 * 落库那一笔的主键。</p>
 *
 * @author fuce
 * @date 2026-10-04
 */
public class BcastGradeVerdict implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 事故处置单代号 */
    private String bizNo;

    /** 按哪一刻回看的 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date occurAt;

    /** 这一趟落库那一笔的主键 */
    private Long recordId;

    /** 归层落定才为真；没够上（一头无可用线/一头越封顶）为假 */
    private boolean settled;

    /** 归到第几层 1..3；没够上留白 */
    private Integer grade;

    /** 没够上的缘由：哪一头、什么毛病，越限把两个数都摆出来；落定时为空 */
    private String reason;

    /** 分钟那头这一趟作数的线（无可用线时为 null） */
    private BcastLinePick minuteLine;

    /** 户数那头这一趟作数的线（无可用线时为 null） */
    private BcastLinePick houseLine;

    /** 分钟那头实际数 */
    private BigDecimal stopMinutes;

    /** 户数那头实际数 */
    private BigDecimal affectHouseholds;

    public String getBizNo() {
        return bizNo;
    }

    public void setBizNo(String bizNo) {
        this.bizNo = bizNo;
    }

    public Date getOccurAt() {
        return occurAt;
    }

    public void setOccurAt(Date occurAt) {
        this.occurAt = occurAt;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public boolean isSettled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public BcastLinePick getMinuteLine() {
        return minuteLine;
    }

    public void setMinuteLine(BcastLinePick minuteLine) {
        this.minuteLine = minuteLine;
    }

    public BcastLinePick getHouseLine() {
        return houseLine;
    }

    public void setHouseLine(BcastLinePick houseLine) {
        this.houseLine = houseLine;
    }

    public BigDecimal getStopMinutes() {
        return stopMinutes;
    }

    public void setStopMinutes(BigDecimal stopMinutes) {
        this.stopMinutes = stopMinutes;
    }

    public BigDecimal getAffectHouseholds() {
        return affectHouseholds;
    }

    public void setAffectHouseholds(BigDecimal affectHouseholds) {
        this.affectHouseholds = affectHouseholds;
    }
}
