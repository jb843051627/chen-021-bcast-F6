package com.fc.v2.controller.admin;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TBcastCaseGrade;
import com.fc.v2.model.custom.BcastGradeHistoryAudit;
import com.fc.v2.model.custom.BcastGradeVerdict;
import com.fc.v2.service.ITBcastCaseGradeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 停播事故定档 Controller（single-exit 形状）。
 *
 * <p>四问只认服务层 {@code gradeAndPersist} 一个出口，本层只转手：屏上不先落
 * 答案再去补依据，也没有第二处能给结论的门。线代号、立线之日随回话入库。</p>
 *
 * @author fuce
 * @date 2026-10-04
 */
@Api(value = "停播事故定档")
@Controller
@RequestMapping("/bcastCaseGrade")
public class BcastCaseGradeController extends BaseController {

    @Autowired
    private ITBcastCaseGradeService bcastCaseGradeService;

    @Log(title = "停播事故定档", action = "grade")
    @ApiOperation(value = "定档（唯一出口）",
            notes = "按 occAt 那一刻回看；两头各自比线，越头摆那头的数；无可用线四栏留白；结论钉死线代号与立线之日")
    @PostMapping("/grade")
    @RequiresPermissions("bcastCaseGrade:grade")
    @ResponseBody
    public AjaxResult grade(String bizNo, Date occAt, BigDecimal stopMinutes, BigDecimal affectedQty) {
        if (bizNo == null || bizNo.trim().isEmpty() || occAt == null) {
            return AjaxResult.error("先讲是哪一起事故、按哪一刻回看");
        }
        // 回话全出自这一趟出口，屏上不自作主张
        BcastGradeVerdict verdict = bcastCaseGradeService.gradeAndPersist(
                bizNo, occAt, stopMinutes, affectedQty);
        return AjaxResult.successData(200, verdict);
    }

    @Log(title = "定档台账", action = "list")
    @ApiOperation(value = "定档台账", notes = "落库的一笔笔结论（含无可用线、不归档），只插不改")
    @GetMapping("/list")
    @RequiresPermissions("bcastCaseGrade:list")
    @ResponseBody
    public AjaxResult list() {
        List<TBcastCaseGrade> rows = bcastCaseGradeService.listGrades();
        return AjaxResult.successData(200, rows);
    }

    @Log(title = "历不对核账", action = "audit")
    @ApiOperation(value = "历不对核账",
            notes = "屏上挂着代号、册子那一日却找不出在场线的旧结论，一笔一条摆出来")
    @GetMapping("/audit")
    @RequiresPermissions("bcastCaseGrade:audit")
    @ResponseBody
    public AjaxResult audit() {
        BcastGradeHistoryAudit audit = bcastCaseGradeService.auditHistory();
        return AjaxResult.successData(200, audit);
    }
}
