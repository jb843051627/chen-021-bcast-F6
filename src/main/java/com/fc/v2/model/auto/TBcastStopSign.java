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
 * 停播检修核准单落名底账对象 t_bcast_stop_sign
 *
 * <p>一笔落名独立成行。屏上的"已记几人"只当展示，真正作数的是这册底账点出来的条数。
 * 退回一档时，本档这一轮已点下的名当场清零——不是抹行，是把这几行冠上"沉底"另放，
 * 早先各档的旧名一笔不动；重走另起一轮，新旧两拨分开记，哪日补的各留各的底。</p>
 *
 * @author fuce
 * @date 2026-10-03
 */
@TableName("t_bcast_stop_sign")
@ApiModel(value = "TBcastStopSign", description = "停播检修核准单落名底账")
public class TBcastStopSign implements Serializable {
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

    /** 停播检修核准单代号（与单子上的单号一并誊来，翻账不必再绕回去对） */
    @TableField("bill_no")
    @ApiModelProperty(value = "停播检修核准单代号")
    private String billNo;

    /** 落在哪一档 0..2（台站/市广电局/省局） */
    @TableField("node_no")
    @ApiModelProperty(value = "落在哪一档 0..2（台站/市广电局/省局）")
    private Integer nodeNo;

    /** 这一档走到第几轮（退一回便加一轮；头一轮为1，新旧两拨分开记） */
    @TableField("round_no")
    @ApiModelProperty(value = "这一档走到第几轮（退一回加一轮，头一轮为1）")
    private Integer roundNo;

    /** 档内名次：这一档这一轮里头一名、第二名……按落名先后排 */
    @TableField("seq_no")
    @ApiModelProperty(value = "档内名次：这一档这一轮里的第几名")
    private Integer seqNo;

    /** 落名人 */
    @TableField("signer")
    @ApiModelProperty(value = "落名人")
    private String signer;

    /** 落名时记的话（可空） */
    @TableField("comment")
    @ApiModelProperty(value = "落名时记的话")
    private String comment;

    /** 落名种类 0点齐落名 1否决落名 */
    @TableField("kind")
    @ApiModelProperty(value = "落名种类 0点齐落名 1否决落名")
    private Integer kind;

    /** 行的情形 0在册（作数） 1沉底（退回时本档当轮的名压下来，不再作数） */
    @TableField("alive_flag")
    @ApiModelProperty(value = "行的情形 0在册作数 1沉底不再作数")
    private Integer aliveFlag;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 落名时刻（哪日补的各留各的底，就看这一刻） */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "落名时刻")
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getKind() {
        return kind;
    }

    public void setKind(Integer kind) {
        this.kind = kind;
    }

    public Integer getAliveFlag() {
        return aliveFlag;
    }

    public void setAliveFlag(Integer aliveFlag) {
        this.aliveFlag = aliveFlag;
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
