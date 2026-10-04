package com.fc.v2.model.custom;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 历不对一条：老结论屏上还挂着线代号，可册子翻回事故那一日，那一头找不出在场线。
 *
 * <p>去年那两份照着已停用的线定下来的处置，要一眼看得出历不对——把这样的结论
 * 一条一条摆出来：哪一笔、事故在哪一刻、屏上仍挂着哪两个代号、是哪一头（分钟/户数）
 * 在当刻找不着在场线，全在本对象里点清楚。找不着分两种：一是当刻那一头一条在场线
 * 都没有；二是当刻在场的是另一条，钉着的代号已经不是它——钉着的线对不上当刻在场线，
 * 都算这一头历不对。</p>
 *
 * @author fuce
* @date 2026-10-04
 */
public class BcastGradeAnomaly implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 历不对的那笔结论主键 */
    private Long recordId;

    /** 事故处置单代号 */
    private String bizNo;

    /** 事故回看那一刻（复算就用它自己那一刻） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date occurAt;

    /** 笔上钉着的归层 */
    private Integer grade;

    /** 分钟那头实际数 */
    private BigDecimal stopMinutes;

    /** 户数那头实际数 */
    private BigDecimal affectHouseholds;

    /** 屏上仍挂着的分钟线代号 */
    private String pinnedMinuteCode;

    /** 屏上仍挂着的户数线代号 */
    private String pinnedHouseCode;

    /** 不对在哪一头：分钟 */
    private boolean minuteSideFault;

    /** 不对在哪一头：户数 */
    private boolean houseSideFault;

    /** 分钟那头历不对的说法：当刻无在场线，或在场的是另一条 */
    private String minuteFaultNote;

    /** 户数那头历不对的说法：当刻无在场线，或在场的是另一条 */
    private String houseFaultNote;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

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

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
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

    public String getPinnedMinuteCode() {
        return pinnedMinuteCode;
    }

    public void setPinnedMinuteCode(String pinnedMinuteCode) {
        this.pinnedMinuteCode = pinnedMinuteCode;
    }

    public String getPinnedHouseCode() {
        return pinnedHouseCode;
    }

    public void setPinnedHouseCode(String pinnedHouseCode) {
        this.pinnedHouseCode = pinnedHouseCode;
    }

    public boolean isMinuteSideFault() {
        return minuteSideFault;
    }

    public void setMinuteSideFault(boolean minuteSideFault) {
        this.minuteSideFault = minuteSideFault;
    }

    public boolean isHouseSideFault() {
        return houseSideFault;
    }

    public void setHouseSideFault(boolean houseSideFault) {
        this.houseSideFault = houseSideFault;
    }

    public String getMinuteFaultNote() {
        return minuteFaultNote;
    }

    public void setMinuteFaultNote(String minuteFaultNote) {
        this.minuteFaultNote = minuteFaultNote;
    }

    public String getHouseFaultNote() {
        return houseFaultNote;
    }

    public void setHouseFaultNote(String houseFaultNote) {
        this.houseFaultNote = houseFaultNote;
    }
}
