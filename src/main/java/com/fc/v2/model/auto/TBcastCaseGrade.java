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
 * 事故定档结论对象 t_bcast_case_grade
 *
 * <p>归层结论落库这一笔，顺手把两头的线代号与立线之日一并钉上；日后线册再怎么换法，
 * 这一笔也不回头改。这一趟没够上（settled=0）与后来补上（settled=1）是两笔，各留各的。</p>
 *
 * @author fuce
 * @date 2026-10-04
 */
@TableName("t_bcast_case_grade")
@ApiModel(value = "TBcastCaseGrade", description = "事故定档结论")
public class TBcastCaseGrade implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 事故处置单代号 */
    @TableField("biz_no")
    @ApiModelProperty(value = "事故处置单代号")
    private String bizNo;

    /** 事故回看那一刻（归层按这一刻查线；补算往期用往期自己那一刻） */
    @TableField("occur_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "事故回看那一刻")
    private Date occurAt;

    /** 本段停播分钟数（分钟那头的数） */
    @TableField("stop_minutes")
    @ApiModelProperty(value = "本段停播分钟数")
    private BigDecimal stopMinutes;

    /** 被影响到的户数（户数那头的数） */
    @TableField("affect_households")
    @ApiModelProperty(value = "被影响到的户数")
    private BigDecimal affectHouseholds;

    /** 归到第几层 1..3；没够上（一头无线/一头越封顶）时留白 */
    @TableField("grade")
    @ApiModelProperty(value = "归到第几层 1..3；没够上时留白")
    private Integer grade;

    /** 这笔落没落定 0没够上 1归层落定（没够上与后来补上各是一笔，不回改） */
    @TableField("settled")
    @ApiModelProperty(value = "这笔落没落定 0没够上 1归层落定")
    private Integer settled;

    /** 落库这一趟分钟线代号（钉死） */
    @TableField("minute_rule_code")
    @ApiModelProperty(value = "分钟线代号（钉死）")
    private String minuteRuleCode;

    /** 分钟线立线之日（钉死） */
    @TableField("minute_eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "分钟线立线之日（钉死）")
    private Date minuteEffStart;

    /** 落库这一趟户数线代号（钉死） */
    @TableField("house_rule_code")
    @ApiModelProperty(value = "户数线代号（钉死）")
    private String houseRuleCode;

    /** 户数线立线之日（钉死） */
    @TableField("house_eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "户数线立线之日（钉死）")
    private Date houseEffStart;

    /** 越限摆数：哪一头的数高过了自己那条线的封顶，把两头的数摆出来 */
    @TableField("exceed_note")
    @ApiModelProperty(value = "越限摆数")
    private String exceedNote;

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

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

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

    public Date getOccurAt() {
        return occurAt;
    }

    public void setOccurAt(Date occurAt) {
        this.occurAt = occurAt;
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

    public Integer getGrade() {
        return grade;
    }

    public void setGrade(Integer grade) {
        this.grade = grade;
    }

    public Integer getSettled() {
        return settled;
    }

    public void setSettled(Integer settled) {
        this.settled = settled;
    }

    public String getMinuteRuleCode() {
        return minuteRuleCode;
    }

    public void setMinuteRuleCode(String minuteRuleCode) {
        this.minuteRuleCode = minuteRuleCode;
    }

    public Date getMinuteEffStart() {
        return minuteEffStart;
    }

    public void setMinuteEffStart(Date minuteEffStart) {
        this.minuteEffStart = minuteEffStart;
    }

    public String getHouseRuleCode() {
        return houseRuleCode;
    }

    public void setHouseRuleCode(String houseRuleCode) {
        this.houseRuleCode = houseRuleCode;
    }

    public Date getHouseEffStart() {
        return houseEffStart;
    }

    public void setHouseEffStart(Date houseEffStart) {
        this.houseEffStart = houseEffStart;
    }

    public String getExceedNote() {
        return exceedNote;
    }

    public void setExceedNote(String exceedNote) {
        this.exceedNote = exceedNote;
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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
