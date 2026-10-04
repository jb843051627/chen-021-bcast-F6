package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 停播事故定档结论对象 t_bcast_case_grade
 *
 * <p>随结论一并入库的线代号、立线之日，落库时就钉死在这一笔上，日后线册再怎么
 * 换也不回头改这一笔。本表只许插、不许改：没够上与后来补上是两笔，各留各的。</p>
 *
 * @author fuce
 * @date 2026-10-04
 */
@TableName("t_bcast_case_grade")
@ApiModel(value = "TBcastCaseGrade", description = "停播事故定档结论")
public class TBcastCaseGrade implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 停播事故代号 */
    @TableField("biz_no")
    @ApiModelProperty(value = "停播事故代号")
    private String bizNo;

    /** 按哪一刻回看（事故当刻；补算往期用往期自己那一刻） */
    @TableField("occ_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "按哪一刻回看")
    private Date occAt;

    /** 本段停播分钟数 */
    @TableField("stop_minutes")
    @ApiModelProperty(value = "本段停播分钟数")
    private BigDecimal stopMinutes;

    /** 被影响到的户数 */
    @TableField("affected_qty")
    @ApiModelProperty(value = "被影响到的户数")
    private BigDecimal affectedQty;

    /** 归到哪一层 1..4；无可用线时留白 */
    @TableField("grade_level")
    @ApiModelProperty(value = "归到哪一层 1..4；无可用线留白")
    private Integer gradeLevel;

    /** 结论栏：第一层..第四层，或无可用线 */
    @TableField("grade_word")
    @ApiModelProperty(value = "结论栏")
    private String gradeWord;

    /** 哪头越了 0两头都不越 1停播分钟头越了 2被影响户头越了 */
    @TableField("over_side")
    @ApiModelProperty(value = "哪头越了 0两头都不越 1分钟头 2户头")
    private Integer overSide;

    /** 越头交代：越了那头的数与其界数落在这里 */
    @TableField("over_note")
    @ApiModelProperty(value = "越头交代")
    private String overNote;

    /** 钉死的分钟线代号（无可用线时留白） */
    @TableField("m_rule_code")
    @ApiModelProperty(value = "分钟线代号（钉死）")
    private String mRuleCode;

    /** 钉死的分钟线立线之日（无可用线时留白） */
    @TableField("m_eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "分钟线立线之日（钉死）")
    private Date mEffStart;

    /** 钉死的户数线代号（无可用线时留白） */
    @TableField("h_rule_code")
    @ApiModelProperty(value = "户数线代号（钉死）")
    private String hRuleCode;

    /** 钉死的户数线立线之日（无可用线时留白） */
    @TableField("h_eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "户数线立线之日（钉死）")
    private Date hEffStart;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getOverSide() {
        return overSide;
    }

    public void setOverSide(Integer overSide) {
        this.overSide = overSide;
    }

    public String getOverNote() {
        return overNote;
    }

    public void setOverNote(String overNote) {
        this.overNote = overNote;
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

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
