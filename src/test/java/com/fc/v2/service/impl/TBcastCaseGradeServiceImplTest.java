package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;

import com.fc.v2.mapper.auto.TBcastCaseGradeMapper;
import com.fc.v2.model.auto.TBcastCaseGrade;
import com.fc.v2.model.auto.TBcastPreLine;
import com.fc.v2.model.custom.BcastGradeHistoryAudit;
import com.fc.v2.model.custom.BcastGradeVerdict;
import com.fc.v2.service.ITBcastPreLineService;

/**
 * 定档唯一出口的单测：两头线各自比、越头摆数、无可用线四栏留白、
 * 线代号与立线之日随结论钉死只插不改、历不对一笔一条摆出。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TBcastCaseGradeServiceImplTest {

    @Mock
    private TBcastCaseGradeMapper gradeMapper;

    @Mock
    private ITBcastPreLineService preLineService;

    @org.mockito.InjectMocks
    private TBcastCaseGradeServiceImpl service;

    private final List<TBcastCaseGrade> ledger = new ArrayList<TBcastCaseGrade>();

    private static Date t(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private TBcastPreLine line(String code, int kind, String th1, String th2, String cap, String start) {
        TBcastPreLine r = new TBcastPreLine();
        r.setId(Long.valueOf(code.hashCode() & 0x7fffffff));
        r.setRuleCode(code);
        r.setLineKind(Integer.valueOf(kind));
        r.setTh1Max(th1 == null ? null : new BigDecimal(th1));
        r.setTh2Max(th2 == null ? null : new BigDecimal(th2));
        r.setTh3Max(cap == null ? null : new BigDecimal(cap));
        r.setEffStart(t(start));
        return r;
    }

    @BeforeEach
    void mockLedger() {
        ledger.clear();
        when(gradeMapper.insert(any())).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock inv) {
                ledger.add(inv.getArgument(0));
                return Integer.valueOf(1);
            }
        });
        when(gradeMapper.selectList(any())).thenReturn(ledger);
    }

    /** 分钟线 M-2025（10/60/180）、户数线 H-2024（500/5000/30000）当刻都在场布桩 */
    private TBcastPreLine mockLines(Date at, String mCode, String hCode, String mCap, String hCap) {
        TBcastPreLine m = line(mCode, 0, "10", "60", mCap, "2025-06-01 00:00:00");
        TBcastPreLine h = line(hCode, 1, "500", "5000", hCap, "2024-01-01 00:00:00");
        when(preLineService.resolveLineByKindAt(at, ITBcastPreLineService.KIND_MINUTES)).thenReturn(m);
        when(preLineService.resolveLineByKindAt(at, ITBcastPreLineService.KIND_HOUSEHOLDS)).thenReturn(h);
        // 档位仍走老 evaluate 签名取
        Answer<Integer> lvl = new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock inv) {
                String code = inv.getArgument(0);
                BigDecimal input = inv.getArgument(1);
                TBcastPreLine src = code.equals(mCode) ? m : h;
                if (input.compareTo(src.getTh1Max()) <= 0) {
                    return Integer.valueOf(1);
                }
                if (input.compareTo(src.getTh2Max()) <= 0) {
                    return Integer.valueOf(2);
                }
                if (src.getTh3Max() != null && input.compareTo(src.getTh3Max()) <= 0) {
                    return Integer.valueOf(3);
                }
                return Integer.valueOf(4);
            }
        };
        when(preLineService.evaluate(any(), any(), eq(at))).thenAnswer(lvl);
        return m;
    }

    // ================= 两头不越才归层、取高头 =================

    @Test
    void 两头各自不越才归层_合到一处取落得高的那一层_代号立线之日钉死() {
        Date at = t("2026-05-01 10:00:00");
        mockLines(at, "M-2025", "H-2024", "180", "30000");

        // 分钟 20→2 档，户数 2000→2 档
        BcastGradeVerdict v = service.gradeAndPersist("SG-1", at, new BigDecimal("20"), new BigDecimal("2000"));
        assertTrue(v.isAvailable());
        assertEquals(Integer.valueOf(2), v.getGradeLevel());
        assertEquals("第二层", v.getGradeWord());

        // 分钟第3档、户数第1档 → 取高头第3层
        v = service.gradeAndPersist("SG-1", at, new BigDecimal("100"), new BigDecimal("5"));
        assertEquals(Integer.valueOf(3), v.getGradeLevel());

        // 落库那笔钉的就是这一趟在场的两条线
        TBcastCaseGrade rec = v.getSaved();
        assertEquals("M-2025", rec.getmRuleCode());
        assertEquals(t("2025-06-01 00:00:00"), rec.getmEffStart());
        assertEquals("H-2024", rec.gethRuleCode());
        assertEquals(t("2024-01-01 00:00:00"), rec.gethEffStart());
    }

    // ================= 越头：摆那头的数，不只回不够 =================

    @Test
    void 分钟头越封顶_摆出那头的数与封顶_不归层_两头代号仍钉死() {
        Date at = t("2026-05-01 10:00:00");
        mockLines(at, "M-2025", "H-2024", "180", "30000");

        BcastGradeVerdict v = service.gradeAndPersist("SG-2", at, new BigDecimal("200"), new BigDecimal("2000"));
        assertFalse(v.isAvailable());
        assertEquals(BcastGradeVerdict.OVER_MINUTES, v.getOverSide());
        assertEquals(BcastGradeVerdict.WORD_OVER, v.getGradeWord());
        assertNull(v.getGradeLevel());
        String note = v.getOverNotes().get(0);
        assertTrue(note.contains("200"));
        assertTrue(note.contains("180"));
        // 越头不归层，这一趟认的线照钉
        assertEquals("M-2025", v.getSaved().getmRuleCode());
        assertEquals("H-2024", v.getSaved().gethRuleCode());
    }

    @Test
    void 户头越了摆户头_两头都越位标记为三() {
        Date at = t("2026-05-01 10:00:00");
        mockLines(at, "M-2025", "H-2024", "180", "30000");

        BcastGradeVerdict v = service.gradeAndPersist("SG-3", at, new BigDecimal("20"), new BigDecimal("40000"));
        assertEquals(BcastGradeVerdict.OVER_HOUSEHOLDS, v.getOverSide());
        assertTrue(v.getOverNotes().get(0).contains("40000"));
        assertTrue(v.getOverNotes().get(0).contains("30000"));

        BcastGradeVerdict both = service.gradeAndPersist("SG-3", at, new BigDecimal("200"), new BigDecimal("40000"));
        assertEquals(3, both.getOverSide());
        assertEquals(2, both.getOverNotes().size());
    }

    // ================= 无可用线：不给结论，四栏留白 =================

    @Test
    void 一头一条也套不上_结论写无可用线_代号立线之日四栏一律留白() {
        Date at = t("2026-05-01 10:00:00");
        when(preLineService.resolveLineByKindAt(at, ITBcastPreLineService.KIND_MINUTES)).thenReturn(null);
        when(preLineService.resolveLineByKindAt(at, ITBcastPreLineService.KIND_HOUSEHOLDS))
                .thenReturn(line("H-2024", 1, "500", "5000", "30000", "2024-01-01 00:00:00"));

        BcastGradeVerdict v = service.gradeAndPersist("SG-4", at, new BigDecimal("20"), new BigDecimal("2000"));
        assertFalse(v.isAvailable());
        assertEquals(BcastGradeVerdict.WORD_NO_LINE, v.getGradeWord());
        assertNull(v.getGradeLevel());
        // 不许借旁边一条线（户数线）的数字糊一个看着合理的答复：四栏留白
        assertNull(v.getSaved().getmRuleCode());
        assertNull(v.getSaved().getmEffStart());
        assertNull(v.getSaved().gethRuleCode());
        assertNull(v.getSaved().gethEffStart());
        assertFalse(v.getMissingNotes().isEmpty());
        // 但这一笔照落库（没够上也是一笔）
        assertEquals(1, ledger.size());
    }

    @Test
    void 数没报齐也按无可用线留白() {
        Date at = t("2026-05-01 10:00:00");
        mockLines(at, "M-2025", "H-2024", "180", "30000");
        BcastGradeVerdict v = service.gradeAndPersist("SG-5", at, null, new BigDecimal("2000"));
        assertEquals(BcastGradeVerdict.WORD_NO_LINE, v.getGradeWord());
        assertNull(v.getSaved().getmRuleCode());
        assertTrue(v.getMissingNotes().get(0).contains("停播分钟头"));
    }

    // ================= 只插不改：没够上与后来补上各一笔 =================

    @Test
    void 每问一趟各落一笔_只插不改_没够上与后来补上是两笔() {
        Date at = t("2026-05-01 10:00:00");
        mockLines(at, "M-2025", "H-2024", "180", "30000");

        // 头一趟越头没够上
        service.gradeAndPersist("SG-6", at, new BigDecimal("200"), new BigDecimal("2000"));
        // 后一趟补上了——另一笔，不回头改前头那笔
        BcastGradeVerdict again = service.gradeAndPersist("SG-6", at, new BigDecimal("20"), new BigDecimal("2000"));
        assertEquals("第二层", again.getGradeWord());

        assertEquals(2, ledger.size());
        assertEquals(BcastGradeVerdict.WORD_OVER, ledger.get(0).getGradeWord());
        assertEquals("第二层", ledger.get(1).getGradeWord());
        verify(gradeMapper, never()).updateById(any());
        verify(gradeMapper, never()).update(any(), any());
        verify(gradeMapper, never()).deleteById(any());
    }

    @Test
    void 缺事故代号或缺回看时刻_整趟不立也不落笔() {
        assertNull(service.gradeAndPersist(null, t("2026-05-01 10:00:00"), new BigDecimal("1"), new BigDecimal("1")));
        assertNull(service.gradeAndPersist("  ", t("2026-05-01 10:00:00"), new BigDecimal("1"), new BigDecimal("1")));
        assertNull(service.gradeAndPersist("SG-7", null, new BigDecimal("1"), new BigDecimal("1")));
        assertEquals(0, ledger.size());
    }

    // ================= 历不对核账 =================

    @Test
    void 历不对_屏上挂代号册子那日找不着在场线_一笔一条摆出_旧笔不改() {
        Date d1 = t("2025-03-01 10:20:00");
        Date d2 = t("2025-09-10 08:05:00");
        TBcastCaseGrade g1 = gradeRow(900, "SG-2025-018", d1, "M-2018", t("2018-01-01 00:00:00"),
                "H-2024", t("2024-01-01 00:00:00"));
        TBcastCaseGrade g2 = gradeRow(901, "SG-2025-041", d2, "M-2025", t("2025-06-01 00:00:00"),
                "H-2020", t("2020-01-01 00:00:00"));
        TBcastCaseGrade g3 = gradeRow(902, "SG-2026-002", t("2026-02-01 09:00:00"),
                "M-2025", t("2025-06-01 00:00:00"), "H-2024", t("2024-01-01 00:00:00"));
        ledger.addAll(Arrays.asList(g1, g2, g3));

        // 复算口径：g1 分钟头 M-2018 在 2025-03-01 找不着在场线（早已让位）
        when(preLineService.resolveByCodeAt("M-2018", d1)).thenReturn(null);
        when(preLineService.resolveByCodeAt("H-2024", d1)).thenReturn(line("H-2024", 1, "500", "5000", "30000", "2024-01-01 00:00:00"));
        // g2 户头 H-2020 在 2025-09-10 找不着
        when(preLineService.resolveByCodeAt("M-2025", d2)).thenReturn(line("M-2025", 0, "15", "90", "240", "2025-06-01 00:00:00"));
        when(preLineService.resolveByCodeAt("H-2020", d2)).thenReturn(null);
        // g3 两头都对得上
        Date d3 = t("2026-02-01 09:00:00");
        when(preLineService.resolveByCodeAt("M-2025", d3)).thenReturn(line("M-2025", 0, "15", "90", "240", "2025-06-01 00:00:00"));
        when(preLineService.resolveByCodeAt("H-2024", d3)).thenReturn(line("H-2024", 1, "500", "5000", "30000", "2024-01-01 00:00:00"));

        BcastGradeHistoryAudit audit = service.auditHistory();
        assertEquals(3, audit.getTotalRows());
        assertFalse(audit.isBalanced());
        assertEquals(2, audit.getMismatchRows());
        List<String> codes = new ArrayList<String>();
        for (BcastGradeHistoryAudit.Item item : audit.getMismatches()) {
            codes.add(item.getGrade().getBizNo());
            assertFalse(item.getNotes().isEmpty());
        }
        assertTrue(codes.contains("SG-2025-018"));
        assertTrue(codes.contains("SG-2025-041"));
        // 只摆不改
        assertEquals("M-2018", g1.getmRuleCode());
        verify(gradeMapper, never()).updateById(any());
    }

    @Test
    void 钉的立线之日与册中在场那条对不上也算历不对() {
        Date d = t("2026-03-01 09:00:00");
        TBcastPreLine current = line("M-2025", 0, "15", "90", "240", "2025-06-01 00:00:00");
        ledger.add(gradeRow(910, "SG-X", d, "M-2025", t("2099-01-01 00:00:00"),
                "H-2024", t("2024-01-01 00:00:00")));
        when(preLineService.resolveByCodeAt("M-2025", d)).thenReturn(current);
        when(preLineService.resolveByCodeAt("H-2024", d)).thenReturn(line("H-2024", 1, "500", "5000", "30000", "2024-01-01 00:00:00"));

        BcastGradeHistoryAudit audit = service.auditHistory();
        assertEquals(1, audit.getMismatchRows());
        assertTrue(audit.getMismatches().get(0).getNotes().get(0).contains("立线之日"));
    }

    @Test
    void 台账一笔不差_无可用线留白的也在账上() {
        Date at = t("2026-05-01 10:00:00");
        when(preLineService.resolveLineByKindAt(at, ITBcastPreLineService.KIND_MINUTES)).thenReturn(null);
        when(preLineService.resolveLineByKindAt(at, ITBcastPreLineService.KIND_HOUSEHOLDS)).thenReturn(null);
        service.gradeAndPersist("SG-8", at, new BigDecimal("20"), new BigDecimal("2000"));

        List<TBcastCaseGrade> rows = service.listGrades();
        assertEquals(1, rows.size());
        assertEquals(BcastGradeVerdict.WORD_NO_LINE, rows.get(0).getGradeWord());
        assertNotNull(rows.get(0).getOccAt());
    }

    private TBcastCaseGrade gradeRow(long id, String bizNo, Date occAt, String mCode, Date mStart,
                                     String hCode, Date hStart) {
        TBcastCaseGrade g = new TBcastCaseGrade();
        g.setId(Long.valueOf(id));
        g.setBizNo(bizNo);
        g.setOccAt(occAt);
        g.setmRuleCode(mCode);
        g.setmEffStart(mStart);
        g.sethRuleCode(hCode);
        g.sethEffStart(hStart);
        g.setDelFlag(Integer.valueOf(0));
        return g;
    }
}
