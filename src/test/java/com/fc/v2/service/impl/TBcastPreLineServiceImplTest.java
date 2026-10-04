package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TBcastCaseGradeMapper;
import com.fc.v2.mapper.auto.TBcastPreLineMapper;
import com.fc.v2.model.auto.TBcastCaseGrade;
import com.fc.v2.model.auto.TBcastPreLine;
import com.fc.v2.model.custom.BcastGradeAnomaly;
import com.fc.v2.model.custom.BcastGradeVerdict;
import com.fc.v2.model.custom.BcastLineCheck;
import com.fc.v2.model.custom.BcastLinePick;

/**
 * 事故定档门槛线：在场口径、立线回执、两头归层、历不对清点的单测。
 * 库用两本账顶替（线册账、结论账），落库与清点都当着面在账上过。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TBcastPreLineServiceImplTest {

    @Mock
    private TBcastPreLineMapper preLineMapper;

    @Mock
    private TBcastCaseGradeMapper caseGradeMapper;

    @org.mockito.InjectMocks
    private TBcastPreLineServiceImpl service;

    /** 线册账：停用的也在账上躺着 */
    private final Map<Long, TBcastPreLine> lineDb = new LinkedHashMap<Long, TBcastPreLine>();
    /** 结论账：每一趟一笔 */
    private final List<TBcastCaseGrade> gradeDb = new ArrayList<TBcastCaseGrade>();
    private long gradeIdSeq = 900L;

    private static Date d(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static BigDecimal n(String s) {
        return new BigDecimal(s);
    }

    /** 立一条线 */
    private TBcastPreLine line(long id, String code, String name, int kind,
                               String th1, String th2, String th3,
                               String start, String end, Integer priority, int status) {
        TBcastPreLine r = new TBcastPreLine();
        r.setId(Long.valueOf(id));
        r.setRuleCode(code);
        r.setRuleName(name);
        r.setLineKind(Integer.valueOf(kind));
        r.setTh1Max(th1 == null ? null : n(th1));
        r.setTh2Max(th2 == null ? null : n(th2));
        r.setTh3Max(th3 == null ? null : n(th3));
        r.setEffStart(start == null ? null : d(start));
        r.setEffEnd(end == null ? null : d(end));
        r.setPriority(priority);
        r.setStatus(Integer.valueOf(status));
        r.setDelFlag(Integer.valueOf(0));
        return r;
    }

    /** 与 schema seed 同一本线册：分钟三轮 + 户数两轮（老说法停用、新说法去年九月才立） */
    @BeforeEach
    void mockDb() {
        lineDb.clear();
        gradeDb.clear();
        gradeIdSeq = 900L;
        lineDb.put(101L, line(101, "PL-F01", "二〇二三老说法（分钟）", 1,
                "10", "30", "60", "2023-01-01 00:00:00", "2024-01-01 00:00:00", 10, 0));
        lineDb.put(102L, line(102, "PL-F02", "二〇二四说法（分钟）", 1,
                "5", "20", "45", "2024-01-01 00:00:00", "2025-06-01 00:00:00", 20, 0));
        lineDb.put(103L, line(103, "PL-F03", "现行说法（分钟）", 1,
                "3", "15", "30", "2025-06-01 00:00:00", null, 30, 0));
        lineDb.put(201L, line(201, "PL-H01", "户数老说法（已整条停用）", 2,
                "500", "2000", "5000", "2024-01-01 00:00:00", null, 10, 1));
        lineDb.put(202L, line(202, "PL-H02", "户数现行说法", 2,
                "300", "1000", "3000", "2025-09-01 00:00:00", null, 20, 0));

        when(preLineMapper.selectById(any())).thenAnswer(inv -> lineDb.get((Long) inv.getArgument(0)));
        when(preLineMapper.selectList(any())).thenAnswer(inv -> {
            // 账上只把未删的线给出去，其余筛子（停用/时效/顺位）由服务层同一个口径过
            List<TBcastPreLine> all = new ArrayList<TBcastPreLine>();
            for (TBcastPreLine r : lineDb.values()) {
                if (r.getDelFlag() == null || r.getDelFlag().intValue() == 0) {
                    all.add(r);
                }
            }
            return all;
        });
        when(caseGradeMapper.selectList(any())).thenAnswer(inv -> {
            // 服务层只查 del_flag=0 且 settled=1 的落定结论，账上照这个形状给
            List<TBcastCaseGrade> settled = new ArrayList<TBcastCaseGrade>();
            for (TBcastCaseGrade g : gradeDb) {
                if ((g.getDelFlag() == null || g.getDelFlag().intValue() == 0)
                        && g.getSettled() != null && g.getSettled().intValue() == 1) {
                    settled.add(g);
                }
            }
            return settled;
        });
        when(caseGradeMapper.insert(any())).thenAnswer(inv -> {
            TBcastCaseGrade g = inv.getArgument(0);
            g.setId(Long.valueOf(++gradeIdSeq));
            gradeDb.add(g);
            return Integer.valueOf(1);
        });
    }

    /** 屏上名单里某一头排首位的代号 */
    private String topOnScreen(String at, int kind) {
        for (TBcastPreLine r : service.listOnScreen(d(at))) {
            if (r.getLineKind() != null && r.getLineKind().intValue() == kind) {
                return r.getRuleCode();
            }
        }
        return null;
    }

    // ================= 立线回执：缺栏与乱序逐栏点出 =================

    @Test
    void 立线_空着的栏逐栏点出() {
        BcastLineCheck c1 = service.checkLine(line(1, "PL-X", "缺起算", 1,
                null, "20", "30", "2026-01-01 00:00:00", null, 1, 0));
        assertFalse(c1.isValid());
        assertEquals(Arrays.asList("起算"), c1.getBlankFields());
        assertTrue(c1.getOrderFaults().isEmpty());

        BcastLineCheck c3 = service.checkLine(line(2, "PL-X", "三个数全空", 1,
                null, null, null, "2026-01-01 00:00:00", null, 1, 0));
        assertFalse(c3.isValid());
        assertEquals(Arrays.asList("起算", "过半", "封顶"), c3.getBlankFields());
    }

    @Test
    void 立线_没大过前头那一栏_两栏的数直接摆出来() {
        BcastLineCheck c = service.checkLine(line(3, "PL-X", "过半封顶都没大过前头", 1,
                "10", "10", "5", "2026-01-01 00:00:00", null, 1, 0));
        assertFalse(c.isValid());
        assertTrue(c.getBlankFields().isEmpty());
        assertEquals(2, c.getOrderFaults().size());
        // 光回一句"数不对"不算交代：哪两栏、各是多少都点出来
        assertTrue(c.getOrderFaults().get(0).contains("过半"));
        assertTrue(c.getOrderFaults().get(0).contains("10"));
        assertTrue(c.getOrderFaults().get(1).contains("封顶"));
        assertTrue(c.getOrderFaults().get(1).contains("5"));

        BcastLineCheck ok = service.checkLine(line(4, "PL-Y", "立得住", 1,
                "5", "20", "45", "2026-01-01 00:00:00", null, 1, 0));
        assertTrue(ok.isValid());
    }

    // ================= 在场口径：立线够着、让位没到 =================

    @Test
    void 在场_立线之日够着_让位之日还没到() {
        assertEquals("PL-F02", service.locateLine(d("2024-06-01 08:00:00"), 1).getRuleCode());
        // 立线当日那一刻就算够着
        assertEquals("PL-F02", service.locateLine(d("2024-01-01 00:00:00"), 1).getRuleCode());
        // 还没立的不算
        BcastLinePick before = service.locateLine(d("2022-12-31 23:59:59"), 1);
        assertFalse(before.isUsable());
        assertEquals("无可用线", before.getWord());
    }

    @Test
    void 交接当日_旧线管到当日之前_新线当日进场_两个通道回话一致() {
        Date 前一刻 = d("2025-05-31 23:59:59");
        Date 当日 = d("2025-06-01 00:00:00");
        // 让位之日不含：当日之前仍听旧线
        assertEquals("PL-F02", service.locateLine(前一刻, 1).getRuleCode());
        // 新线自当日进场
        assertEquals("PL-F03", service.locateLine(当日, 1).getRuleCode());
        // 换两个通道问同一件事，回话必须一致：定位出口 = 屏上名单逐条排出来的头一条
        assertEquals(service.locateLine(前一刻, 1).getRuleCode(), topOnScreen("2025-05-31 23:59:59", 1));
        assertEquals(service.locateLine(当日, 1).getRuleCode(), topOnScreen("2025-06-01 00:00:00", 1));
        // 第三个通道 evaluateTop 也是同一条线：17 分钟在旧线落2档、在新线落3档
        assertEquals(2, service.evaluateTop(n("17"), 前一刻));
        assertEquals(3, service.evaluateTop(n("17"), 当日));
        // 旧线在让位当日那一刻已经不作数
        assertEquals(0, service.evaluate("PL-F02", n("17"), 当日));
    }

    @Test
    void 让位栏留白_按一直在场认_兼容旧卷存量() {
        // F03 让位栏留白：多远的以后都在场
        assertTrue(service.locateLine(d("2099-01-01 00:00:00"), 1).isUsable());
        // 旧卷存量：只填了立线之日的线，按一直在场认
        lineDb.put(301L, line(301, "PL-OLD", "旧卷存量线", 1,
                "1", "2", "3", "2020-01-01 00:00:00", null, 5, 0));
        assertTrue(service.usable(301L, d("2026-10-01 00:00:00")));
    }

    @Test
    void 停用线_不进判定_册底翻得到() {
        // PL-H01 虽让位栏留白，但整条停用：任何一刻都不选它出来
        assertFalse(service.locateLine(d("2025-03-10 09:20:00"), 2).isUsable());
        assertFalse(service.usable(201L, d("2025-03-10 09:20:00")));
        for (TBcastPreLine r : service.listOnScreen(d("2025-03-10 09:20:00"))) {
            assertFalse("PL-H01".equals(r.getRuleCode()));
        }
        // 退居册底不等于销毁：翻册子仍找得着
        boolean inLedger = false;
        for (TBcastPreLine r : service.listLedger()) {
            if ("PL-H01".equals(r.getRuleCode())) {
                inLedger = true;
            }
        }
        assertTrue(inLedger);
        // 按主键也翻得到
        assertEquals("PL-H01", service.selectTBcastPreLineById(201L).getRuleCode());
    }

    @Test
    void 同刻两条_先比让位顺位_顺位相同再比代号_末位者胜() {
        // 同窗口、同顺位：L-B 排在 L-A 末位，L-B 胜出
        lineDb.put(401L, line(401, "L-A", "并列甲", 1,
                "1", "2", "3", "2026-01-01 00:00:00", null, 100, 0));
        lineDb.put(402L, line(402, "L-B", "并列乙", 1,
                "1", "2", "3", "2026-01-01 00:00:00", null, 100, 0));
        assertEquals("L-B", service.locateLine(d("2026-03-01 00:00:00"), 1).getRuleCode());
        // 屏上名单同一个出处：B 在 A 前头
        assertEquals("L-B", topOnScreen("2026-03-01 00:00:00", 1));
        // 顺位不同：顺位高的说话，代号再靠后也让开
        lineDb.put(403L, line(403, "L-C", "顺位高", 1,
                "1", "2", "3", "2026-01-01 00:00:00", null, 101, 0));
        assertEquals("L-C", service.locateLine(d("2026-03-01 00:00:00"), 1).getRuleCode());
    }

    @Test
    void 补算往期_查册用往期那一刻_与眼下最新那条无关() {
        // 2024 年的事故：现行 F03 再新也不参与，听当刻在场的 F02
        assertEquals("PL-F02", service.locateLine(d("2024-06-01 00:00:00"), 1).getRuleCode());
        assertEquals(2, service.evaluate("PL-F02", n("17"), d("2024-06-01 00:00:00")));
        // 当刻不存在的现行代号，查不出来
        assertEquals(0, service.evaluate("PL-F03", n("17"), d("2024-06-01 00:00:00")));
    }

    @Test
    void 一条也套不上_结论写无可用线_代号立线日两边留白() {
        // 去年三月户数那头：H01 停用、H02 九月才立
        BcastLinePick pick = service.locateLine(d("2025-03-10 09:20:00"), 2);
        assertFalse(pick.isUsable());
        assertEquals("无可用线", pick.getWord());
        assertNull(pick.getRuleCode());
        assertNull(pick.getEffStart());
    }

    @Test
    void 在场名单_逐条走一遍_出处同一处() {
        for (String at : Arrays.asList("2023-06-01 00:00:00", "2024-06-01 00:00:00",
                "2025-03-10 09:20:00", "2025-09-02 00:00:00", "2026-10-01 00:00:00")) {
            // 名单总数 = 数出来的条数
            assertEquals(service.listOnScreen(d(at)).size(), service.countAvailable(d(at)));
            // 每一头名单头一条与定位出口是同一条
            for (int kind = 1; kind <= 2; kind++) {
                BcastLinePick pick = service.locateLine(d(at), kind);
                assertEquals(pick.isUsable() ? pick.getRuleCode() : null, topOnScreen(at, kind),
                        "时刻 " + at + " 两头 " + kind + " 两处回话不一致");
            }
        }
    }

    // ================= 老签名：进什么出什么 =================

    @Test
    void 老签名evaluate_分档边界与坏线() {
        Date at = d("2026-01-01 00:00:00"); // F03: 3 / 15 / 30
        assertEquals(1, service.evaluate("PL-F03", n("3"), at));   // 等于起算取头档
        assertEquals(2, service.evaluate("PL-F03", n("15"), at));  // 等于过半
        assertEquals(3, service.evaluate("PL-F03", n("30"), at));  // 等于封顶
        assertEquals(4, service.evaluate("PL-F03", n("30.01"), at)); // 高过封顶
        assertEquals(0, service.evaluate("PL-F03", n("-1"), at));  // 为负
        assertEquals(0, service.evaluate("PL-F03", null, at));
        assertEquals(0, service.evaluate("NO-SUCH", n("10"), at));
        // 分界数立不住的线，不作数
        lineDb.put(410L, line(410, "PL-BAD", "坏线", 1,
                "10", "9", "8", "2026-01-01 00:00:00", null, 200, 0));
        assertEquals(0, service.evaluate("PL-BAD", n("5"), at));
        assertEquals(0, service.evaluateTop(n("5"), at)); // 顺位虽高，线立不住也不给档
    }

    // ================= 两头合看：归层、越限摆数 =================

    @Test
    void 归层_两头各自不越封顶_取更外那头_线代号立线日钉死() {
        // 2026 年：分钟 F03(3/15/30)、户数 H02(300/1000/3000)
        BcastGradeVerdict v = service.grade("SG-2026-010", d("2026-03-15 08:30:00"),
                n("10"), n("500"));
        assertTrue(v.isSettled());
        assertEquals(Integer.valueOf(2), v.getGrade()); // 分钟落2、户数落2 → 2
        assertEquals("PL-F03", v.getMinuteLine().getRuleCode());
        assertEquals("PL-H02", v.getHouseLine().getRuleCode());
        // 随结论入库的栏位按出口这一趟回的话写
        TBcastCaseGrade rec = gradeDb.get(0);
        assertEquals("PL-F03", rec.getMinuteRuleCode());
        assertEquals(d("2025-06-01 00:00:00"), rec.getMinuteEffStart());
        assertEquals("PL-H02", rec.getHouseRuleCode());
        assertEquals(d("2025-09-01 00:00:00"), rec.getHouseEffStart());
        assertEquals(Integer.valueOf(1), rec.getSettled());

        // 两头都在头一档，归到一层
        BcastGradeVerdict v1 = service.grade("SG-2026-011", d("2026-03-16 08:30:00"),
                n("2"), n("200"));
        assertTrue(v1.isSettled());
        assertEquals(Integer.valueOf(1), v1.getGrade());
    }

    @Test
    void 归层_哪一头越了_把那一头实际数与封顶数摆出来() {
        // 分钟越封顶 30
        BcastGradeVerdict overMin = service.grade("SG-2026-020", d("2026-03-15 08:30:00"),
                n("40"), n("500"));
        assertFalse(overMin.isSettled());
        assertNull(overMin.getGrade());
        assertTrue(overMin.getReason().contains("40"));
        assertTrue(overMin.getReason().contains("30"));
        assertTrue(overMin.getReason().contains("停播分钟"));

        // 户数越封顶 3000：别只回一句"不够"
        BcastGradeVerdict overHouse = service.grade("SG-2026-021", d("2026-03-15 08:30:00"),
                n("10"), n("4000"));
        assertFalse(overHouse.isSettled());
        assertTrue(overHouse.getReason().contains("4000"));
        assertTrue(overHouse.getReason().contains("3000"));
        assertTrue(overHouse.getReason().contains("影响户数"));
    }

    @Test
    void 归层_一头无在场线_留白_不许借旁边线糊答复() {
        // 去年三月：分钟有 F02，户数无在场线
        BcastGradeVerdict v = service.grade("SG-2025-100", d("2025-03-10 09:20:00"),
                n("12"), n("800"));
        assertFalse(v.isSettled());
        assertNull(v.getGrade());
        assertNull(v.getHouseLine());
        assertEquals("PL-F02", v.getMinuteLine().getRuleCode());
        assertTrue(v.getReason().contains("无可用线"));
    }

    @Test
    void 没够上与后来补上是两笔_各留各的_不回头改() {
        BcastGradeVerdict miss = service.grade("SG-2026-030", d("2026-03-15 08:30:00"),
                n("40"), n("500"));
        assertFalse(miss.isSettled());
        BcastGradeVerdict hit = service.grade("SG-2026-030", d("2026-03-15 08:30:00"),
                n("10"), n("500"));
        assertTrue(hit.isSettled());
        // 两笔：头一笔没够上原样留着，不因后来补上回头改
        assertEquals(2, gradeDb.size());
        assertFalse(miss.getRecordId().equals(hit.getRecordId()));
        TBcastCaseGrade first = gradeDb.get(0);
        assertEquals(Integer.valueOf(0), first.getSettled());
        assertTrue(first.getExceedNote().contains("40"));
        assertNull(first.getGrade());
        assertEquals(Integer.valueOf(1), gradeDb.get(1).getSettled());
    }

    @Test
    void 归层_参数报不齐_不留笔() {
        assertNull(service.grade("  ", d("2026-03-15 08:30:00"), n("10"), n("500")));
        assertNull(service.grade("SG-X", null, n("10"), n("500")));
        assertEquals(0, gradeDb.size());
    }

    // ================= 历不对清点 =================

    /** 往结论账上放一笔落定结论 */
    private void gradeRecord(long id, String bizNo, String occurAt, String mins, String houses,
                             int grade, String minuteCode, String minuteStart,
                             String houseCode, String houseStart) {
        TBcastCaseGrade g = new TBcastCaseGrade();
        g.setId(Long.valueOf(id));
        g.setBizNo(bizNo);
        g.setOccurAt(d(occurAt));
        g.setStopMinutes(mins == null ? null : n(mins));
        g.setAffectHouseholds(houses == null ? null : n(houses));
        g.setGrade(Integer.valueOf(grade));
        g.setSettled(Integer.valueOf(1));
        g.setMinuteRuleCode(minuteCode);
        g.setMinuteEffStart(minuteStart == null ? null : d(minuteStart));
        g.setHouseRuleCode(houseCode);
        g.setHouseEffStart(houseStart == null ? null : d(houseStart));
        g.setDelFlag(Integer.valueOf(0));
        gradeDb.add(g);
    }

    @Test
    void 历不对_照着停用线定的两份_一条条摆出来() {
        gradeRecord(901, "SG-2025-001", "2025-03-10 09:20:00", "12", "800", 1,
                "PL-F02", "2024-01-01 00:00:00", "PL-H01", "2024-01-01 00:00:00");
        gradeRecord(902, "SG-2025-002", "2025-04-02 14:00:00", "30", "2600", 2,
                "PL-F02", "2024-01-01 00:00:00", "PL-H01", "2024-01-01 00:00:00");
        gradeRecord(903, "SG-2026-001", "2026-03-15 08:30:00", "10", "500", 1,
                "PL-F03", "2025-06-01 00:00:00", "PL-H02", "2025-09-01 00:00:00");

        List<BcastGradeAnomaly> anomalies = service.listGradeAnomalies();
        assertEquals(2, anomalies.size());
        // 一眼看出历不对：哪一笔、屏上仍挂着什么代号、哪一日
        assertEquals("SG-2025-001", anomalies.get(0).getBizNo());
        assertEquals("PL-H01", anomalies.get(0).getPinnedHouseCode());
        assertTrue(anomalies.get(0).isHouseSideFault());
        assertFalse(anomalies.get(0).isMinuteSideFault());
        assertTrue(anomalies.get(0).getHouseFaultNote().contains("找不出在场线"));
        assertEquals("SG-2025-002", anomalies.get(1).getBizNo());
        // 两头都对得上当刻在场线的那笔，不许摆出来
        for (BcastGradeAnomaly a : anomalies) {
            assertFalse("SG-2026-001".equals(a.getBizNo()));
        }
    }

    @Test
    void 历不对_当刻在场的已是另一条_也算_钉对旧线的不算() {
        // 钉着 F01，可事故当日在场的是 F02：分钟那头历不对（当刻在场的已是另一条）
        gradeRecord(911, "SG-2024-100", "2024-06-01 00:00:00", "12", "800", 1,
                "PL-F01", "2023-01-01 00:00:00", "PL-H02", "2025-09-01 00:00:00");
        // 钉着 F02、事故当日 F02 确在场：分钟那头钉得对；户数那头留白，另摆一头
        gradeRecord(912, "SG-2024-101", "2024-06-02 00:00:00", "12", "800", 1,
                "PL-F02", "2024-01-01 00:00:00", null, null);

        List<BcastGradeAnomaly> anomalies = service.listGradeAnomalies();
        BcastGradeAnomaly a911 = null;
        BcastGradeAnomaly a912 = null;
        for (BcastGradeAnomaly a : anomalies) {
            if ("SG-2024-100".equals(a.getBizNo())) {
                a911 = a;
            }
            if ("SG-2024-101".equals(a.getBizNo())) {
                a912 = a;
            }
        }
        // 911 两头都不对：分钟那头的说法里点得出当刻在场的 PL-F02
        assertTrue(a911 != null);
        assertTrue(a911.isMinuteSideFault());
        assertTrue(a911.getMinuteFaultNote().contains("PL-F02"));
        assertTrue(a911.isHouseSideFault());
        // 912 分钟那头钉得对，不许算它分钟历不对；户数那头留白仍摆它一头
        assertTrue(a912 != null);
        assertFalse(a912.isMinuteSideFault());
        assertTrue(a912.isHouseSideFault());
    }
}
