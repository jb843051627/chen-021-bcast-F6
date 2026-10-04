package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TBcastCaseGradeMapper;
import com.fc.v2.mapper.auto.TBcastPreLineMapper;
import com.fc.v2.model.auto.TBcastCaseGrade;
import com.fc.v2.model.auto.TBcastPreLine;
import com.fc.v2.model.custom.BcastGradeAnomaly;
import com.fc.v2.model.custom.BcastGradeVerdict;
import com.fc.v2.model.custom.BcastLineCheck;
import com.fc.v2.model.custom.BcastLinePick;
import com.fc.v2.service.ITBcastPreLineService;

/**
 * 事故定档门槛线 Service业务层处理（rule-eval 形状）。
 *
 * <p>在场口径全类只此一处（{@link #presentList}）：未删、未停用、立线之日已经够着、
 * 让位之日还没到（让位之日不含；留白按一直在场认），再按让位顺位降序、代号降序排定。
 * 定位、在场名单、条数、单条可用、归层、历不对复算，全从这一处出——屏上那份名单与
 * 逐条走一遍得到的名单，出处同一处。时段比较在 Java 内存里做（只按 del_flag 取数），
 * 不把 java.util.Date 绑进 SQL：JDBC 与本机时区不对称时，切换当日会整体错档。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TBcastPreLineServiceImpl implements ITBcastPreLineService {

    private static final Logger log = LoggerFactory.getLogger(TBcastPreLineServiceImpl.class);

    /** 线的情形：在场 */
    private static final int STATUS_ON = 0;
    /** 线的情形：已停用（退居册底：翻得到，不进判定） */
    private static final int STATUS_OFF = 1;

    /** 分界栏名，回执上直接点栏用 */
    private static final String FIELD_TH1 = "起算";
    private static final String FIELD_TH2 = "过半";
    private static final String FIELD_TH3 = "封顶";

    /** 顺位降序；并列按代号降序（排法落在末位的那条胜出：字符串序大者居末） */
    private static final Comparator<TBcastPreLine> PRIORITY_THEN_CODE_DESC = new Comparator<TBcastPreLine>() {
        @Override
        public int compare(TBcastPreLine a, TBcastPreLine b) {
            int pa = a.getPriority() == null ? 0 : a.getPriority().intValue();
            int pb = b.getPriority() == null ? 0 : b.getPriority().intValue();
            if (pa != pb) {
                return pb - pa;
            }
            String ca = a.getRuleCode() == null ? "" : a.getRuleCode();
            String cb = b.getRuleCode() == null ? "" : b.getRuleCode();
            return cb.compareTo(ca);
        }
    };

    @javax.annotation.Resource
    private TBcastPreLineMapper bcastPreLineMapper;

    @javax.annotation.Resource
    private TBcastCaseGradeMapper bcastCaseGradeMapper;

    @Override
    public TBcastPreLine selectTBcastPreLineById(Long id) {
        return id == null ? null : this.bcastPreLineMapper.selectById(id);
    }

    // ================= 老签名：一动不动 =================

    @Override
    public int evaluate(String ruleCode, BigDecimal input, Date at) {
        if (ruleCode == null || ruleCode.trim().isEmpty() || input == null || at == null
                || input.compareTo(BigDecimal.ZERO) < 0) {
            return 0;
        }
        // 老方法按代号查当刻在场的版本；查线之道仍只走在场这唯一口径，不开第二处
        for (TBcastPreLine r : presentList(at, null)) {
            if (ruleCode.equals(r.getRuleCode())) {
                return levelOf(r, input);
            }
        }
        return 0;
    }

    @Override
    public int evaluateTop(BigDecimal input, Date at) {
        if (input == null || at == null || input.compareTo(BigDecimal.ZERO) < 0) {
            return 0;
        }
        TBcastPreLine top = present(at, KIND_MINUTE);
        return top == null ? 0 : levelOf(top, input);
    }

    @Override
    public List<TBcastPreLine> listAvailable(Date at) {
        return at == null ? new ArrayList<TBcastPreLine>() : presentList(at, null);
    }

    @Override
    public boolean usable(Long id, Date at) {
        if (id == null || at == null) {
            return false;
        }
        TBcastPreLine r = this.bcastPreLineMapper.selectById(id);
        return r != null && isPresent(r, at);
    }

    @Override
    public int countAvailable(Date at) {
        return at == null ? 0 : presentList(at, null).size();
    }

    // ================= 新查线之道：另起的名字 =================

    @Override
    public BcastLinePick locateLine(Date at, int lineKind) {
        if (at == null || (lineKind != KIND_MINUTE && lineKind != KIND_HOUSE)) {
            return BcastLinePick.none(lineKind);
        }
        TBcastPreLine r = present(at, lineKind);
        if (r == null) {
            // 一条也套不上：结论栏写明无可用线，代号、立线之日两边一律留白
            return BcastLinePick.none(lineKind);
        }
        BcastLinePick pick = new BcastLinePick();
        pick.setUsable(true);
        pick.setWord(r.getRuleName() == null ? r.getRuleCode() : r.getRuleName());
        pick.setLineKind(Integer.valueOf(lineKind));
        pick.setRuleCode(r.getRuleCode());
        pick.setRuleName(r.getRuleName());
        pick.setEffStart(r.getEffStart());
        pick.setEffEnd(r.getEffEnd());
        pick.setPriority(r.getPriority());
        pick.setTh1Max(r.getTh1Max());
        pick.setTh2Max(r.getTh2Max());
        pick.setTh3Max(r.getTh3Max());
        return pick;
    }

    @Override
    public BcastLineCheck checkLine(TBcastPreLine line) {
        BcastLineCheck check = new BcastLineCheck();
        if (line == null) {
            check.setValid(false);
            return check;
        }
        check.setRuleCode(line.getRuleCode());

        // 先点空着的栏：三个数少一个就立不起来，光回一句"数不对"不算交代
        List<String> blanks = new ArrayList<String>();
        if (line.getTh1Max() == null) {
            blanks.add(FIELD_TH1);
        }
        if (line.getTh2Max() == null) {
            blanks.add(FIELD_TH2);
        }
        if (line.getTh3Max() == null) {
            blanks.add(FIELD_TH3);
        }
        check.setBlankFields(blanks);

        // 再点没大过前头那一栏的：两栏各是多少一并摆出来
        List<String> faults = new ArrayList<String>();
        if (line.getTh1Max() != null && line.getTh2Max() != null
                && line.getTh2Max().compareTo(line.getTh1Max()) <= 0) {
            faults.add(FIELD_TH2 + "没大过前头的" + FIELD_TH1 + "：" + FIELD_TH2 + "="
                    + line.getTh2Max().toPlainString() + "，" + FIELD_TH1 + "="
                    + line.getTh1Max().toPlainString());
        }
        if (line.getTh2Max() != null && line.getTh3Max() != null
                && line.getTh3Max().compareTo(line.getTh2Max()) <= 0) {
            faults.add(FIELD_TH3 + "没大过前头的" + FIELD_TH2 + "：" + FIELD_TH3 + "="
                    + line.getTh3Max().toPlainString() + "，" + FIELD_TH2 + "="
                    + line.getTh2Max().toPlainString());
        }
        check.setOrderFaults(faults);
        check.setValid(blanks.isEmpty() && faults.isEmpty());
        return check;
    }

    @Override
    public List<TBcastPreLine> listOnScreen(Date at) {
        return at == null ? new ArrayList<TBcastPreLine>() : presentList(at, null);
    }

    @Override
    public List<TBcastPreLine> listLedger() {
        // 册子里一条不漏：到期让位的、整条停用的都在，只滤删除；按立线之日降序翻
        return this.bcastPreLineMapper.selectList(new QueryWrapper<TBcastPreLine>()
                .eq("del_flag", 0)
                .orderByDesc("eff_start", "id"));
    }

    // ================= 归层出口：两头各看各的线 =================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BcastGradeVerdict grade(String bizNo, Date occurAt, BigDecimal stopMinutes,
                                  BigDecimal affectHouseholds) {
        BcastGradeVerdict v = new BcastGradeVerdict();
        v.setBizNo(bizNo);
        v.setOccurAt(occurAt);
        v.setStopMinutes(stopMinutes);
        v.setAffectHouseholds(affectHouseholds);
        if (bizNo == null || bizNo.trim().isEmpty() || occurAt == null) {
            // 连事故代号与回看那一刻都报不出来，无从查线，也不留笔
            return null;
        }

        BcastLinePick minutePick = locateLine(occurAt, KIND_MINUTE);
        BcastLinePick housePick = locateLine(occurAt, KIND_HOUSE);
        v.setMinuteLine(minutePick.isUsable() ? minutePick : null);
        v.setHouseLine(housePick.isUsable() ? housePick : null);

        List<String> reasons = new ArrayList<String>();
        int minuteLevel = sideCheck(minutePick, stopMinutes, "停播分钟", "分钟", reasons);
        int houseLevel = sideCheck(housePick, affectHouseholds, "影响户数", "户", reasons);

        TBcastCaseGrade rec = new TBcastCaseGrade();
        rec.setBizNo(bizNo.trim());
        rec.setOccurAt(occurAt);
        rec.setStopMinutes(stopMinutes);
        rec.setAffectHouseholds(affectHouseholds);
        rec.setDelFlag(Integer.valueOf(0));
        rec.setMinuteRuleCode(minutePick.getRuleCode());
        rec.setMinuteEffStart(minutePick.getEffStart());
        rec.setHouseRuleCode(housePick.getRuleCode());
        rec.setHouseEffStart(housePick.getEffStart());

        if (reasons.isEmpty()) {
            // 两头各自都不越封顶才算归到这一层；合到一处取更外的那一头
            int g = Math.max(minuteLevel, houseLevel);
            v.setSettled(true);
            v.setGrade(Integer.valueOf(g));
            rec.setGrade(Integer.valueOf(g));
            rec.setSettled(Integer.valueOf(1));
        } else {
            // 没够上：结论留白，哪一头的什么毛病写明，越限把两头的数摆出来
            v.setSettled(false);
            rec.setSettled(Integer.valueOf(0));
            String reason = joinReasons(reasons);
            v.setReason(reason);
            rec.setExceedNote(reason);
        }
        // 每一趟落一笔：没够上与后来补上各是各的，落库即钉死，不回头改
        this.bcastCaseGradeMapper.insert(rec);
        v.setRecordId(rec.getId());
        return v;
    }

    @Override
    public List<BcastGradeAnomaly> listGradeAnomalies() {
        List<TBcastCaseGrade> records = this.bcastCaseGradeMapper.selectList(
                new QueryWrapper<TBcastCaseGrade>()
                        .eq("del_flag", 0)
                        .eq("settled", 1)
                        .orderByAsc("occur_at", "id"));
        List<BcastGradeAnomaly> anomalies = new ArrayList<BcastGradeAnomaly>();
        for (TBcastCaseGrade rec : records) {
            if (rec.getOccurAt() == null) {
                continue;
            }
            BcastLinePick minuteNow = locateLine(rec.getOccurAt(), KIND_MINUTE);
            BcastLinePick houseNow = locateLine(rec.getOccurAt(), KIND_HOUSE);
            boolean minuteFault = !sameCode(rec.getMinuteRuleCode(), minuteNow);
            boolean houseFault = !sameCode(rec.getHouseRuleCode(), houseNow);
            if (!minuteFault && !houseFault) {
                continue;
            }
            BcastGradeAnomaly a = new BcastGradeAnomaly();
            a.setRecordId(rec.getId());
            a.setBizNo(rec.getBizNo());
            a.setOccurAt(rec.getOccurAt());
            a.setGrade(rec.getGrade());
            a.setStopMinutes(rec.getStopMinutes());
            a.setAffectHouseholds(rec.getAffectHouseholds());
            a.setPinnedMinuteCode(rec.getMinuteRuleCode());
            a.setPinnedHouseCode(rec.getHouseRuleCode());
            a.setMinuteSideFault(minuteFault);
            a.setHouseSideFault(houseFault);
            if (minuteFault) {
                a.setMinuteFaultNote(faultNote("停播分钟", rec.getMinuteRuleCode(), minuteNow));
            }
            if (houseFault) {
                a.setHouseFaultNote(faultNote("影响户数", rec.getHouseRuleCode(), houseNow));
            }
            anomalies.add(a);
        }
        return anomalies;
    }

    // ================= 在场口径：全类只此一处 =================

    /** at 时刻某一头在场、顺位最高的那一条；lineKind 为 null 时不挑头 */
    private TBcastPreLine present(Date at, Integer lineKind) {
        List<TBcastPreLine> list = presentList(at, lineKind);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 在场名单的唯一出处。条件：未删、未停用、立线之日已经够着（eff_start ≤ at）、
     * 让位之日还没到（eff_end 为空=一直在场；非空则 at &lt; eff_end，让位当日不含）。
     * 排定：让位顺位降序，顺位仍同再按代号降序（末位者胜）。
     */
    private List<TBcastPreLine> presentList(Date at, Integer lineKind) {
        if (at == null) {
            return new ArrayList<TBcastPreLine>();
        }
        List<TBcastPreLine> all = this.bcastPreLineMapper.selectList(
                new QueryWrapper<TBcastPreLine>().eq("del_flag", 0));
        List<TBcastPreLine> hit = new ArrayList<TBcastPreLine>();
        for (TBcastPreLine r : all) {
            if (lineKind != null
                    && (r.getLineKind() == null || r.getLineKind().intValue() != lineKind.intValue())) {
                continue;
            }
            if (isPresent(r, at)) {
                hit.add(r);
            }
        }
        Collections.sort(hit, PRIORITY_THEN_CODE_DESC);
        return hit;
    }

    /** 单条线在 at 时刻在不在场：停用线任何一刻都不作数；判定全走这一个条件 */
    private boolean isPresent(TBcastPreLine r, Date at) {
        if (r == null || at == null) {
            return false;
        }
        if (r.getStatus() != null && r.getStatus().intValue() == STATUS_OFF) {
            // 已停用的不进现行判定：册子里躺着给人翻，不许再选它出来比数
            return false;
        }
        if (r.getEffStart() == null || r.getEffStart().after(at)) {
            return false;
        }
        // 让位之日不含：旧线管到当日之前，新线自当日进场，交接当日只认一套说法
        if (r.getEffEnd() != null && !at.before(r.getEffEnd())) {
            return false;
        }
        return true;
    }

    // ================= 分档与两头合看 =================

    /**
     * 一头的核验：线在场、数报了且不为负、分界数立得住、不越封顶。
     * 有毛病往 reasons 里落一句（越限把实际数与封顶数都摆出来），返回 0；
     * 没毛病返回这一头落在哪一档（1..3）。
     */
    private int sideCheck(BcastLinePick pick, BigDecimal input, String sideName, String unit,
                          List<String> reasons) {
        if (!pick.isUsable()) {
            reasons.add(sideName + "那头无可用线（结论落" + BcastLinePick.WORD_NONE + "）");
            return 0;
        }
        if (input == null || input.compareTo(BigDecimal.ZERO) < 0) {
            reasons.add(sideName + "那头的数没报齐或为负，无法照线看");
            return 0;
        }
        BigDecimal th1 = pick.getTh1Max();
        BigDecimal th2 = pick.getTh2Max();
        BigDecimal th3 = pick.getTh3Max();
        if (th1 == null || th2 == null || th3 == null
                || th2.compareTo(th1) <= 0 || th3.compareTo(th2) <= 0) {
            reasons.add(sideName + "那头作数的线" + pick.getRuleCode() + "三个分界数立不住");
            return 0;
        }
        if (input.compareTo(th3) > 0) {
            // 哪一头越了，就把那一头的数摆出来：实际多少、封顶多少，别只回一句"不够"
            reasons.add(sideName + "那头" + input.toPlainString() + unit + "高过线" + pick.getRuleCode()
                    + "的封顶" + th3.toPlainString() + unit);
            return 0;
        }
        if (input.compareTo(th1) <= 0) {
            return 1;
        }
        if (input.compareTo(th2) <= 0) {
            return 2;
        }
        return 3;
    }

    /**
     * 单规则分档：等于上限取高一档（input≤起算落1…≤封顶落3，高过封顶落4）。
     * 三个分界数缺一个或没从小到大连上，这条线立不起来，返回 0。
     */
    private int levelOf(TBcastPreLine rule, BigDecimal input) {
        BigDecimal th1 = rule.getTh1Max();
        BigDecimal th2 = rule.getTh2Max();
        BigDecimal th3 = rule.getTh3Max();
        if (th1 == null || th2 == null || th3 == null
                || th2.compareTo(th1) <= 0 || th3.compareTo(th2) <= 0) {
            return 0;
        }
        if (input.compareTo(th1) <= 0) {
            return 1;
        }
        if (input.compareTo(th2) <= 0) {
            return 2;
        }
        if (input.compareTo(th3) <= 0) {
            return 3;
        }
        return 4;
    }

    /** 笔上钉着的代号，对不对得上当刻在场那条：对不上（含当刻一条也没有）即历不对 */
    private boolean sameCode(String pinnedCode, BcastLinePick now) {
        if (pinnedCode == null || !now.isUsable()) {
            return false;
        }
        return pinnedCode.equals(now.getRuleCode());
    }

    private String faultNote(String sideName, String pinnedCode, BcastLinePick now) {
        String pinned = pinnedCode == null ? "（代号留白）" : pinnedCode;
        if (!now.isUsable()) {
            return sideName + "那头事故当日册子里找不出在场线，屏上仍挂着" + pinned;
        }
        return sideName + "那头事故当日在场的已是" + now.getRuleCode() + "，屏上仍挂着" + pinned;
    }

    private String joinReasons(List<String> reasons) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) {
                sb.append("；");
            }
            sb.append(reasons.get(i));
        }
        return sb.toString();
    }
}
