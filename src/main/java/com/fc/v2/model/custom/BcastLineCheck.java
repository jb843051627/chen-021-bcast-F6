package com.fc.v2.model.custom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 立线回执：三个分界数（起算、过半、封顶，按分钟/户计）缺一个就立不起来。
 *
 * <p>回执上不许光回一句"数不对"——空着的是哪一栏、没大过前头那一栏的是哪一栏、
 * 两栏各是多少，都得一条一条直接指出来。三个数齐了又从小到大连得上，{@link #valid}
 * 才为真，这时候线才立得起来。</p>
 *
 * @author fuce
 * @date 2026-10-04
 */
public class BcastLineCheck implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 被验的线代号 */
    private String ruleCode;

    /** 三个数齐了、且起算&lt;过半&lt;封顶，才立得起来 */
    private boolean valid;

    /** 空着的栏：元素为 起算/过半/封顶；没有空栏时为空表 */
    private List<String> blankFields = new ArrayList<String>();

    /** 没大过前头那一栏的毛病，一条一句话，把两栏的数都摆出来 */
    private List<String> orderFaults = new ArrayList<String>();

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getBlankFields() {
        return blankFields;
    }

    public void setBlankFields(List<String> blankFields) {
        this.blankFields = blankFields;
    }

    public List<String> getOrderFaults() {
        return orderFaults;
    }

    public void setOrderFaults(List<String> orderFaults) {
        this.orderFaults = orderFaults;
    }
}
