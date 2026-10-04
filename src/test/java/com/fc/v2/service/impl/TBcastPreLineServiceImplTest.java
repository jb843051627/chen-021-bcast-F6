package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.fc.v2.mapper.auto.TBcastPreLineMapper;
import com.fc.v2.model.auto.TBcastPreLine;
import com.fc.v2.model.custom.BcastPreLineCheckResult;

/**
 * 门槛线册子的单测：在场、交接当日、顺位/代号平局、停用不参选、立线回执、
 * 名单与逐条走查同一出处。库用 mock 顶替，册子摆在内存里。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TBcastPreLineServiceImplTest {

    @Mock
    private TBcastPreLineMapper preLineMapper;

    @org.mockito.InjectMocks
    private TBcastPreLineServiceImpl service;

    private final List<TBcastPreLine> book = new ArrayList<TBcastPreLine>();

    private static Date t(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String n(String v) {
        return v == null ? null : new BigDecimal(v).stripTrailingZeros().toPlainString();
    }

    private TBcastPreLine line(long id, String code, Integer kind, String th1, String th2, String th3,
                               String start, String end, Integer priority, Integer status) {
        TBcastPreLine r = new TBcastPreLine();
        r.setId(Long.valueOf(id));
        r.setRuleCode(code);
        r.setRuleName("线名" + code);
        r.setLineKind(kind);
        r.setTh1Max(th1 == null ? null : new BigDecimal(th1));
        r.setTh2Max(th2 == null ? null : new BigDecimal(th2));
        r.setTh3Max(th3 == null ? null : new BigDecimal(th3));
        r.setEffStart(t(start));
        r.setEffEnd(end == null ? null : t(end));
        r.setPriority(priority);
        r.setStatus(status);
        r.setDelFlag(Integer.valueOf(0));
        return r;
    }

    /** 省局三轮说法：M-2018→M-2022 交接于 2022-03-01，M-2022→M-2025 交接于 2025-06-01 */
    @BeforeEach
    void mockBook() {
        book.clear();
        book.add(line(100, "M-2018", 0, "5", "30", "120",
                "2018-01-01 00:00:00", "2022-03-01 00:00:00", 10, 1));
        book.add(line(101, "M-2022", 0, "10", "60", "180",
                "2022-03-01 00:00:00", "2025-06-01 00:00:00", 20, 1));
        book.add(line(102, "M-2025", 0, "15", "90", "240",
                "2025-06-01 00:00:00", null, 30, 0));
        book.add(line(110, "H-2020", 1, "1000", "10000", "50000",
                "2020-01-01 00:00:00", "2024-01-01 00:00:00", 10, 1));
        book.add(line(111, "H-2024", 1, "500", "5000", "30000",
                "2024-01-01 00:00:00", null, 20, 0));
        when(preLineMapper.selectList(any())).thenReturn(book);
    }

    // ================= 交接当日：旧线管到之前，新线当日进场 =================

    @Test
    void 交接当日只认新线_旧线前一刻还作数() {
        // 交接日 2022-03-01 00:00:00：新线当日进场，旧线让位之日不含
        TBcastPreLine onDay = service.resolveLineByKindAt(t("2022-03-01 00:00:00"), 0);
        assertEquals("M-2022", onDay.getRuleCode());
        // 前一秒仍是旧线
        assertEquals("M-2018", service.resolveLineByKindAt(t("2022-02-28 23:59:59"), 0).getRuleCode());
        // 后一轮交接当日同理
        assertEquals("M-2025", service.resolveLineByKindAt(t("2025-06-01 00:00:00"), 0).getRuleCode());
        assertEquals("M-2022", service.resolveLineByKindAt(t("2025-05-31 23:59:59"), 0).getRuleCode());
    }

    @Test
    void 换两个通道问同一件事回话一致_名单定位条数单条同源() {
        Date at = t("2023-05-01 08:00:00");
        List<TBcastPreLine> present = service.listAvailable(at);
        // 逐条走一遍定位，与名单头一条必须是同一条
        assertSame(present.get(0), service.resolveLineAt(at));
        // 条数与名单同一回点出来
        assertEquals(present.size(), service.countAvailable(at));
        // 单条 usable 也认这同一份
        assertTrue(service.usable(present.get(0).getId(), at));
        assertFalse(service.usable(Long.valueOf(100L), at));
        // 按代号问：在场的问得着，已让位的问不着
        assertEquals("M-2022", service.resolveByCodeAt("M-2022", at).getRuleCode());
        assertNull(service.resolveByCodeAt("M-2018", at));
    }

    // ================= 停用线：翻得到、不参选；往期复算仍认窗口 =================

    @Test
    void 停用线在册可翻_屏上名单也露_只是现行判定选不到它() {
        // 翻册：停用的也在册
        List<TBcastPreLine> all = service.listBook();
        assertTrue(all.containsAll(book));
        // 现行判定（2099 远在当下之后）：M-2022 虽窗口早已让位，且停用；现行只认在场的 M-2025
        Date now = t("2099-01-01 00:00:00");
        List<TBcastPreLine> present = service.listAvailable(now);
        for (TBcastPreLine r : present) {
            assertFalse("M-2018".equals(r.getRuleCode()));
            assertFalse("M-2022".equals(r.getRuleCode()));
        }
        assertEquals("M-2025", service.resolveLineAt(now).getRuleCode());
        assertFalse(service.usable(Long.valueOf(101L), now));
    }

    @Test
    void 补算往期用往期那一刻_已停用的旧线在它当值那日仍在场() {
        // 2023 年复算：M-2022 眼下已停用，但在它自己当值的日子仍算数（只认生效窗口）
        Date past = t("2023-05-01 08:00:00");
        assertEquals("M-2022", service.resolveLineByKindAt(past, 0).getRuleCode());
        // 户头 2021 年复算认 H-2020
        assertEquals("H-2020", service.resolveLineByKindAt(t("2021-06-01 00:00:00"), 1).getRuleCode());
        // 立线之前一条也套不上
        assertNull(service.resolveLineByKindAt(t("2017-12-31 23:59:59"), 0));
    }

    @Test
    void 让位之日留白的按一直在场认() {
        // M-2025 / H-2024 让位之日留白，远期仍在场（旧卷复算的存量口径）
        assertEquals("M-2025", service.resolveLineByKindAt(t("2099-01-01 00:00:00"), 0).getRuleCode());
        assertEquals("H-2024", service.resolveLineByKindAt(t("2099-01-01 00:00:00"), 1).getRuleCode());
    }

    @Test
    void 现行判定窗口重叠时停用线也不参选_往期它当值那日仍可复算() {
        book.clear();
        // 一条已停用、让位之日却留白（窗口一直开着）、顺位还高的线
        book.add(line(300, "M-OLD-STILL", 0, "1", "2", "3",
                "2020-01-01 00:00:00", null, 99, 1));
        book.add(line(301, "M-NOW", 0, "10", "20", "30",
                "2020-01-01 00:00:00", null, 1, 0));
        // 现行判定：就拿"当下"这一刻来问（传入时刻与服务内取的当下相差毫秒，仍算同一刻）
        Date now = new Date();
        // 现行判定不许选停用线出来比数，哪怕它顺位高、窗口还开着
        assertEquals("M-NOW", service.resolveLineByKindAt(now, 0).getRuleCode());
        for (TBcastPreLine r : service.listAvailable(now)) {
            assertFalse("M-OLD-STILL".equals(r.getRuleCode()));
        }
        assertFalse(service.usable(Long.valueOf(300L), now));
        // 翻册仍翻得着
        assertTrue(service.listBook().stream().anyMatch(r -> "M-OLD-STILL".equals(r.getRuleCode())));
        // 补算往期：它当值那日仍算数（旧卷结论靠它复算）
        assertEquals("M-OLD-STILL", service.resolveLineByKindAt(t("2021-01-01 00:00:00"), 0).getRuleCode());
    }

    // ================= 顺位、代号平局 =================

    @Test
    void 同刻在场先比顺位_顺位相同代号落在末位者胜() {
        book.clear();
        book.add(line(200, "TIE-LOW", 0, "1", "2", "3",
                "2024-01-01 00:00:00", null, 5, 0));
        book.add(line(201, "TIE-HI", 0, "1", "2", "3",
                "2024-01-01 00:00:00", null, 9, 0));
        // 顺位 9 压过 5
        assertEquals("TIE-HI", service.resolveLineAt(t("2024-06-01 00:00:00")).getRuleCode());

        book.get(0).setPriority(Integer.valueOf(9));
        // 顺位相同：代号字典序末位（TIE-LOW）胜出
        assertEquals("TIE-LOW", service.resolveLineAt(t("2024-06-01 00:00:00")).getRuleCode());
        // 一件事只有一个答案：名单头一条与定位同一条
        assertSame(service.listAvailable(t("2024-06-01 00:00:00")).get(0),
                service.resolveLineAt(t("2024-06-01 00:00:00")));
    }

    // ================= 老 evaluate 签名不动、分档口径 =================

    @Test
    void 老签名evaluate按代号取当刻在场线_等于上限取高一档() {
        Date at = t("2023-05-01 00:00:00"); // M-2022 当值：10/60/180
        assertEquals(1, service.evaluate("M-2022", new BigDecimal("10"), at));
        assertEquals(1, service.evaluate("M-2022", new BigDecimal("10.00"), at));
        assertEquals(2, service.evaluate("M-2022", new BigDecimal("10.01"), at));
        assertEquals(2, service.evaluate("M-2022", new BigDecimal("60"), at));
        assertEquals(3, service.evaluate("M-2022", new BigDecimal("180"), at));
        assertEquals(4, service.evaluate("M-2022", new BigDecimal("181"), at));
        // 已让位/未到场/没这代号：0，不借旁边一条线糊答复
        assertEquals(0, service.evaluate("M-2018", new BigDecimal("20"), at));
        assertEquals(0, service.evaluate("M-2025", new BigDecimal("20"), at));
        assertEquals(0, service.evaluate("NOT-EXIST", new BigDecimal("20"), at));
        assertEquals(0, service.evaluate("M-2022", new BigDecimal("-1"), at));
        assertEquals(0, service.evaluate("M-2022", null, at));
        // evaluateTop 当刻无线 → 0
        assertEquals(0, service.evaluateTop(new BigDecimal("20"), t("2017-01-01 00:00:00")));
        // 当刻取顺位最高那条判档：2023 年分钟线是 M-2022
        assertEquals(2, service.evaluateTop(new BigDecimal("60"), at));
    }

    // ================= 立线回执 =================

    @Test
    void 三档齐且逐档增大才立得起来_落场落默认头() {
        TBcastPreLine r = line(0, "M-2099", null, "1", "2", "3",
                "2099-01-01 00:00:00", null, 1, null);
        r.setId(null);
        BcastPreLineCheckResult receipt = service.checkAndCreateLine(r);
        assertTrue(receipt.isSuccess());
        ArgumentCaptor<TBcastPreLine> cap = ArgumentCaptor.forClass(TBcastPreLine.class);
        verify(preLineMapper).insert(cap.capture());
        assertEquals(Integer.valueOf(0), cap.getValue().getStatus());
        assertEquals(Integer.valueOf(0), cap.getValue().getDelFlag());
        // 没递管哪一头，按分钟线落
        assertEquals(Integer.valueOf(0), cap.getValue().getLineKind());
    }

    @Test
    void 空着的栏一栏一条点出来() {
        TBcastPreLine r = new TBcastPreLine();
        r.setRuleName("  ");
        r.setRuleCode(" ");
        // 立线之日、让位顺位、三个分界数全空
        BcastPreLineCheckResult receipt = service.checkAndCreateLine(r);
        assertFalse(receipt.isSuccess());
        List<String> errs = receipt.getErrors();
        assertTrue(join(errs).contains("线名"));
        assertTrue(join(errs).contains("代号"));
        assertTrue(join(errs).contains("立线之日"));
        assertTrue(join(errs).contains("让位顺位"));
        assertTrue(join(errs).contains("起算"));
        assertTrue(join(errs).contains("过半"));
        assertTrue(join(errs).contains("封顶"));
        verify(preLineMapper, never()).insert(any());
    }

    @Test
    void 没大过前头那一栏的直接点出是哪一栏() {
        TBcastPreLine r = line(0, "M-BAD", 0, "10", "10", "5",
                "2099-01-01 00:00:00", null, 1, null);
        r.setId(null);
        BcastPreLineCheckResult receipt = service.checkAndCreateLine(r);
        assertFalse(receipt.isSuccess());
        String all = join(receipt.getErrors());
        // 过半没大过起算、封顶没大过过半，各点各的栏，不回一句"数不对"
        assertTrue(all.contains("过半"));
        assertTrue(all.contains("封顶"));
        assertTrue(all.contains(n("10")));
        verify(preLineMapper, never()).insert(any());
    }

    @Test
    void 空册子与界点为空不办事() {
        assertNull(service.resolveLineAt(null));
        assertEquals(0, service.countAvailable(null));
        assertFalse(service.usable(Long.valueOf(1L), null));
        assertTrue(service.listAvailable(null).isEmpty());
        BcastPreLineCheckResult receipt = service.checkAndCreateLine(null);
        assertFalse(receipt.isSuccess());
    }

    private static String join(List<String> errs) {
        StringBuilder sb = new StringBuilder();
        for (String e : errs) {
            sb.append(e).append("；");
        }
        return sb.toString();
    }
}
