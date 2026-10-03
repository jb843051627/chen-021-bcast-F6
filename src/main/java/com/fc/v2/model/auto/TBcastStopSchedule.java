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
import java.util.Date;

/**
 * 停播检修排期名册对象 t_bcast_stop_schedule
 *
 * <p>省局点齐、核准单排定那一瞬，从落名底账当前轮的在册名一笔誊成。名册与核准结果
 * 出自同一次算：不是一处取数、一处再手填。每行拴着它底账上的那一笔（sign_id），
 * 事后从省局往回收数到台站，一块一块清点都对得回原笔。</p>
 *
 * @author fuce
 * @date 2026-10-03
 */
@TableName("t_bcast_stop_schedule")
@ApiModel(value = "TBcastStopSchedule", description = "停播检修排期名册")
public class TBcastStopSchedule implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 所属核准单主键 */
    @TableField("bill_id")
    @ApiModelProperty(value = "所属核准单主键")
    private Long billId;

    /** 停播检修核准单代号 */
    @TableField("bill_no")
    @ApiModelProperty(value = "停播检修核准单代号")
    private String billNo;

    /** 这一笔原落在哪一档 0..2（台站/市广电局/省局） */
    @TableField("node_no")
    @ApiModelProperty(value = "这一笔原落在哪一档 0..2")
    private Integer nodeNo;

    /** 这一档排定时作数的是第几轮 */
    @TableField("round_no")
    @ApiModelProperty(value = "这一档排定时作数的轮次")
    private Integer roundNo;

    /** 档内名次 */
    @TableField("seq_no")
    @ApiModelProperty(value = "档内名次")
    private Integer seqNo;

    /** 落名人（排期名单上的名） */
    @TableField("signer")
    @ApiModelProperty(value = "落名人")
    private String signer;

    /** 所对的落名底账那一笔：名册与底账逐笔拴死 */
    @TableField("sign_id")
    @ApiModelProperty(value = "所对的落名底账行主键")
    private Long signId;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 誊入名册者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "誊入名册者")
    private String createBy;

    /** 誊入名册时刻（核准结果与名册同一次算成的那一刻） */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "誊入名册时刻")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public Integer getNodeNo() {
        return nodeNo;
    }

    public void setNodeNo(Integer nodeNo) {
        this.nodeNo = nodeNo;
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(Integer roundNo) {
        this.roundNo = roundNo;
    }

    public Integer getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(Integer seqNo) {
        this.seqNo = seqNo;
    }

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
    }

    public Long getSignId() {
        return signId;
    }

    public void setSignId(Long signId) {
        this.signId = signId;
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
}
