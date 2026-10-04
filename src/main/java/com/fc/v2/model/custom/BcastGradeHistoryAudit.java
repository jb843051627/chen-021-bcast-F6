package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TBcastCaseGrade;

/**
 * 历不对核账：把早年落库的定档结论一笔笔翻出来，拿结论里钉着的线代号回册子
 * 对——结论落定那一刻在场的线，如今在册子那一日还找不找得着在场的同代号线。
 *
 * <p>去年那两份是照着早已停用的线定下来的：屏上还挂着线代号，册子在那一日却
 * 找不出在场线。这样的笔一笔一条摆在 {@link #mismatches} 里，一眼看得出历不对。
 * 钉死的那一笔不回头改，只摆出来；账平（一笔不对的都没有）时 {@link #balanced}
 * 为真、清单为空。</p>
 *
 * @author fuce
 * @date 2026-10-04
 */
public class BcastGradeHistoryAudit implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 账上一共翻了几笔（落库的结论总数） */
    private int totalRows;

    /** 历不对的笔数 */
    private int mismatchRows;

    /** 平不平：一笔对不上的都没有才算平 */
    private boolean balanced;

    /** 历不对的笔，一笔一条（含逐头交代） */
    private List<Item> mismatches = new ArrayList<Item>();

    /**
     * 一笔历不对：结论行原样带着，下面逐头交代是哪头的代号在册子那一日找不着在场线。
     */
    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 屏上还挂着代号、册子那一日却对不上的那一笔 */
        private TBcastCaseGrade grade;

        /** 逐头交代，如"分钟线 M-2018：2025-03-01 当日册中找不出在场线（已停用/已让位）" */
        private List<String> notes = new ArrayList<String>();

        public Item() {
        }

        public Item(TBcastCaseGrade grade, List<String> notes) {
            this.grade = grade;
            this.notes = notes;
        }

        public TBcastCaseGrade getGrade() {
            return grade;
        }

        public void setGrade(TBcastCaseGrade grade) {
            this.grade = grade;
        }

        public List<String> getNotes() {
            return notes;
        }

        public void setNotes(List<String> notes) {
            this.notes = notes;
        }
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getMismatchRows() {
        return mismatchRows;
    }

    public void setMismatchRows(int mismatchRows) {
        this.mismatchRows = mismatchRows;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }

    public List<Item> getMismatches() {
        return mismatches;
    }

    public void setMismatches(List<Item> mismatches) {
        this.mismatches = mismatches == null ? new ArrayList<Item>() : mismatches;
    }
}
