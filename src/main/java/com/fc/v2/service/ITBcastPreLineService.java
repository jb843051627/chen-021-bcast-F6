package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TBcastPreLine;
import com.fc.v2.model.custom.BcastPreLineCheckResult;

/**
 * 事故定档门槛线 Service接口（rule-book 形状：查册、定位、立线，无删改）。
 *
 * <p>省局把说法改过三轮，每轮往册子里新添一条、旧的退居册底。册底不是销毁：
 * 翻旧册照翻得着，只是现行这一路只认当刻在场的那条。某一刻按哪一刻回看，
 * 当刻在场（立线之日已够着、让位之日还没到）的那条才作数——补算往期用往期
 * 自己那一刻，与眼下最新落的那条无关。</p>
 *
 * <p>同刻在场两条的，先比让位顺位（数值大的先说话）；顺位仍相同，再比代号，
 * 按排法落在末位（字典序最大）的那条胜出。一件事只许给一个答案。已经停用
 * （status=1）的不进现行判定：册子里躺着给人翻，屏上也翻得到，只是不许选它
 * 出来比数；但补算往期只认生效窗口——旧线在它自己当值那日仍算在场，旧卷
 * 结论才复算得出来。一条也套不上时不给结论，由调用方按"无可用线"留白处理。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITBcastPreLineService {

    /** 管哪一头：本段停播分钟线 */
    int KIND_MINUTES = 0;
    /** 管哪一头：被影响户数线 */
    int KIND_HOUSEHOLDS = 1;

    /** 按主键查册：停用的、退居册底的也翻得着（翻旧册照翻） */
    TBcastPreLine selectTBcastPreLineById(Long id);

    /**
     * 单规则求值：按 ruleCode 在 at 时刻在场的那一条判档（1..4）。
     * 老方法签名一动不动：进什么出什么，不加新参数。
     * 无在场版本 / 输入越界 / 参数缺失一律返回 0。
     */
    int evaluate(String ruleCode, BigDecimal input, Date at);

    /**
     * 多规则求值：at 时刻在场的分钟线中让位顺位最高者的档位；无在场分钟线返回 0。
     * 老签名不动，旧册这一路只摆分钟线，故固定走分钟头，口径与定位同一处。
     */
    int evaluateTop(BigDecimal input, Date at);

    /**
     * at 时刻在场名单（顺位降序、代号降序）。出处只有这一处：
     * 屏上那份在场名单与逐条走一遍定位得到的名单，是同一次点出来的。
     */
    List<TBcastPreLine> listAvailable(Date at);

    /**
     * 查线之道（另起的名字）：at 时刻在场的那一条——先比让位顺位，
     * 顺位相同再比代号、落在末位者胜；一条也套不上返回 null。
     * 名单与定位同源，同一件事换两个通道问回话必须一致。
     */
    TBcastPreLine resolveLineAt(Date at);

    /** 按代号查线之道：at 时刻该代号在场的那一条；不在场（含已停用、已让位）返回 null */
    TBcastPreLine resolveByCodeAt(String ruleCode, Date at);

    /**
     * 按头查线之道：at 时刻在场、且管这一头（0本段停播分钟线 / 1被影响户数线）
     * 的那一条；这一头一条也套不上返回 null。旧卷 line_kind 空着的按分钟线认。
     */
    TBcastPreLine resolveLineByKindAt(Date at, int lineKind);

    /** 单条线在 at 时刻在不在场（批量导入/报表等旁路复用，口径与定位/名单一致） */
    boolean usable(Long id, Date at);

    /** at 时刻在场线条数（列表页角标 / 看板），与在场名单同一回点出来 */
    int countAvailable(Date at);

    /** 翻册：在册的全部线，含已停用、退居册底的，按立线之日升序摆（老册照翻） */
    List<TBcastPreLine> listBook();

    /**
     * 立线：三个分界数少一个立不起来，没大过前头一栏的也立不起来。
     * 回执把空着的栏、没递增的栏一栏一条点出来，不回一句"数不对"了事。
     * 立得住的线落 status=0（在场）、立线之日起在场；让位之日留空的按一直在场认。
     */
    BcastPreLineCheckResult checkAndCreateLine(TBcastPreLine line);
}
