package com.fc.v2.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import com.fc.v2.mapper.auto.TBcastStopBillMapper;
import com.fc.v2.mapper.auto.TBcastStopScheduleMapper;
import com.fc.v2.mapper.auto.TBcastStopSignMapper;
import com.fc.v2.model.auto.TBcastStopBill;
import com.fc.v2.model.auto.TBcastStopSchedule;
import com.fc.v2.model.auto.TBcastStopSign;
import com.fc.v2.model.custom.BcastStopScheduleAudit;
import com.fc.v2.service.ITBcastStopBillService;

/**
 * 停播检修核准单：三级签批落名出口的单测。三张表用三本内存账顶替，落名、沉底、
 * 排定、誊名册都当着面在账上过——前年河湾区"纸面一张、库里三张、谁也归不到谁名下"
 * 的旧账，今年逐笔点得齐。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TBcastStopBillServiceImplTest {

    @Mock
    private TBcastStopBillMapper billMapper;
    @Mock
    private TBcastStopSignMapper signMapper;
    @Mock
    private TBcastStopScheduleMapper scheduleMapper;

    @org.mockito.InjectMocks
    private TBcastStopBillServiceImpl service;

    private final Map<Long, TBcastStopBill> bills = new LinkedHashMap<Long, TBcastStopBill>();
    private final Map<Long, TBcastStopSign> signs = new LinkedHashMap<Long, TBcastStopSign>();
    private final Map<Long, TBcastStopSchedule> schedules = new LinkedHashMap<Long, TBcastStopSchedule>();
    private long billSeq = 1000L;
    private long signSeq = 5000L;
    private long schedSeq = 9000L;

    @BeforeEach
    void mockDb() {
        bills.clear();
        signs.clear();
        schedules.clear();
        billSeq = 1000L;
        signSeq = 5000L;
        schedSeq = 9000L;

        when(billMapper.selectById(any())).thenAnswer(inv -> bills.get((Long) inv.getArgument(0)));
        when(billMapper.insert(any())).thenAnswer(inv -> {
            TBcastStopBill r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(Long.valueOf(++billSeq));
            }
            bills.put(r.getId(), r);
            return Integer.valueOf(1);
        });
        when(billMapper.updateById(any())).thenAnswer(inv -> {
            TBcastStopBill r = inv.getArgument(0);
            bills.put(r.getId(), r);
            return Integer.valueOf(1);
        });

        when(signMapper.insert(any())).thenAnswer(inv -> {
            TBcastStopSign r = inv.getArgument(0);
            r.setId(Long.valueOf(++signSeq));
            signs.put(r.getId(), r);
            return Integer.valueOf(1);
        });
        when(signMapper.updateById(any())).thenAnswer(inv -> {
            TBcastStopSign r = inv.getArgument(0);
            signs.put(r.getId(), r);
            return Integer.valueOf(1);
        });
        when(signMapper.selectList(any())).thenAnswer(inv -> {
            Long billId = firstLongParam((com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?>) inv.getArgument(0));
            List<TBcastStopSign> out = new ArrayList<TBcastStopSign>();
            for (TBcastStopSign s : signs.values()) {
                if (billId == null || billId.equals(s.getBillId())) {
                    out.add(s);
                }
            }
            return out;
        });

        when(scheduleMapper.insert(any())).thenAnswer(inv -> {
            TBcastStopSchedule r = inv.getArgument(0);
            r.setId(Long.valueOf(++schedSeq));
            schedules.put(r.getId(), r);
            return Integer.valueOf(1);
        });
        when(scheduleMapper.selectList(any())).thenAnswer(inv -> {
            Long billId = firstLongParam((com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<?>) inv.getArgument(0));
            List<TBcastStopSchedule> out = new ArrayList<TBcastStopSchedule>();
            for (TBcastStopSchedule s : schedules.values()) {
                if (billId == null || billId.equals(s.getBillId())) {
                    out.add(s);
                }
            }
            return out;
        });
    }

    /** 从 MyBatis-Plus 条件里把账单主键拣出来（mock 不真拼 SQL） */
    private static Long firstLongParam(QueryWrapper<?> w) {
        w.getSqlSegment();
        for (Object v : w.getParamNameValuePairs().values()) {
            if (v instanceof Long) {
                return (Long) v;
            }
        }
        return null;
    }

    // ================= 家什 =================

    private TBcastStopBill file(String billNo) {
        TBcastStopBill r = new TBcastStopBill();
        r.setBillNo(billNo);
        // 人递进来的档号、已记数、情形只算意图
        r.setNodeNo(Integer.valueOf(2));
        r.setSignCount(Integer.valueOf(99));
        r.setStatus(Integer.valueOf(2));
        assertEquals(1, service.fileTBcastStopBill(r));
        return r;
    }

    private TBcastStopBill sign(Long id, int node, String person) {
        return service.land(id, Integer.valueOf(node), ITBcastStopBillService.LAND_SIGN, person, null);
    }

    /** 台站1 → 市2 → 省2，一路点齐 */
    private void signAllTheWay(Long id, String station, String c1, String c2, String p1, String p2) {
        assertEquals(1, sign(id, 0, station).getNodeNo().intValue());
        assertEquals(1, sign(id, 1, c1).getNodeNo().intValue()); // 市里缺一名，仍挂在市
        assertEquals(2, sign(id, 1, c2).getNodeNo().intValue()); // 市里点齐，挪到省
        TBcastStopBill p1r = sign(id, 2, p1);
        assertEquals(2, p1r.getNodeNo().intValue());           // 省局缺一名，仍挂在省
        assertEquals(0, p1r.getStatus().intValue());
        TBcastStopBill sealed = sign(id, 2, p2);
        assertEquals(1, sealed.getStatus().intValue());        // 省局点齐，排定
    }

    private List<TBcastStopSign> aliveSigns(Long billId, int node, int round) {
        List<TBcastStopSign> out = new ArrayList<TBcastStopSign>();
        for (TBcastStopSign s : service.listSigns(billId)) {
            if (s.getNodeNo() == node && s.getRoundNo() == round && s.getAliveFlag() == 0
                    && s.getKind() == 0) {
                out.add(s);
            }
        }
        return out;
    }

    private List<TBcastStopSign> sunkSigns(Long billId, int node, int round) {
        List<TBcastStopSign> out = new ArrayList<TBcastStopSign>();
        for (TBcastStopSign s : service.listSigns(billId)) {
            if (s.getNodeNo() == node && s.getRoundNo() == round && s.getAliveFlag() == 1) {
                out.add(s);
            }
        }
        return out;
    }

    private List<TBcastStopSchedule> rosterOf(Long billId, int node) {
        List<TBcastStopSchedule> out = new ArrayList<TBcastStopSchedule>();
        for (TBcastStopSchedule s : service.listSchedule(billId)) {
            if (s.getNodeNo() == node) {
                out.add(s);
            }
        }
        return out;
    }

    // ================= 立卡：系统落笔 =================

    @Test
    void 立案_档号算法已记数情形一律系统落笔_人递的只算意图() {
        TBcastStopBill r = file("JX-01");
        assertEquals(Integer.valueOf(0), r.getNodeNo());
        assertEquals(Integer.valueOf(0), r.getSignMode()); // 台站一笔即可
        assertEquals(Integer.valueOf(1), r.getNeedCount());
        assertEquals(Integer.valueOf(0), r.getSignCount());
        assertEquals(Integer.valueOf(0), r.getStatus());
        // 单号空着立不起来
        assertEquals(0, service.fileTBcastStopBill(new TBcastStopBill()));
    }

    // ================= 三档算法各不同 =================

    @Test
    void 台站头一名落下便过_市里省局缺一名仍挂候点() {
        TBcastStopBill r = file("JX-02");
        // 台站一人即过：单子挪到市，台档底账在册1笔
        TBcastStopBill atCity = sign(r.getId(), 0, "台长老王");
        assertEquals(Integer.valueOf(1), atCity.getNodeNo());
        assertEquals(Integer.valueOf(2), atCity.getNeedCount());
        assertEquals(Integer.valueOf(0), atCity.getSignCount());
        assertEquals(1, aliveSigns(r.getId(), 0, 1).size());

        // 市里头一名：缺一人，仍挂市，不挪档
        TBcastStopBill one = sign(r.getId(), 1, "市甲");
        assertEquals(Integer.valueOf(1), one.getNodeNo());
        assertEquals(Integer.valueOf(1), one.getSignCount());

        // 同一人不顶两名
        assertNull(sign(r.getId(), 1, "市甲"));
        assertEquals(1, aliveSigns(r.getId(), 1, 1).size());

        // 市里第二名点齐才挪到省
        assertEquals(2, sign(r.getId(), 1, "市乙").getNodeNo().intValue());
        assertEquals(2, aliveSigns(r.getId(), 1, 1).size());
    }

    // ================= 越档不作数 =================

    @Test
    void 绕开市里直落省局_单子仍卡市里_那一笔不入库() {
        TBcastStopBill r = file("JX-03");
        sign(r.getId(), 0, "台长老王");
        sign(r.getId(), 1, "市甲"); // 市里只点一名，单子卡在市
        // 递个省局档号来落名：意图与当前档不合，整笔不作数
        assertNull(sign(r.getId(), 2, "省甲"));
        // 库里仍是卡在市、已记1人
        TBcastStopBill reread = service.selectTBcastStopBillById(r.getId());
        assertEquals(Integer.valueOf(1), reread.getNodeNo());
        assertEquals(Integer.valueOf(1), reread.getSignCount());
        // 省局一笔都没落下
        assertEquals(0, aliveSigns(r.getId(), 2, 1).size());
    }

    @Test
    void 卷面档号被拨到省局_出口顺着底账压回缺名的市里() {
        TBcastStopBill r = file("JX-04");
        sign(r.getId(), 0, "台长老王");
        // 库里档号被人拨到省局（市只点了一名）
        TBcastStopBill tampered = bills.get(r.getId());
        tampered.setNodeNo(Integer.valueOf(2));
        // 回查自愈：压回市里
        assertEquals(Integer.valueOf(1), service.selectTBcastStopBillById(r.getId()).getNodeNo());
        // 拿省局档号来落名照样不作数
        assertNull(sign(r.getId(), 2, "省甲"));
        // 档号没递（null）也不作数
        assertNull(service.land(r.getId(), null, ITBcastStopBillService.LAND_SIGN, "市乙", null));
        // 落名人空着不作数
        assertNull(sign(r.getId(), 1, "  "));
    }

    // ================= 退回一格：本档清零、旧档不动、新旧分轮 =================

    @Test
    void 省局退回市里_省局当轮名沉底清零_台站市里旧名一笔不动() {
        TBcastStopBill r = file("JX-05");
        signAllTheWay(r.getId(), "台长", "市甲", "市乙", "省甲", "省乙");
        // 排定后退不回（已了断）——这里先测在核时退回，故另立一张走到省局只点一名
        TBcastStopBill g = file("JX-05B");
        sign(g.getId(), 0, "台长");
        sign(g.getId(), 1, "市甲");
        sign(g.getId(), 1, "市乙");
        sign(g.getId(), 2, "省甲"); // 省局缺一名，挂在省
        assertEquals(2, service.selectTBcastStopBillById(g.getId()).getNodeNo().intValue());

        // 省局退回：只退一格到市
        TBcastStopBill back = service.land(g.getId(), Integer.valueOf(2),
                ITBcastStopBillService.LAND_RETURN, "省值班主任", "材料需补");
        assertEquals(Integer.valueOf(1), back.getNodeNo());
        // 市档早先两名一笔不动，档上已记仍是2
        assertEquals(Integer.valueOf(2), back.getSignCount());
        assertEquals(2, aliveSigns(g.getId(), 1, 1).size());
        // 台站旧名也不动
        assertEquals(1, aliveSigns(g.getId(), 0, 1).size());
        // 省局当轮那一名沉底（行还在，不再作数），不抹掉
        assertEquals(1, sunkSigns(g.getId(), 2, 1).size());
        assertEquals(0, aliveSigns(g.getId(), 2, 1).size());

        // 头一道无处可退（另立一张停在台站的单验）
        TBcastStopBill at0 = file("JX-05D");
        assertNull(service.land(at0.getId(), Integer.valueOf(0),
                ITBcastStopBillService.LAND_RETURN, null, null));

        // 市里旧名本就点齐：放行即可，不叫市里重点；省局重走开新一轮
        TBcastStopBill again = service.land(g.getId(), Integer.valueOf(1),
                ITBcastStopBillService.LAND_RESUBMIT, "市值班主任", null);
        assertEquals(Integer.valueOf(2), again.getNodeNo());
        assertEquals(Integer.valueOf(0), again.getSignCount());
        // 前档缺名时放行不作数（另起一张卡在市里的单验）
        TBcastStopBill h = file("JX-05C");
        sign(h.getId(), 0, "台长");
        sign(h.getId(), 1, "市甲");
        assertNull(service.land(h.getId(), Integer.valueOf(1),
                ITBcastStopBillService.LAND_RESUBMIT, null, null));
    }

    @Test
    void 退回后重走_省局换一拨人_新旧两轮分开记名册只认新作数轮() {
        TBcastStopBill g = file("JX-06");
        sign(g.getId(), 0, "台长");
        sign(g.getId(), 1, "市甲");
        sign(g.getId(), 1, "市乙");
        sign(g.getId(), 2, "旧省甲");
        service.land(g.getId(), Integer.valueOf(2), ITBcastStopBillService.LAND_RETURN, "省主任", null);
        service.land(g.getId(), Integer.valueOf(1), ITBcastStopBillService.LAND_RESUBMIT, null, null);

        // 省局新一轮点两个新人
        sign(g.getId(), 2, "新省甲");
        TBcastStopBill sealed = sign(g.getId(), 2, "新省乙");
        assertEquals(1, sealed.getStatus().intValue());

        // 省局头一轮旧名沉底，第二轮两名在册；新旧两拨分开留底
        assertEquals(1, sunkSigns(g.getId(), 2, 1).size());
        List<TBcastStopSign> round2 = aliveSigns(g.getId(), 2, 2);
        assertEquals(2, round2.size());
        // 名册里省局这一块只誊新一轮的两个人，旧的不进
        List<TBcastStopSchedule> provinceRoster = rosterOf(g.getId(), 2);
        assertEquals(2, provinceRoster.size());
        for (TBcastStopSchedule row : provinceRoster) {
            assertEquals(Integer.valueOf(2), row.getRoundNo());
            assertTrue(row.getSigner().startsWith("新省"));
        }
    }

    // ================= 否决了断 =================

    @Test
    void 哪一档否了全单了断_往后各档不得再记名() {
        TBcastStopBill r = file("JX-07");
        sign(r.getId(), 0, "台长");
        TBcastStopBill vetoed = service.land(r.getId(), Integer.valueOf(1),
                ITBcastStopBillService.LAND_VETO, "市局长", "不同意停");
        assertEquals(Integer.valueOf(2), vetoed.getStatus());
        // 否决那笔留在底账
        boolean hasVeto = false;
        for (TBcastStopSign s : service.listSigns(r.getId())) {
            if (s.getKind() == 1) {
                hasVeto = true;
            }
        }
        assertTrue(hasVeto);
        // 往后落名、退回、放行一概不作数
        assertNull(sign(r.getId(), 1, "市甲"));
        assertNull(service.land(r.getId(), Integer.valueOf(1),
                ITBcastStopBillService.LAND_RETURN, null, null));
        // 没排定就没有名册，无从对账
        assertNull(service.auditSchedule(r.getId()));
        assertTrue(service.listSchedule(r.getId()).isEmpty());
    }

    // ================= 排定 + 名册同一次算 + 对账 =================

    @Test
    void 省局点齐即排定_名册五笔与底账逐笔拴死_总数与每块都平() {
        TBcastStopBill r = file("JX-08");
        signAllTheWay(r.getId(), "台长", "市甲", "市乙", "省甲", "省乙");

        // 卡面四栏：落在省局、应点2、已记2、已排定
        TBcastStopBill view = service.selectTBcastStopBillById(r.getId());
        assertEquals(Integer.valueOf(2), view.getNodeNo());
        assertEquals(Integer.valueOf(1), view.getSignMode());
        assertEquals(Integer.valueOf(2), view.getNeedCount());
        assertEquals(Integer.valueOf(2), view.getSignCount());
        assertEquals(Integer.valueOf(1), view.getStatus());
        // 卷面已记数被人填花，回查仍按底账点出来的盖回
        bills.get(r.getId()).setSignCount(Integer.valueOf(99));
        assertEquals(Integer.valueOf(2), service.selectTBcastStopBillById(r.getId()).getSignCount());

        // 名册五笔：台1 市2 省2，按档口、名次排
        List<TBcastStopSchedule> roster = service.listSchedule(r.getId());
        assertEquals(5, roster.size());
        assertEquals(1, rosterOf(r.getId(), 0).size());
        assertEquals(2, rosterOf(r.getId(), 1).size());
        assertEquals(2, rosterOf(r.getId(), 2).size());
        // 每行都拴得回底账原笔
        for (TBcastStopSchedule row : roster) {
            assertNotNull(row.getSignId());
            assertEquals(signs.get(row.getSignId()).getSigner(), row.getSigner());
        }

        BcastStopScheduleAudit audit = service.auditSchedule(r.getId());
        assertTrue(audit.isScheduled());
        assertTrue(audit.isBalanced());
        assertEquals(5, audit.getRequiredCount());
        assertEquals(5, audit.getLedgerCount());
        assertEquals(5, audit.getScheduleCount());
        // 自省局往回收数到台站的块次序：省、市、台
        assertEquals(2, audit.getBlocks().get(0).getNodeNo());
        assertEquals(1, audit.getBlocks().get(1).getNodeNo());
        assertEquals(0, audit.getBlocks().get(2).getNodeNo());
        // 任取一块单看也平，逐笔 matched
        for (BcastStopScheduleAudit.Block b : audit.getBlocks()) {
            assertTrue(b.isBalanced(), b.getNodeName() + "这一块应当平");
            assertEquals(b.getNeedCount(), b.getSignCount());
            assertEquals(b.getNeedCount(), b.getScheduleCount());
            for (BcastStopScheduleAudit.Entry e : b.getEntries()) {
                assertTrue(e.isMatched());
                assertNotNull(e.getSignId());
                assertNotNull(e.getScheduleId());
            }
        }
        // 底账全程留着（在册5笔，无沉底无否决）
        assertEquals(5, audit.getHistory().size());
    }

    @Test
    void 排定之后谁也添不进名_退回否决也都不接() {
        TBcastStopBill r = file("JX-09");
        signAllTheWay(r.getId(), "台长", "市甲", "市乙", "省甲", "省乙");
        int rosterBefore = service.listSchedule(r.getId()).size();

        assertNull(sign(r.getId(), 2, "来人丙"));
        assertNull(service.land(r.getId(), Integer.valueOf(2),
                ITBcastStopBillService.LAND_RETURN, null, null));
        assertNull(service.land(r.getId(), Integer.valueOf(2),
                ITBcastStopBillService.LAND_VETO, null, null));
        assertEquals(rosterBefore, service.listSchedule(r.getId()).size());
        assertEquals(Integer.valueOf(1), service.selectTBcastStopBillById(r.getId()).getStatus());
    }

    @Test
    void 对账_名册缺一笔_差在哪一块当场点得出_整块不平() {
        TBcastStopBill r = file("JX-10");
        signAllTheWay(r.getId(), "台长", "市甲", "市乙", "省甲", "省乙");
        // 人为抽掉名册里市档一行（模拟一处取数一处手填对不上）
        Long toRemove = null;
        for (TBcastStopSchedule s : schedules.values()) {
            if (s.getBillId().equals(r.getId()) && s.getNodeNo() == 1) {
                toRemove = s.getId();
                break;
            }
        }
        assertNotNull(toRemove);
        schedules.remove(toRemove);

        BcastStopScheduleAudit audit = service.auditSchedule(r.getId());
        assertFalse(audit.isBalanced());
        assertEquals(5, audit.getRequiredCount());
        assertEquals(5, audit.getLedgerCount());
        assertEquals(4, audit.getScheduleCount());
        BcastStopScheduleAudit.Block city = null;
        for (BcastStopScheduleAudit.Block b : audit.getBlocks()) {
            if (b.getNodeNo() == 1) {
                city = b;
            }
        }
        assertNotNull(city);
        assertFalse(city.isBalanced());
        assertEquals(2, city.getSignCount());
        assertEquals(1, city.getScheduleCount());
        boolean namedCity = false;
        for (String m : audit.getMismatches()) {
            if (m.contains("市广电局")) {
                namedCity = true;
            }
        }
        assertTrue(namedCity);
    }
}
