package com.fc.v2.service;

import java.util.List;

import com.fc.v2.model.auto.TBcastStopBill;
import com.fc.v2.model.auto.TBcastStopSchedule;
import com.fc.v2.model.auto.TBcastStopSign;
import com.fc.v2.model.custom.BcastStopScheduleAudit;

/**
 * 停播检修核准单 Service接口（approval-chain 形状：三级签批，一条链走到底）
 *
 * <p>三道关口按次序走：第一道台站（一人，头一名落下便过）、第二道市广电局（两人，
 * 缺一人在窗口里仍挂候点）、第三道省局（两人）。这张单子走到哪一档、下一档收不收它、
 * 省局点齐之后还添不添得进人、越档先落的那一笔作不作数、退一档时本档这一轮的名清不清——
 * 五样结论一律出自本接口那一个落名出口 {@link #land}，屏上不留第二处能把档号直接改定的
 * 把手。接口递上来的档号只当意图：与出口数出来的不合，整笔不作数。</p>
 *
 * <p>"已记几人"不由谁拿手往格子里填：系统顺着这张单的落名底账一笔一笔点出来；
 * 屏上那个数只当展示，底账条数与档口应点人数持平才算数。退一档只退一格，本档当轮的名
 * 当场清零（沉底留行、不抹掉），早先各档旧名一笔不动；重走另起一轮，新旧两拨分开记。
 * 省局点齐，单子排定，排期名册在同一次算里从底账誊成——一处取数、一处誊写，不许手填。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITBcastStopBillService {

    /** 落名出口的动作：落名（在当前档点一笔） */
    int LAND_SIGN = 0;
    /** 落名出口的动作：退回（把整张单退回紧挨着的上一档） */
    int LAND_RETURN = 1;
    /** 落名出口的动作：否决（全单就此了断） */
    int LAND_VETO = 2;
    /** 落名出口的动作：提交放行（退回后从上一档重新提交，旧名已齐的不叫人再点一遍） */
    int LAND_RESUBMIT = 3;

    /**
     * 按主键回查单据（签名照旧：进什么样出什么样，不塞新参）。
     * 档号、应点/已记几人以落名出口顺着底账数出来的为准：卷面回读与回话不合，按回话那一份走。
     */
    TBcastStopBill selectTBcastStopBillById(Long id);

    /**
     * 立一张核准单。单号之外的档号、算法、已记数、情形一律由系统落笔：
     * 从台站那一道、在核起，人递进来的档号与已记数只算意图，不入库。
     *
     * @return 1 立成；0 立不起来（单为空或单号空着）
     */
    int fileTBcastStopBill(TBcastStopBill record);

    /**
     * 落名出口——往前、退回、否决三桩走的都是这一个口子。
     *
     * <p>{@code intentNodeNo} 是屏上递上来的档号，只算意图：与系统顺着底账数出来的当前档
     * 不一致的，整笔不作数（越档先落、绕开市里直落省局，一律返回 null，单子仍卡在原档）。
     * 落名只认当前档当前轮、同一落名人不重复点；该档缺名未齐时单子在窗口挂候点，点齐才
     * 挪到紧挨着的下一档。已排定、已否决的单，任何动作都不作数。</p>
     *
     * @param id           核准单主键
     * @param intentNodeNo 递上来的档号（意图，不作定档依据）
     * @param action       动作：{@link #LAND_SIGN}/{@link #LAND_RETURN}/{@link #LAND_VETO}/{@link #LAND_RESUBMIT}
     * @param signer       落名/退回/否决人（落名时不能为空）
     * @param comment      附言（可空）
     * @return 出口回话：更新后的单据；被拒返回 null
     */
    TBcastStopBill land(Long id, Integer intentNodeNo, int action, String signer, String comment);

    /**
     * 排期名单：省局点齐那一瞬从落名底账同一次誊成的那一份，按档口、档内名次排。
     * 未排定的单没有名册，返回空表。
     */
    List<TBcastStopSchedule> listSchedule(Long id);

    /**
     * 事后翻查对账：自省局往回收数到台站，一块一块清点。每档一块，块上带着应点几人、
     * 底账当前轮几笔、名册几笔与逐笔对照；笔笔对得上才算平，任取一块单看也经得起复核。
     * 单子未排定时返回 null（没有排期，无从对账）。
     */
    BcastStopScheduleAudit auditSchedule(Long id);

    /**
     * 翻这张单的落名底账：在册的、沉底的旧轮、否决的那笔都在里头，按落名先后排。
     */
    List<TBcastStopSign> listSigns(Long id);
}
