package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TBcastPreLineMapper;
import com.fc.v2.model.auto.TBcastPreLine;
import com.fc.v2.model.custom.BcastPreLineCheckResult;
import com.fc.v2.service.ITBcastPreLineService;

/**
 * 事故定档门槛线 Service业务层处理（rule-book 形状：查册、定位、立线，无删改）。
 *
 * <p>省局改过三轮说法，册子里便躺着三条（或更多）：新添的进场、旧的退居册底。
 * 本类只认一个判在场的口径——立线之日已经够着（含当日，新线自当日进场）、
 * 让位之日还没到（让位之日不含，旧线管到当日之前）、且未停用；让位之日留空
 * 的按一直在场认（旧卷结论要靠它复算，存量这一条守住）。补算往期用往期自己
 * 那一刻查册，与眼下最新落的那条无关。</p>
 *
 * <p>在场名单、逐条定位、条数、单条在不在场，全走 {@link #presentAt} 这一处
 * 点出来，不分两回数；同一件事换两个通道问，回话一致。停用线不进这个名单，
 * 只在 {@link #listBook} 的翻册名单里给人翻。</p>
 *
 * <p>时段比较一律走墙钟字符串（yyyy-MM-dd HH:mm:ss）：JDBC 的 serverTimezone
 * 与本机时区不对称，直接把 java.util.Date 往 SQL 里绑会整体偏移，切换当日
 * 这类边界会随机错档（实测踩到）。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TBcastPreLineServiceImpl implements ITBcastPreLineService {

    private static final Logger log = LoggerFactory.getLogger(TBcastPreLineServiceImpl.class);

    /** 线的情形：在场（旧行这一栏空着的也按在场容下，守住存量） */
    private static final int STATUS_PRESENT = 0;
    /** 线的情形：已停用——在册可翻，不参选、不比数 */
    private static final int STATUS_RETIRED = 1;

    /** 管哪一头：本段停播分钟线（旧卷这一栏空着的按这头认，守住存量） */
    private static final int KIND_MINUTES = 0;
    /** 管哪一头：被影响户数线 */
    private static final int KIND_HOUSEHOLDS = 1;

    /** 优先级降序；并列按 ruleCode 降序（字典序落在末位者胜，确定性） */
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

    private static String ts(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(d);
    }

    @Override
    public TBcastPreLine selectTBcastPreLineById(Long id) {
        if (id == null) {
            return null;
        }
        return this.bcastPreLineMapper.selectById(id);
    }

    @Override
    public List<TBcastPreLine> listBook() {
        // 老册照翻：停用的、退居册底的都在册，只滤删除标记；按立线之日先后摆
        return this.bcastPreLineMapper.selectList(new QueryWrapper<TBcastPreLine>()
                .eq("del_flag", 0)
                .orderByAsc("eff_start", "id"));
    }

    @Override
    public List<TBcastPreLine> listAvailable(Date at) {
        if (at == null) {
            return new ArrayList<TBcastPreLine>();
        }
        return presentAt(at);
    }

    @Override
    public TBcastPreLine resolveLineAt(Date at) {
        if (at == null) {
            return null;
        }
        List<TBcastPreLine> present = presentAt(at);
        // 名单已按顺位降序、代号降序摆好，头一条就是算数的那条；空名单不给结论
        return present.isEmpty() ? null : present.get(0);
    }

    @Override
    public TBcastPreLine resolveByCodeAt(String ruleCode, Date at) {
        if (ruleCode == null || ruleCode.trim().isEmpty() || at == null) {
            return null;
        }
        for (TBcastPreLine r : presentAt(at)) {
            if (ruleCode.equals(r.getRuleCode())) {
                return r;
            }
        }
        return null;
    }

    @Override
    public TBcastPreLine resolveLineByKindAt(Date at, int lineKind) {
        if (at == null) {
            return null;
        }
        // 名单同出处：就在场名单里挑管这一头的，头一条即算数的那条
        for (TBcastPreLine r : presentAt(at)) {
            if (kindOf(r) == lineKind) {
                return r;
            }
        }
        return null;
    }

    /** 旧卷 line_kind 空着的按分钟线认，守住存量 */
    private int kindOf(TBcastPreLine r) {
        return r.getLineKind() == null ? KIND_MINUTES : r.getLineKind().intValue();
    }

    @Override
    public boolean usable(Long id, Date at) {
        if (id == null || at == null) {
            return false;
        }
        for (TBcastPreLine r : presentAt(at)) {
            if (id.equals(r.getId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int countAvailable(Date at) {
        if (at == null) {
            return 0;
        }
        return presentAt(at).size();
    }

    /**
     * 老方法签名一动不动：按 ruleCode 取 at 时刻在场的那一条判档。
     * 判档口径与查册同一处——该代号当刻不在场（已让位/已停用/未到场），返回 0。
     */
    @Override
    public int evaluate(String ruleCode, BigDecimal input, Date at) {
        if (ruleCode == null || ruleCode.trim().isEmpty() || input == null || at == null) {
            return 0;
        }
        if (input.compareTo(BigDecimal.ZERO) < 0) {
            return 0;
        }
        TBcastPreLine rule = resolveByCodeAt(ruleCode, at);
        return levelOf(rule, input);
    }

    /**
     * 多规则求值（老签名）：旧册这一路只摆停播分钟线，故固定取分钟头当刻算数
     * 那条的档位；无在场分钟线返回 0。线分两头后口径不变，守住存量。
     */
    @Override
    public int evaluateTop(BigDecimal input, Date at) {
        if (input == null || at == null || input.compareTo(BigDecimal.ZERO) < 0) {
            return 0;
        }
        TBcastPreLine rule = resolveLineByKindAt(at, KIND_MINUTES);
        return levelOf(rule, input);
    }

    @Override
    public BcastPreLineCheckResult checkAndCreateLine(TBcastPreLine line) {
        BcastPreLineCheckResult receipt = new BcastPreLineCheckResult();
        if (line == null) {
            receipt.getErrors().add("空行立不起来");
            return receipt;
        }
        List<String> errors = receipt.getErrors();

        String name = line.getRuleName() == null ? "" : line.getRuleName().trim();
        String code = line.getRuleCode() == null ? "" : line.getRuleCode().trim();
        if (name.isEmpty()) {
            errors.add("线名一栏空着，立不起来");
        }
        if (code.isEmpty()) {
            errors.add("代号一栏空着，立不起来");
        }
        if (line.getEffStart() == null) {
            errors.add("立线之日一栏空着，立不起来");
        }
        if (line.getPriority() == null) {
            errors.add("让位顺位一栏空着，立不起来");
        }
        // 三个分界数少一个就立不起来：空着的那一栏直接点出来
        if (line.getTh1Max() == null) {
            errors.add("起算一栏空着，立不起来");
        }
        if (line.getTh2Max() == null) {
            errors.add("过半一栏空着，立不起来");
        }
        if (line.getTh3Max() == null) {
            errors.add("封顶一栏空着，立不起来");
        }
        // 没大过前头一栏的那一栏也直接点出来（只在两栏都填着时比，空缺已另点过）
        if (line.getTh1Max() != null && line.getTh2Max() != null
                && line.getTh2Max().compareTo(line.getTh1Max()) <= 0) {
            errors.add("过半（" + line.getTh2Max().stripTrailingZeros().toPlainString()
                    + "）没大过起算（" + line.getTh1Max().stripTrailingZeros().toPlainString()
                    + "），这一栏立不起来");
        }
        if (line.getTh2Max() != null && line.getTh3Max() != null
                && line.getTh3Max().compareTo(line.getTh2Max()) <= 0) {
            errors.add("封顶（" + line.getTh3Max().stripTrailingZeros().toPlainString()
                    + "）没大过过半（" + line.getTh2Max().stripTrailingZeros().toPlainString()
                    + "），这一栏立不起来");
        }
        if (line.getTh1Max() != null && line.getTh1Max().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("起算（" + line.getTh1Max().stripTrailingZeros().toPlainString()
                    + "）为负，这一栏立不起来");
        }
        if (!errors.isEmpty()) {
            // 错处已逐条点在回执上，线不落册
            log.info("立线被挡回，共{}处：{}", Integer.valueOf(errors.size()), errors);
            return receipt;
        }

        line.setRuleName(name);
        line.setRuleCode(code);
        // 管哪一头：递了按递的，没递的新线按分钟线落（旧卷空着的也按分钟线认）
        if (line.getLineKind() == null
                || (line.getLineKind().intValue() != KIND_MINUTES && line.getLineKind().intValue() != KIND_HOUSEHOLDS)) {
            line.setLineKind(Integer.valueOf(KIND_MINUTES));
        }
        // 新线落在场；让位之日留空的按一直在场认（null 原样留着）
        line.setStatus(Integer.valueOf(STATUS_PRESENT));
        line.setDelFlag(Integer.valueOf(0));
        this.bcastPreLineMapper.insert(line);
        receipt.setSuccess(true);
        receipt.setLine(line);
        return receipt;
    }

    // ================= 查册只有这一处出处 =================

    /**
     * at 时刻在场名单：从册里一次取齐（含停用的，一并取回来由这一处筛），逐条
     * 走一遍在场判定，再按让位顺位降序、代号降序摆定。在场名单、定位、条数、
     * 单条在不在场都出自这里，两回数出来不一样就算错。
     */
    private List<TBcastPreLine> presentAt(Date at) {
        List<TBcastPreLine> all = this.bcastPreLineMapper.selectList(
                new QueryWrapper<TBcastPreLine>().eq("del_flag", 0));
        List<TBcastPreLine> present = new ArrayList<TBcastPreLine>();
        String sAt = ts(at);
        // 现行判定才剔停用线；补算往期（at 的墙钟秒早于此刻）只认生效窗口，旧卷结论才复算得出来。
        // 按墙钟秒比：屏上拿"当下"来问时，传入那一刻与本处取的当下相差毫秒，仍算同一刻（现行）。
        boolean excludeRetired = sAt.compareTo(ts(new Date())) >= 0;
        for (TBcastPreLine r : all) {
            if (isPresent(r, sAt, excludeRetired)) {
                present.add(r);
            }
        }
        Collections.sort(present, PRIORITY_THEN_CODE_DESC);
        return present;
    }

    /**
     * 在场判定：立线之日已够着（含当日，新线自当日进场）、让位之日还没到
     * （不含当日，旧线管到当日之前）；让位之日留白的旧线按一直在场认。
     * excludeRetired 为真时，已停用的不进现行判定（翻册照翻，只是不参选）。
     */
    private boolean isPresent(TBcastPreLine r, String sAt, boolean excludeRetired) {
        if (excludeRetired && r.getStatus() != null && r.getStatus().intValue() == STATUS_RETIRED) {
            return false;
        }
        if (r.getEffStart() == null) {
            return false;
        }
        if (sAt.compareTo(ts(r.getEffStart())) < 0) {
            return false;
        }
        if (r.getEffEnd() != null && sAt.compareTo(ts(r.getEffEnd())) >= 0) {
            // 旧线管到让位之日之前，交接当日只认新线那一套说法
            return false;
        }
        return true;
    }

    /**
     * 分档：等于上限取高一档。旧线三档没维护全的，只按维护着的档比，
     * 高过最大那一档落第 4 档。
     */
    private int levelOf(TBcastPreLine rule, BigDecimal input) {
        if (rule == null) {
            // 一条也套不上：不给结论（0 即无可用档），不借旁边一条线的数字糊答复
            return 0;
        }
        if (rule.getTh1Max() != null && input.compareTo(rule.getTh1Max()) <= 0) {
            return 1;
        }
        if (rule.getTh2Max() != null && input.compareTo(rule.getTh2Max()) <= 0) {
            return 2;
        }
        if (rule.getTh3Max() != null && input.compareTo(rule.getTh3Max()) <= 0) {
            return 3;
        }
        return 4;
    }
}
