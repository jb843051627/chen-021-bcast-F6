package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TBcastCaseGradeMapper;
import com.fc.v2.model.auto.TBcastCaseGrade;
import com.fc.v2.model.auto.TBcastPreLine;
import com.fc.v2.model.custom.BcastGradeHistoryAudit;
import com.fc.v2.model.custom.BcastGradeVerdict;
import com.fc.v2.service.ITBcastCaseGradeService;
import com.fc.v2.service.ITBcastPreLineService;

/**
 * 停播事故定档 Service业务层处理（single-exit 形状）。
 *
 * <p>四问只出 {@link #gradeAndPersist} 一个出口：按哪条线归层（at 当刻在场、
 * 顺位/代号排定的那条）、一条也够不着时给不给说法（落"无可用线"，四栏留白）、
 * 同顺位听谁的（线册那一处已定，这里不另说）、两头线合到一处取哪一头（两头
 * 各自比完，取落得高的那一层）。查册一律委托线册服务，不在本层另数一回——
 * 屏上名单与逐条走查同源，回话才不会两个样。</p>
 *
 * <p>结论落库是只插不改的底账：线代号与立线之日随这一趟出口的回话钉死，
 * 日后线册再怎么换也不回头改这一笔；没够上（无可用线/越头）与后来补上
 * 各落各的笔。</p>
 *
 * @author fuce
 * @date 2026-10-04
 */
@Service
public class TBcastCaseGradeServiceImpl implements ITBcastCaseGradeService {

    private static final Logger log = LoggerFactory.getLogger(TBcastCaseGradeServiceImpl.class);

    /** 一头落在第 4 档，即高过这头线的封顶，这头越了 */
    private static final int LEVEL_OVER_CAP = 4;

    @javax.annotation.Resource
    private TBcastCaseGradeMapper bcastCaseGradeMapper;

    @javax.annotation.Resource
    private ITBcastPreLineService bcastPreLineService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BcastGradeVerdict gradeAndPersist(String bizNo, Date occAt, BigDecimal stopMinutes,
                                             BigDecimal affectedQty) {
        // 先讲按哪一刻回看：没那一刻、没事故代号，整趟回话立不起来，也不落笔
        if (bizNo == null || bizNo.trim().isEmpty() || occAt == null) {
            return null;
        }
        BcastGradeVerdict v = new BcastGradeVerdict();
        v.setBizNo(bizNo.trim());
        v.setOccAt(occAt);
        v.setStopMinutes(stopMinutes);
        v.setAffectedQty(affectedQty);

        // 两头各自定位当刻在场的那条线（同刻同顺位听谁的，线册那一处已排定）
        TBcastPreLine mLine = bcastPreLineService.resolveLineByKindAt(occAt,
                ITBcastPreLineService.KIND_MINUTES);
        TBcastPreLine hLine = bcastPreLineService.resolveLineByKindAt(occAt,
                ITBcastPreLineService.KIND_HOUSEHOLDS);

        boolean mReady = fillSide(v, true, mLine, stopMinutes, occAt);
        boolean hReady = fillSide(v, false, hLine, affectedQty, occAt);

        TBcastCaseGrade rec = new TBcastCaseGrade();
        rec.setBizNo(v.getBizNo());
        rec.setOccAt(occAt);
        rec.setStopMinutes(stopMinutes);
        rec.setAffectedQty(affectedQty);
        rec.setDelFlag(Integer.valueOf(0));

        if (!mReady || !hReady) {
            // 一条也套不上：不给结论。代号、立线之日两边一律留白，不许借旁线糊答复
            v.setAvailable(false);
            v.setGradeLevel(null);
            v.setGradeWord(BcastGradeVerdict.WORD_NO_LINE);
            rec.setGradeLevel(null);
            rec.setGradeWord(BcastGradeVerdict.WORD_NO_LINE);
            rec.setOverSide(Integer.valueOf(BcastGradeVerdict.OVER_NONE));
            rec.setRemark(join(v.getMissingNotes()));
            log.info("事故{}于{}定档：无可用线（{}）", v.getBizNo(), day(occAt), v.getMissingNotes());
            bcastCaseGradeMapper.insert(rec);
            v.setSaved(rec);
            return v;
        }

        // 两头都有线有数：越没越封顶，逐头摆数，不回一句"不够"
        boolean mOver = Integer.valueOf(LEVEL_OVER_CAP).equals(v.getmLevel());
        boolean hOver = Integer.valueOf(LEVEL_OVER_CAP).equals(v.gethLevel());
        int overSide = BcastGradeVerdict.OVER_NONE;
        if (mOver) {
            overSide |= BcastGradeVerdict.OVER_MINUTES;
            v.getOverNotes().add(overNote("停播分钟头", stopMinutes, mLine));
        }
        if (hOver) {
            overSide |= BcastGradeVerdict.OVER_HOUSEHOLDS;
            v.getOverNotes().add(overNote("被影响户头", affectedQty, hLine));
        }

        // 两头线都真真切切在场：这一趟出口认了哪两条，代号与立线之日就钉哪两条
        v.setmRuleCode(mLine.getRuleCode());
        v.setmEffStart(mLine.getEffStart());
        v.sethRuleCode(hLine.getRuleCode());
        v.sethEffStart(hLine.getEffStart());
        rec.setmRuleCode(mLine.getRuleCode());
        rec.setmEffStart(mLine.getEffStart());
        rec.sethRuleCode(hLine.getRuleCode());
        rec.sethEffStart(hLine.getEffStart());

        if (overSide != BcastGradeVerdict.OVER_NONE) {
            // 哪一头越了就摆哪一头的数，不归层；钉死的还是这一趟在场的两条线
            v.setAvailable(false);
            v.setOverSide(overSide);
            v.setGradeLevel(null);
            v.setGradeWord(BcastGradeVerdict.WORD_OVER);
            rec.setOverSide(Integer.valueOf(overSide));
            rec.setGradeLevel(null);
            rec.setGradeWord(BcastGradeVerdict.WORD_OVER);
            rec.setOverNote(join(v.getOverNotes()));
            log.info("事故{}于{}定档：{}不归层", v.getBizNo(), day(occAt), v.getOverNotes());
            bcastCaseGradeMapper.insert(rec);
            v.setSaved(rec);
            return v;
        }

        // 两头各自都不越才算归到这一层；合到一处取两头落得高的那一层
        int level = Math.max(v.getmLevel().intValue(), v.gethLevel().intValue());
        String word = "第" + CN[level - 1] + "层";
        v.setAvailable(true);
        v.setOverSide(BcastGradeVerdict.OVER_NONE);
        v.setGradeLevel(Integer.valueOf(level));
        v.setGradeWord(word);
        rec.setOverSide(Integer.valueOf(BcastGradeVerdict.OVER_NONE));
        rec.setGradeLevel(Integer.valueOf(level));
        rec.setGradeWord(word);
        bcastCaseGradeMapper.insert(rec);
        v.setSaved(rec);
        return v;
    }

    @Override
    public List<TBcastCaseGrade> listGrades() {
        return this.bcastCaseGradeMapper.selectList(new QueryWrapper<TBcastCaseGrade>()
                .eq("del_flag", 0)
                .orderByAsc("occ_at", "id"));
    }

    @Override
    public BcastGradeHistoryAudit auditHistory() {
        BcastGradeHistoryAudit audit = new BcastGradeHistoryAudit();
        List<TBcastCaseGrade> rows = this.bcastCaseGradeMapper.selectList(
                new QueryWrapper<TBcastCaseGrade>().eq("del_flag", 0));
        audit.setTotalRows(rows.size());
        int bad = 0;
        for (TBcastCaseGrade g : rows) {
            List<String> notes = new ArrayList<String>();
            // 屏上还挂着线代号的才要对；无可用线那笔四栏留白，无账可对
            checkPinned(g.getOccAt(), "分钟线", g.getmRuleCode(), g.getmEffStart(), notes);
            checkPinned(g.getOccAt(), "户数线", g.gethRuleCode(), g.gethEffStart(), notes);
            if (!notes.isEmpty()) {
                bad++;
                audit.getMismatches().add(new BcastGradeHistoryAudit.Item(g, notes));
            }
        }
        audit.setMismatchRows(bad);
        audit.setBalanced(bad == 0);
        return audit;
    }

    private static final String[] CN = {"一", "二", "三", "四"};

    /**
     * 一头的回话：线在不在、数报没报、落在第几档。档位列只在线与数都齐时给。
     * 返回这一头够不够得着（有线、有数）；不够的缘由进 missingNotes。
     */
    private boolean fillSide(BcastGradeVerdict v, boolean minutesSide, TBcastPreLine line,
                             BigDecimal input, Date at) {
        String sideName = minutesSide ? "停播分钟头" : "被影响户头";
        String kindName = minutesSide ? "分钟线" : "户数线";
        if (line == null) {
            v.getMissingNotes().add(day(at) + "当刻册中找不出在场的" + kindName + "（一条也套不上）");
            return false;
        }
        if (input == null || input.compareTo(BigDecimal.ZERO) < 0) {
            v.getMissingNotes().add(sideName + "的数没报齐，" + kindName + "代号" + line.getRuleCode() + "空比一场");
            return false;
        }
        // 档位仍照线代号走老方法取：进什么出什么，口径与线册同一处
        int level = bcastPreLineService.evaluate(line.getRuleCode(), input, at);
        if (minutesSide) {
            v.setmLevel(Integer.valueOf(level));
        } else {
            v.sethLevel(Integer.valueOf(level));
        }
        return true;
    }

    /** 越头交代：把越了那头的数与线的封顶都摆出来，不只回一句"不够" */
    private String overNote(String sideName, BigDecimal input, TBcastPreLine line) {
        BigDecimal cap = line.getTh3Max();
        return sideName + "这一起" + strip(input) + "，高过" + line.getRuleCode()
                + "封顶" + (cap == null ? "（封顶栏空着）" : strip(cap)) + "，这一头越了";
    }

    /** 历不对：钉着的代号在结论那一日要对得上当刻在场、且立线之日也对得上 */
    private void checkPinned(Date occAt, String kindName, String pinnedCode, Date pinnedStart,
                             List<String> notes) {
        if (pinnedCode == null || pinnedCode.trim().isEmpty()) {
            return;
        }
        TBcastPreLine now = bcastPreLineService.resolveByCodeAt(pinnedCode, occAt);
        if (now == null) {
            notes.add(kindName + " " + pinnedCode + "：" + (occAt == null ? "?" : day(occAt))
                    + " 当日屏上还挂着代号，册中却找不出在场线（已停用或已让位）");
            return;
        }
        if (pinnedStart != null && now.getEffStart() != null
                && !day(pinnedStart).equals(day(now.getEffStart()))) {
            notes.add(kindName + " " + pinnedCode + "：钉的立线之日" + day(pinnedStart)
                    + "与册中在场那条的" + day(now.getEffStart()) + "对不上");
        }
    }

    private String strip(BigDecimal n) {
        return n.stripTrailingZeros().toPlainString();
    }

    private String day(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    private String join(List<String> notes) {
        StringBuilder sb = new StringBuilder();
        for (String n : notes) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(n);
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
