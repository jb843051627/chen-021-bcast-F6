package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TBcastPreLine;
import com.fc.v2.model.custom.BcastGradeAnomaly;
import com.fc.v2.model.custom.BcastGradeVerdict;
import com.fc.v2.model.custom.BcastLineCheck;
import com.fc.v2.model.custom.BcastLinePick;

/**
 * 事故定档门槛线 Service接口（rule-eval 形状：规则求值，无增删改）。
 *
 * <p>四问的回话只出本服务这一个出口：一起事故在某一刻按哪条线归层（{@link #locateLine}）、
 * 当刻一条线也够不着时给不给说法（回话落"无可用线"，代号与立线之日两边留白）、
 * 同顺位几条线听谁的（顺位高者，仍同则代号排在末位者）、两头线合到一处取哪一头
 * （{@link #grade}，两头各自不越封顶才归层，取更外那头）。屏上不许先落答案再补依据。</p>
 *
 * <p>在场口径全服务只此一处：立线之日够着（≤回看时刻）、让位之日还没到
 * （让位之日不含；留白按一直在场认，兼容旧卷存量）、未停用、未删，再按让位顺位
 * 降序、代号降序取头一条。补算往期时回看时刻就是往期自己那一刻，与眼下最新落的
 * 那条无关。已停用的线退居册底：{@link #listLedger()} 翻得到，但不进在场名单，
 * 也不参与比数，更不跟在新结论后面。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITBcastPreLineService {

    /** 线管哪一头：本段停播分钟数 */
    int KIND_MINUTE = 1;

    /** 线管哪一头：被影响到的户数 */
    int KIND_HOUSE = 2;

    /** 按主键查线（册子里躺着的也查得到，停用不等于销毁） */
    TBcastPreLine selectTBcastPreLineById(Long id);

    /**
     * 单规则求值：按 ruleCode 在 at 时刻在场的版本，把分钟数落进 1..4 档
     * （1未过起算…3未过封顶，4高过封顶）。当刻该代号没有在场版本、分界数立不住、
     * 参数缺失或为负，一律返回 0。
     *
     * <p>老方法签名一动不动：进什么出什么，不加新参数。要按"哪一头"查线，
     * 另请走 {@link #locateLine}。</p>
     */
    int evaluate(String ruleCode, BigDecimal input, Date at);

    /**
     * 多规则求值：at 时刻分钟那头在场线中让位顺位最高者的档位（1..4）；
     * 无在场线、分界数立不住或输入为负返回 0。
     */
    int evaluateTop(BigDecimal input, Date at);

    /** at 时刻在场名单（分钟、户数两头合在一处，顺位降序、代号降序） */
    List<TBcastPreLine> listAvailable(Date at);

    /** 单条线在 at 时刻在不在场（口径与定位/名单同一处，不许旁路另算） */
    boolean usable(Long id, Date at);

    /** at 时刻在场线条数（两头合计；角标/看板用） */
    int countAvailable(Date at);

    // ================= 新查线之道：另起的名字，另接的回话 =================

    /**
     * 定位出口：at 时刻某一头在场、算数的那一条。一条也套不上时不给结论——
     * 回话写明"无可用线"，线代号、立线之日两边一律留白，不借旁边一条线的数糊答复。
     *
     * @param at       回看时刻（补算往期就递往期自己那一刻）
     * @param lineKind {@link #KIND_MINUTE} 或 {@link #KIND_HOUSE}
     */
    BcastLinePick locateLine(Date at, int lineKind);

    /**
     * 立线回执：起算、过半、封顶三个分界数少一个就立不起来；空着哪一栏、
     * 哪一栏没大过前头那一栏（两栏各是多少），回执上一条一条直接点出来。
     */
    BcastLineCheck checkLine(TBcastPreLine line);

    /**
     * 屏上那份在场名单：只摆 at 时刻在场的线。与逐条走一遍得到的名单出处同一处
     * （同一个在场口径），两回数出来不一样就算错。已停用的线不摆。
     */
    List<TBcastPreLine> listOnScreen(Date at);

    /**
     * 翻册子：未删的线一条不漏——到期让位的、整条停用的都在里头，按立线之日降序。
     * 只供翻看，册底的线不许从这里选去比数。
     */
    List<TBcastPreLine> listLedger();

    /**
     * 归层出口（四问之四）：一起事故，分钟数按分钟线看、户数按户数线看，
     * 两头各自都不越自己那条线的封顶才算归层，层档取两头落档里更外的那一头。
     * 哪一头无在场线、或哪一头越了封顶，结论留白、缘由写明，越限把"实际数/封顶数"
     * 摆出来，不只回一句"不够"。
     *
     * <p>每走一趟就落一笔：归层落定、这一趟没够上各是各的一笔，落库时把两头的
     * 线代号与立线之日一并钉死，日后线册再怎么换也不回头改这一笔。</p>
     *
     * @param bizNo            事故处置单代号
     * @param occurAt          事故回看那一刻
     * @param stopMinutes      本段停播分钟数
     * @param affectHouseholds 被影响到的户数
     * @return 归层回话（含落库那一笔的主键）
     */
    BcastGradeVerdict grade(String bizNo, Date occurAt, BigDecimal stopMinutes, BigDecimal affectHouseholds);

    /**
     * 历不对清点：把屏上还挂着线代号、可册子翻回事故那一日那一头却找不出在场线
     * （或当刻在场的已是另一条）的落定结论，一条一条摆出来。复算一律用结论自己
     * 那笔事故时刻，与眼下最新落的线无关。
     */
    List<BcastGradeAnomaly> listGradeAnomalies();
}
