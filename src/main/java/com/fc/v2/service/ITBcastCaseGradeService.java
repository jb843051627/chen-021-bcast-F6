package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TBcastCaseGrade;
import com.fc.v2.model.custom.BcastGradeHistoryAudit;
import com.fc.v2.model.custom.BcastGradeVerdict;

/**
 * 停播事故定档 Service接口（single-exit 形状：四问只出一个出口）。
 *
 * <p>一起事故在某一刻按哪条线归层、当刻一条线也够不着时给不给说法、同顺位
 * 几条线听谁的、两头线合到一处取哪一头——这四问的回话只出自
 * {@link #gradeAndPersist} 这一个出口，此外不开第二处能给结论的门；
 * 屏上不许先落一个答案再去补依据。随结论一并入库的线代号、立线之日，
 * 都按这一趟出口回的话来写。</p>
 *
 * <p>本段停播分钟数走分钟线、被影响户数走户数线，两头各自不越封顶才算归层；
 * 合到一处取两头里落得高的那一层。任一头没线、没报数，结论栏写"无可用线"，
 * 代号、立线之日四栏一律留白。结论落库时把两头线代号与立线之日钉死，只插
 * 不改：没够上与后来补上是两笔，各留各的。</p>
 *
 * @author fuce
 * @date 2026-10-04
 */
public interface ITBcastCaseGradeService {

    /**
     * 定档唯一出口。按 occAt 那一刻查册（补算往期用往期自己那一刻，与眼下
     * 最新那条无关）：两头各自定位当刻在场的线、各自比三个分界数；回话落库
     * 一笔，线代号与立线之日随这一笔钉死。每问一次落一笔，旧笔不回头改。
     */
    BcastGradeVerdict gradeAndPersist(String bizNo, Date occAt, BigDecimal stopMinutes, BigDecimal affectedQty);

    /** 定档台账：落库的一笔笔结论（含无可用线、不归档的），按回看时刻先后摆 */
    List<TBcastCaseGrade> listGrades();

    /**
     * 历不对核账：把屏上还挂着线代号、册子在结论那一日却找不出在场同代号
     * （或钉的立线之日对不上）的笔，一笔一条摆出来。钉死的旧笔不回头改。
     */
    BcastGradeHistoryAudit auditHistory();
}
