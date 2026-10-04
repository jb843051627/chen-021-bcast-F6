package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fc.v2.model.auto.TBcastPreLine;

/**
 * 立线回执。
 *
 * <p>一条线三个分界数，从小到大依次叫起算、过半、封顶，少一个就立不起来；
 * 回执上把空着的那一栏、以及没大过前头那一栏的那一栏，一栏一条直接点出来——
 * 光回一句"数不对"不算交代。其余册子各栏（线名、代号、立线之日、让位顺位）
 * 空着的也逐栏点。指不出错处，线才落册，落册的那一条随回执带回。</p>
 *
 * @author fuce
 * @date 2026-10-04
 */
public class BcastPreLineCheckResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 立不立得起来：三档齐且逐档增大、各栏不缺，才作数 */
    private boolean success;

    /** 逐栏点出来的错处；没有错处是空表，不拿一句"数不对"搪塞 */
    private List<String> errors = new ArrayList<String>();

    /** 立得住时落册的那一条（回执与册子是同一条）；立不住时为空 */
    private TBcastPreLine line;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors == null ? new ArrayList<String>() : errors;
    }

    public TBcastPreLine getLine() {
        return line;
    }

    public void setLine(TBcastPreLine line) {
        this.line = line;
    }
}
