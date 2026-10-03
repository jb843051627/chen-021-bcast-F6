package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TBcastStopSign;

/**
 * 停播检修核准单·排期对账结果。
 *
 * <p>省局点齐、单子排定之后，事后翻查就用这一份：自省局往回收数到台站，一块一块清点。
 * 每一档一块，块上带着该档应点几人、底账当前轮在册几笔、排期名册几笔，以及逐笔的
 * 对得上对不上；总数与任一块都经得起单看。核准结果与排期名册出自同一次算——名册每一行
 * 都拴着落名底账上的那一笔（signId），不是一处取数、一处再手填。</p>
 *
 * @author fuce
 * @date 2026-10-03
 */
public class BcastStopScheduleAudit implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 核准单主键 */
    private Long billId;

    /** 核准单代号 */
    private String billNo;

    /** 是否已排定（省局点齐、名册已誊） */
    private boolean scheduled;

    /** 三道块并起来是否笔笔对得上（总数与任一块都平） */
    private boolean balanced;

    /** 三道合计应点人数（台站1、市2、省2） */
    private int requiredCount;

    /** 底账当前轮在册名总笔数 */
    private int ledgerCount;

    /** 排期名册总笔数 */
    private int scheduleCount;

    /** 三块账：自省局往回收数到台站的次序排（省局→市广电局→台站） */
    private List<Block> blocks = new ArrayList<Block>();

    /** 全程底账：沉底的旧轮、否决的那笔也留在里头，哪日补的各留各的底 */
    private List<TBcastStopSign> history = new ArrayList<TBcastStopSign>();

    /** 对不上时逐句交代：哪一档、差的是名册还是底账、差几笔 */
    private List<String> mismatches = new ArrayList<String>();

    /**
     * 一档一块。任取一块单看：应点几人、底账几笔、名册几笔、逐笔拴不拴得住，都在这一块上。
     */
    public static class Block implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 档口号 0..2 */
        private int nodeNo;

        /** 档口名（台站/市广电局/省局） */
        private String nodeName;

        /** 排定时作数的是第几轮 */
        private int roundNo;

        /** 该档应点人数 */
        private int needCount;

        /** 底账当前轮在册名笔数 */
        private int signCount;

        /** 排期名册该档笔数 */
        private int scheduleCount;

        /** 这一块自身对不对得上 */
        private boolean balanced;

        /** 逐笔对照（按档内名次排） */
        private List<Entry> entries = new ArrayList<Entry>();

        public int getNodeNo() {
            return nodeNo;
        }

        public void setNodeNo(int nodeNo) {
            this.nodeNo = nodeNo;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        public int getRoundNo() {
            return roundNo;
        }

        public void setRoundNo(int roundNo) {
            this.roundNo = roundNo;
        }

        public int getNeedCount() {
            return needCount;
        }

        public void setNeedCount(int needCount) {
            this.needCount = needCount;
        }

        public int getSignCount() {
            return signCount;
        }

        public void setSignCount(int signCount) {
            this.signCount = signCount;
        }

        public int getScheduleCount() {
            return scheduleCount;
        }

        public void setScheduleCount(int scheduleCount) {
            this.scheduleCount = scheduleCount;
        }

        public boolean isBalanced() {
            return balanced;
        }

        public void setBalanced(boolean balanced) {
            this.balanced = balanced;
        }

        public List<Entry> getEntries() {
            return entries;
        }

        public void setEntries(List<Entry> entries) {
            this.entries = entries;
        }
    }

    /**
     * 一笔对照：底账那一笔与名册那一行拴不拴得住。
     */
    public static class Entry implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 档内名次 */
        private int seqNo;

        /** 落名人 */
        private String signer;

        /** 底账行主键 */
        private Long signId;

        /** 名册行主键（底账有、名册缺时为空） */
        private Long scheduleId;

        /** 这一笔两边拴不拴得住 */
        private boolean matched;

        public Entry() {
        }

        public Entry(int seqNo, String signer, Long signId, Long scheduleId, boolean matched) {
            this.seqNo = seqNo;
            this.signer = signer;
            this.signId = signId;
            this.scheduleId = scheduleId;
            this.matched = matched;
        }

        public int getSeqNo() {
            return seqNo;
        }

        public void setSeqNo(int seqNo) {
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

        public Long getScheduleId() {
            return scheduleId;
        }

        public void setScheduleId(Long scheduleId) {
            this.scheduleId = scheduleId;
        }

        public boolean isMatched() {
            return matched;
        }

        public void setMatched(boolean matched) {
            this.matched = matched;
        }
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

    public boolean isScheduled() {
        return scheduled;
    }

    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }

    public int getRequiredCount() {
        return requiredCount;
    }

    public void setRequiredCount(int requiredCount) {
        this.requiredCount = requiredCount;
    }

    public int getLedgerCount() {
        return ledgerCount;
    }

    public void setLedgerCount(int ledgerCount) {
        this.ledgerCount = ledgerCount;
    }

    public int getScheduleCount() {
        return scheduleCount;
    }

    public void setScheduleCount(int scheduleCount) {
        this.scheduleCount = scheduleCount;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<Block> blocks) {
        this.blocks = blocks;
    }

    public List<TBcastStopSign> getHistory() {
        return history;
    }

    public void setHistory(List<TBcastStopSign> history) {
        this.history = history;
    }

    public List<String> getMismatches() {
        return mismatches;
    }

    public void setMismatches(List<String> mismatches) {
        this.mismatches = mismatches;
    }
}
