package com.fc.v2.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * 停播检修核准单 Service业务层处理（approval-chain 形状：三级签批，一条链走到底）。
 *
 * <p>前年河湾区那张单在台站、市广电局、省局之间来回退了两趟：退回来重走时先前各档的名
 * 被一并抹净，台站等于把同一件事点了两遍；市里重走又是另一拨人，省局翻账时三道记录谁也
 * 归不到谁名下。今年把规矩收口到本类：单子走到哪一档、下一档收不收、省局点齐后还添不添
 * 得进人、越档先落作不作数、退一档本档这一轮的名清不清——五样结论全出在
 * {@link #land} 这一个落名出口，别处不开第二处能把档号直接改定的口子。</p>
 *
 * <p>档号（node_no）只由本出口落笔：接口递上来的档号当意图看，与出口数出来的当前档不合，
 * 整笔不作数。已记几人不由谁拿手填——{@link #recomputeView} 顺着落名底账
 * （t_bcast_stop_sign）一笔一笔点，点出来的条数与档口应点人数持平才算数，屏上那个数
 * 只当展示。退回一格时本档当轮的名冠"沉底"另放（行不删），早先各档旧名一笔不动；重走
 * 另起一轮，新旧两拨分开记。省局点齐，单子排定，检修排期名册在同一事务里从底账当前作数
 * 轮一笔誊成（每行拴着底账原笔 sign_id）——核准结果与名册出自同一次算，不许手填。</p>
 *
 * <p>不继承 ServiceImpl：IService 那一套 save/updateById/removeById 是绕得过去的
 * 第二道口子，收口规矩不许它开张。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TBcastStopBillServiceImpl implements ITBcastStopBillService {

    private static final Logger log = LoggerFactory.getLogger(TBcastStopBillServiceImpl.class);

    /** 三道关口：台站、市广电局、省局 */
    private static final int NODE_STATION = 0;
    private static final int NODE_CITY = 1;
    private static final int NODE_PROVINCE = 2;
    private static final int MAX_NODE = NODE_PROVINCE;

    /** 同档点齐办法：0一笔即可（台站），1两笔点齐（市、省） */
    private static final int MODE_OR = 0;
    private static final int MODE_AND = 1;

    /** 核准情形 */
    private static final int STATUS_RUNNING = 0;
    private static final int STATUS_SCHEDULED = 1;
    private static final int STATUS_VETOED = 2;

    /** 落名种类 */
    private static final int KIND_SIGN = 0;
    private static final int KIND_VETO = 1;

    /** 底账行情形 */
    private static final int ALIVE_YES = 0;
    private static final int ALIVE_SUNK = 1;

    /** 各档应点人数：台站1、市2、省2。三档算法各定各的，不照着彼此套 */
    private static final int[] NEED_BY_NODE = {1, 2, 2};

    private static final String[] NODE_NAMES = {"台站", "市广电局", "省局"};

    @javax.annotation.Resource
    private TBcastStopBillMapper bcastStopBillMapper;

    @javax.annotation.Resource
    private TBcastStopSignMapper bcastStopSignMapper;

    @javax.annotation.Resource
    private TBcastStopScheduleMapper bcastStopScheduleMapper;

    /**
     * 读单（签名照旧，不塞新参）。回查时把档口各栏按底账重数一遍盖回去：
     * 卷面带出的已记数/应点数与出口回话不合，按回话那一份走。
     */
    @Override
    public TBcastStopBill selectTBcastStopBillById(Long id) {
        if (id == null) {
            return null;
        }
        TBcastStopBill bill = this.bcastStopBillMapper.selectById(id);
        if (bill == null) {
            return null;
        }
        List<TBcastStopSign> signs = loadSigns(id);
        recomputeView(bill, signs);
        return bill;
    }

    /**
     * 立单。单号之外的档号、算法、已记数、情形一律由系统落笔：从台站、在核起，
     * 人递进来的 nodeNo/signCount/status 只算意图，不入库。
     */
    @Override
    public int fileTBcastStopBill(TBcastStopBill record) {
        if (record == null) {
            return 0;
        }
        String billNo = record.getBillNo() == null ? "" : record.getBillNo().trim();
        if (billNo.isEmpty()) {
            return 0;
        }
        record.setBillNo(billNo);
        // 眼下归在哪一档由系统落笔：一律从台站那一道、头一轮起
        record.setNodeNo(Integer.valueOf(NODE_STATION));
        record.setSignMode(Integer.valueOf(modeOf(NODE_STATION)));
        record.setNeedCount(Integer.valueOf(needOf(NODE_STATION)));
        record.setSignCount(Integer.valueOf(0));
        record.setStatus(Integer.valueOf(STATUS_RUNNING));
        record.setDelFlag(Integer.valueOf(0));
        return this.bcastStopBillMapper.insert(record);
    }

    /**
     * 落名出口——落名、退回、否决、退回后放行四桩走的都是这一个口子。
     * 全方法一个事务：省局点齐时单改状态、底账落笔、名册誊写同成同败。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TBcastStopBill land(Long id, Integer intentNodeNo, int action, String signer, String comment) {
        if (id == null) {
            return null;
        }
        TBcastStopBill bill = this.bcastStopBillMapper.selectById(id);
        if (bill == null) {
            return null;
        }
        // 已排定、已否决的单一律了断：往后谁也不能再记名，退回否决也都不接
        int status = bill.getStatus() == null ? STATUS_RUNNING : bill.getStatus().intValue();
        if (status != STATUS_RUNNING) {
            log.info("核准单{}已了断（情形{}），落名出口不再接任何动作", id, Integer.valueOf(status));
            return null;
        }
        List<TBcastStopSign> signs = loadSigns(id);
        recomputeView(bill, signs);
        int node = bill.getNodeNo() == null ? NODE_STATION : bill.getNodeNo().intValue();

        // 递上来的档号只当意图：与出口数出来的当前档不合，整笔不作数。
        // 绕开市里直落省局、往还没走到的档先落一笔，单子仍卡在原档。
        if (intentNodeNo == null || intentNodeNo.intValue() != node) {
            log.info("核准单{}递上来的档号{}与当前档{}不合，整笔不作数", id, intentNodeNo, Integer.valueOf(node));
            return null;
        }

        String person = signer == null ? "" : signer.trim();
        String words = comment == null ? "" : comment.trim();

        switch (action) {
            case LAND_SIGN:
                return doSign(bill, signs, node, person, words);
            case LAND_RETURN:
                return doReturn(bill, signs, node, person, words);
            case LAND_VETO:
                return doVeto(bill, signs, node, person, words);
            case LAND_RESUBMIT:
                return doResubmit(bill, signs, node, person, words);
            default:
                return null;
        }
    }

    @Override
    public List<TBcastStopSchedule> listSchedule(Long id) {
        if (id == null) {
            return new ArrayList<TBcastStopSchedule>();
        }
        List<TBcastStopSchedule> rows = this.bcastStopScheduleMapper.selectList(
                new QueryWrapper<TBcastStopSchedule>()
                        .eq("bill_id", id).eq("del_flag", 0)
                        .orderByAsc("node_no", "seq_no"));
        return rows == null ? new ArrayList<TBcastStopSchedule>() : rows;
    }

    @Override
    public List<TBcastStopSign> listSigns(Long id) {
        if (id == null) {
            return new ArrayList<TBcastStopSign>();
        }
        List<TBcastStopSign> rows = loadSigns(id);
        sortSigns(rows);
        return rows;
    }

    /**
     * 事后翻查对账：自省局往回收数到台站，一块一块清点。每档一块，块上应点几人、
     * 底账当前作数轮几笔、名册几笔、逐笔拴不拴得住都在；总数与任取一块都平才算过。
     */
    @Override
    public BcastStopScheduleAudit auditSchedule(Long id) {
        if (id == null) {
            return null;
        }
        TBcastStopBill bill = this.bcastStopBillMapper.selectById(id);
        if (bill == null || bill.getStatus() == null
                || bill.getStatus().intValue() != STATUS_SCHEDULED) {
            // 没排定就没有排期名册，无从对账
            return null;
        }
        List<TBcastStopSign> signs = loadSigns(id);
        List<TBcastStopSchedule> schedule = listSchedule(id);

        BcastStopScheduleAudit audit = new BcastStopScheduleAudit();
        audit.setBillId(bill.getId());
        audit.setBillNo(bill.getBillNo());
        audit.setScheduled(true);
        audit.setHistory(signs);

        Map<Long, TBcastStopSchedule> scheduleBySignId = new HashMap<Long, TBcastStopSchedule>();
        for (TBcastStopSchedule s : schedule) {
            if (s.getSignId() != null) {
                scheduleBySignId.put(s.getSignId(), s);
            }
        }

        int requiredTotal = 0;
        int ledgerTotal = 0;
        boolean allBalanced = true;

        // 自省局往回收数到台站：2→1→0
        for (int node = MAX_NODE; node >= NODE_STATION; node--) {
            List<TBcastStopSign> winners = winningSigns(signs, node);
            sortSigns(winners);
            int need = needOf(node);
            BcastStopScheduleAudit.Block block = new BcastStopScheduleAudit.Block();
            block.setNodeNo(node);
            block.setNodeName(NODE_NAMES[node]);
            block.setRoundNo(winners.isEmpty() ? 0 : winners.get(0).getRoundNo().intValue());
            block.setNeedCount(need);
            block.setSignCount(winners.size());
            block.setScheduleCount(countScheduleByNode(schedule, node));

            boolean blockBalanced = winners.size() == need;
            for (TBcastStopSign sg : winners) {
                TBcastStopSchedule sc = sg.getId() == null ? null : scheduleBySignId.get(sg.getId());
                boolean matched = sc != null
                        && sc.getNodeNo() != null && sc.getNodeNo().intValue() == node
                        && eq(sc.getSigner(), sg.getSigner())
                        && sc.getSeqNo() != null && sc.getSeqNo().equals(sg.getSeqNo());
                block.getEntries().add(new BcastStopScheduleAudit.Entry(
                        sg.getSeqNo() == null ? 0 : sg.getSeqNo().intValue(),
                        sg.getSigner(), sg.getId(), sc == null ? null : sc.getId(), matched));
                if (!matched) {
                    blockBalanced = false;
                }
            }
            // 名册里多出底账拴不住的行，也算不平
            if (block.getScheduleCount() != winners.size()) {
                blockBalanced = false;
            }
            block.setBalanced(blockBalanced);
            audit.getBlocks().add(block);

            requiredTotal += need;
            ledgerTotal += winners.size();
            allBalanced &= blockBalanced;
            if (!blockBalanced) {
                audit.getMismatches().add(NODE_NAMES[node] + "这一块对不平：应点" + need
                        + "、底账在册" + winners.size() + "、名册" + block.getScheduleCount());
            }
        }

        audit.setRequiredCount(requiredTotal);
        audit.setLedgerCount(ledgerTotal);
        audit.setScheduleCount(schedule.size());
        audit.setBalanced(allBalanced
                && requiredTotal == ledgerTotal
                && requiredTotal == schedule.size());
        return audit;
    }

    // ================= 落名出口里的四桩 =================

    /** 落名：只在当前档当前轮点一笔；点齐才挪到紧挨着的下一档，省局点齐则排定誊名册 */
    private TBcastStopBill doSign(TBcastStopBill bill, List<TBcastStopSign> signs,
                                  int node, String person, String words) {
        if (person.isEmpty()) {
            return null;
        }
        int[] roundHolder = currentRound(signs, node);
        int round = roundHolder[0];
        int alive = roundHolder[1];
        int need = needOf(node);

        // 当前档早已点齐（退回后落回的前一档）：不接新名，要往前走请走放行那一桩
        if (alive >= need) {
            log.info("核准单{}第{}档已点齐，落名不收，请放行", bill.getId(), Integer.valueOf(node));
            return null;
        }
        // 同一档同一轮里一个人不能顶两个名：市里、省局要的是两个人
        if (hasSigner(signs, node, round, person)) {
            log.info("核准单{}第{}档第{}轮{}已点过，不顶第二名", bill.getId(),
                    Integer.valueOf(node), Integer.valueOf(round), person);
            return null;
        }

        TBcastStopSign sign = newSign(bill, node, round, alive + 1, person, words, KIND_SIGN, ALIVE_YES);
        this.bcastStopSignMapper.insert(sign);
        signs.add(sign);

        int nowAlive = alive + 1;
        if (nowAlive < need) {
            // 缺一名仍在窗口挂候点，单子不挪档
            bill.setSignCount(Integer.valueOf(nowAlive));
            bill.setUpdateTime(new Date());
            this.bcastStopBillMapper.updateById(bill);
            return bill;
        }

        // 这一档点齐
        if (node == NODE_PROVINCE) {
            // 省局点齐：单子排定，同一次算里把名册誊成
            sealScheduled(bill, signs);
            return bill;
        }
        // 只挪到紧挨着的下一道；新档开新一轮（若曾退回过，轮次接着往下排）
        int next = node + 1;
        bill.setNodeNo(Integer.valueOf(next));
        bill.setSignMode(Integer.valueOf(modeOf(next)));
        bill.setNeedCount(Integer.valueOf(needOf(next)));
        bill.setSignCount(Integer.valueOf(0));
        bill.setUpdateTime(new Date());
        this.bcastStopBillMapper.updateById(bill);
        return bill;
    }

    /** 退回：只退一格；本档当轮已点下的名当场沉底清零，早先各档旧名一笔不动 */
    private TBcastStopBill doReturn(TBcastStopBill bill, List<TBcastStopSign> signs,
                                    int node, String person, String words) {
        if (node <= NODE_STATION) {
            // 头一道没人可退
            log.info("核准单{}已在头一道台站，无处可退", bill.getId());
            return null;
        }
        int[] roundHolder = currentRound(signs, node);
        int round = roundHolder[0];
        List<TBcastStopSign> aliveHere = new ArrayList<TBcastStopSign>();
        for (TBcastStopSign s : signs) {
            if (s.getNodeNo() != null && s.getNodeNo().intValue() == node
                    && s.getRoundNo() != null && s.getRoundNo().intValue() == round
                    && s.getKind() != null && s.getKind().intValue() == KIND_SIGN
                    && s.getAliveFlag() != null && s.getAliveFlag().intValue() == ALIVE_YES) {
                aliveHere.add(s);
            }
        }
        // 本档这一轮已点下的名当场清零：不抹行，冠沉底另放
        for (TBcastStopSign s : aliveHere) {
            s.setAliveFlag(Integer.valueOf(ALIVE_SUNK));
            this.bcastStopSignMapper.updateById(s);
        }

        int prev = node - 1;
        bill.setNodeNo(Integer.valueOf(prev));
        bill.setSignMode(Integer.valueOf(modeOf(prev)));
        bill.setNeedCount(Integer.valueOf(needOf(prev)));
        // 落回上一档：它早先点下的旧名一笔不动，档上已记数照旧名点出来
        int[] prevRound = currentRound(signs, prev);
        bill.setSignCount(Integer.valueOf(prevRound[1]));
        appendRemark(bill, NODE_NAMES[node] + "退回至" + NODE_NAMES[prev]
                + "（本档第" + round + "轮" + aliveHere.size() + "名沉底）"
                + (person.isEmpty() ? "" : "：" + person)
                + (words.isEmpty() ? "" : " " + words));
        this.bcastStopBillMapper.updateById(bill);
        return bill;
    }

    /** 否决：哪一档否了，全单就此了断，往后各档不得再记名 */
    private TBcastStopBill doVeto(TBcastStopBill bill, List<TBcastStopSign> signs,
                                  int node, String person, String words) {
        int[] roundHolder = currentRound(signs, node);
        TBcastStopSign veto = newSign(bill, node, roundHolder[0], roundHolder[1] + 1,
                person.isEmpty() ? null : person, words, KIND_VETO, ALIVE_YES);
        this.bcastStopSignMapper.insert(veto);
        signs.add(veto);

        bill.setStatus(Integer.valueOf(STATUS_VETOED));
        bill.setSignCount(Integer.valueOf(roundHolder[1]));
        appendRemark(bill, NODE_NAMES[node] + "否决，全单了断"
                + (person.isEmpty() ? "" : "：" + person)
                + (words.isEmpty() ? "" : " " + words));
        this.bcastStopBillMapper.updateById(bill);
        return bill;
    }

    /**
     * 退回后放行：单子落回的前一档旧名本就点齐，从这一档接着往上前行；
     * 前一档缺名未齐的，放不了行；下一档重走开新一轮，新旧两拨分开记。
     */
    private TBcastStopBill doResubmit(TBcastStopBill bill, List<TBcastStopSign> signs,
                                      int node, String person, String words) {
        if (node >= NODE_PROVINCE) {
            // 省局没有下一道可放行
            return null;
        }
        int[] roundHolder = currentRound(signs, node);
        if (roundHolder[1] < needOf(node)) {
            // 前一档尚有缺名未齐，不许往上挪
            log.info("核准单{}第{}档尚欠名，放行不作数", bill.getId(), Integer.valueOf(node));
            return null;
        }
        int next = node + 1;
        bill.setNodeNo(Integer.valueOf(next));
        bill.setSignMode(Integer.valueOf(modeOf(next)));
        bill.setNeedCount(Integer.valueOf(needOf(next)));
        // 下一档重走另起一轮（其旧轮已在退回时沉底）；新轮还没落名，已记为0
        bill.setSignCount(Integer.valueOf(0));
        appendRemark(bill, NODE_NAMES[node] + "放行至" + NODE_NAMES[next]
                + (person.isEmpty() ? "" : "：" + person)
                + (words.isEmpty() ? "" : " " + words));
        this.bcastStopBillMapper.updateById(bill);
        return bill;
    }

    // ================= 排定与名册 =================

    /** 省局点齐：状态排定，名册从底账各档当前作数轮一笔誊成，同事务同成败 */
    private void sealScheduled(TBcastStopBill bill, List<TBcastStopSign> signs) {
        for (int node = NODE_STATION; node <= NODE_PROVINCE; node++) {
            List<TBcastStopSign> winners = winningSigns(signs, node);
            sortSigns(winners);
            for (TBcastStopSign sg : winners) {
                TBcastStopSchedule row = new TBcastStopSchedule();
                row.setBillId(bill.getId());
                row.setBillNo(bill.getBillNo());
                row.setNodeNo(Integer.valueOf(node));
                row.setRoundNo(sg.getRoundNo());
                row.setSeqNo(sg.getSeqNo());
                row.setSigner(sg.getSigner());
                row.setSignId(sg.getId());
                row.setDelFlag(Integer.valueOf(0));
                this.bcastStopScheduleMapper.insert(row);
            }
        }
        bill.setNodeNo(Integer.valueOf(NODE_PROVINCE));
        bill.setSignMode(Integer.valueOf(modeOf(NODE_PROVINCE)));
        bill.setNeedCount(Integer.valueOf(needOf(NODE_PROVINCE)));
        bill.setSignCount(Integer.valueOf(needOf(NODE_PROVINCE)));
        bill.setStatus(Integer.valueOf(STATUS_SCHEDULED));
        bill.setUpdateTime(new Date());
        this.bcastStopBillMapper.updateById(bill);
    }

    // ================= 底账点人 =================

    /**
     * 把卡面四栏按底账重数盖回：当前档、该档算法、应点几人、如今记了几人。
     * 档号只在本类落笔；已记数是从底账当前轮在册名点出来的，不读卷面填的那个数。
     */
    private void recomputeView(TBcastStopBill bill, List<TBcastStopSign> signs) {
        int status = bill.getStatus() == null ? STATUS_RUNNING : bill.getStatus().intValue();
        int node = bill.getNodeNo() == null ? NODE_STATION : bill.getNodeNo().intValue();
        node = Math.max(NODE_STATION, Math.min(node, MAX_NODE));
        if (status == STATUS_RUNNING) {
            // 卷面档号只作上限：它前头若有一档从没点齐（绕档把档号拨前），
            // 出口把它压回头一个缺名的档，越档先落的那一笔不作数。
            node = effectiveRunningNode(signs, node);
        }
        bill.setNodeNo(Integer.valueOf(node));
        bill.setSignMode(Integer.valueOf(modeOf(node)));
        bill.setNeedCount(Integer.valueOf(needOf(node)));
        if (status == STATUS_RUNNING) {
            int[] roundHolder = currentRound(signs, node);
            bill.setSignCount(Integer.valueOf(roundHolder[1]));
        } else if (status == STATUS_SCHEDULED) {
            bill.setSignCount(Integer.valueOf(needOf(NODE_PROVINCE)));
        } else {
            int[] roundHolder = currentRound(signs, node);
            bill.setSignCount(Integer.valueOf(roundHolder[1]));
        }
    }

    /**
     * 在核单的实归档：卷面档号之前的每一档都得点得齐（有一轮在册名够数），
     * 头一个缺名的档才是单子真正卡住的地方——绕开市里直拨省局的，压回市里。
     * 退回后落回的前一档本就点齐、在窗口候放行，不在这句误伤之列。
     */
    private int effectiveRunningNode(List<TBcastStopSign> signs, int paperNode) {
        int node = Math.max(NODE_STATION, Math.min(paperNode, MAX_NODE));
        for (int n = NODE_STATION; n < node; n++) {
            if (winningSigns(signs, n).size() != needOf(n)) {
                return n;
            }
        }
        return node;
    }

    /**
     * 当前档正开着的是第几轮、在册几人。
     * 从没进过的档：头一轮、0人；最大一轮还有在册名：就在那一轮；
     * 最大一轮全沉底了（被退回清零过）：下回来开的是再下一轮。
     *
     * @return [轮次, 该轮在册名数]
     */
    private int[] currentRound(List<TBcastStopSign> signs, int node) {
        int maxRound = 0;
        for (TBcastStopSign s : signs) {
            if (s.getKind() != null && s.getKind().intValue() == KIND_SIGN
                    && s.getNodeNo() != null && s.getNodeNo().intValue() == node
                    && s.getRoundNo() != null && s.getRoundNo().intValue() > maxRound) {
                maxRound = s.getRoundNo().intValue();
            }
        }
        if (maxRound == 0) {
            return new int[] {1, 0};
        }
        int alive = countAlive(signs, node, maxRound);
        if (alive > 0) {
            return new int[] {maxRound, alive};
        }
        return new int[] {maxRound + 1, 0};
    }

    /** 这一档当前作数（点齐排定）的那一轮在册名：有在册名的最大一轮，按档内名次排 */
    private List<TBcastStopSign> winningSigns(List<TBcastStopSign> signs, int node) {
        int winRound = 0;
        for (TBcastStopSign s : signs) {
            if (s.getKind() != null && s.getKind().intValue() == KIND_SIGN
                    && s.getAliveFlag() != null && s.getAliveFlag().intValue() == ALIVE_YES
                    && s.getNodeNo() != null && s.getNodeNo().intValue() == node
                    && s.getRoundNo() != null && s.getRoundNo().intValue() > winRound) {
                winRound = s.getRoundNo().intValue();
            }
        }
        List<TBcastStopSign> out = new ArrayList<TBcastStopSign>();
        if (winRound == 0) {
            return out;
        }
        for (TBcastStopSign s : signs) {
            if (s.getKind() != null && s.getKind().intValue() == KIND_SIGN
                    && s.getAliveFlag() != null && s.getAliveFlag().intValue() == ALIVE_YES
                    && s.getNodeNo() != null && s.getNodeNo().intValue() == node
                    && s.getRoundNo() != null && s.getRoundNo().intValue() == winRound) {
                out.add(s);
            }
        }
        return out;
    }

    private int countAlive(List<TBcastStopSign> signs, int node, int round) {
        int n = 0;
        for (TBcastStopSign s : signs) {
            if (s.getKind() != null && s.getKind().intValue() == KIND_SIGN
                    && s.getAliveFlag() != null && s.getAliveFlag().intValue() == ALIVE_YES
                    && s.getNodeNo() != null && s.getNodeNo().intValue() == node
                    && s.getRoundNo() != null && s.getRoundNo().intValue() == round) {
                n++;
            }
        }
        return n;
    }

    private boolean hasSigner(List<TBcastStopSign> signs, int node, int round, String person) {
        for (TBcastStopSign s : signs) {
            if (s.getKind() != null && s.getKind().intValue() == KIND_SIGN
                    && s.getAliveFlag() != null && s.getAliveFlag().intValue() == ALIVE_YES
                    && s.getNodeNo() != null && s.getNodeNo().intValue() == node
                    && s.getRoundNo() != null && s.getRoundNo().intValue() == round
                    && eq(s.getSigner(), person)) {
                return true;
            }
        }
        return false;
    }

    private int countScheduleByNode(List<TBcastStopSchedule> schedule, int node) {
        int n = 0;
        for (TBcastStopSchedule s : schedule) {
            if (s.getNodeNo() != null && s.getNodeNo().intValue() == node) {
                n++;
            }
        }
        return n;
    }

    private TBcastStopSign newSign(TBcastStopBill bill, int node, int round, int seq,
                                   String person, String words, int kind, int alive) {
        TBcastStopSign s = new TBcastStopSign();
        s.setBillId(bill.getId());
        s.setBillNo(bill.getBillNo());
        s.setNodeNo(Integer.valueOf(node));
        s.setRoundNo(Integer.valueOf(round));
        s.setSeqNo(Integer.valueOf(seq));
        s.setSigner(person);
        s.setComment(words.isEmpty() ? null : words);
        s.setKind(Integer.valueOf(kind));
        s.setAliveFlag(Integer.valueOf(alive));
        s.setDelFlag(Integer.valueOf(0));
        return s;
    }

    private List<TBcastStopSign> loadSigns(Long billId) {
        List<TBcastStopSign> rows = this.bcastStopSignMapper.selectList(
                new QueryWrapper<TBcastStopSign>()
                        .eq("bill_id", billId).eq("del_flag", 0));
        List<TBcastStopSign> out = rows == null ? new ArrayList<TBcastStopSign>() : rows;
        sortSigns(out);
        return out;
    }

    private void sortSigns(List<TBcastStopSign> rows) {
        Collections.sort(rows, new Comparator<TBcastStopSign>() {
            @Override
            public int compare(TBcastStopSign a, TBcastStopSign b) {
                int c = Integer.compare(nz(a.getNodeNo()), nz(b.getNodeNo()));
                if (c != 0) {
                    return c;
                }
                c = Integer.compare(nz(a.getRoundNo()), nz(b.getRoundNo()));
                if (c != 0) {
                    return c;
                }
                c = Integer.compare(nz(a.getSeqNo()), nz(b.getSeqNo()));
                if (c != 0) {
                    return c;
                }
                return a.getId() == null || b.getId() == null
                        ? Integer.compare(System.identityHashCode(a), System.identityHashCode(b))
                        : a.getId().compareTo(b.getId());
            }
        });
    }

    private void appendRemark(TBcastStopBill bill, String line) {
        String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String more = stamp + " " + line;
        String old = bill.getRemark();
        bill.setRemark(old == null || old.trim().isEmpty() ? more : old + "\n" + more);
    }

    private static int needOf(int node) {
        return NEED_BY_NODE[node];
    }

    private static int modeOf(int node) {
        return needOf(node) == 1 ? MODE_OR : MODE_AND;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v.intValue();
    }

    private static boolean eq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
