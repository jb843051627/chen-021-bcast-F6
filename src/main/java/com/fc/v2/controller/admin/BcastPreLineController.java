package com.fc.v2.controller.admin;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TBcastPreLine;
import com.fc.v2.model.custom.BcastGradeVerdict;
import com.fc.v2.service.ITBcastPreLineService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 事故定档门槛线 Controller（rule-eval 形状：查线、定档都只听服务层那一个出口）。
 *
 * <p>屏上不许先落一个答案再去补依据：在场名单、定位、立线回执、归层、历不对清点，
 * 每一个口都只把参数递到服务层、把出口的回话原样带回，屏上不自作主张，也不留第二处
 * 能给结论的门。随结论一并入库的线代号、立线之日，都按出口这一趟回的话来写。</p>
 *
 * @author fuce
 * @date 2026-10-04
 */
@Api(value = "事故定档门槛线")
@Controller
@RequestMapping("/bcastPreLine")
public class BcastPreLineController extends BaseController {

    private final String prefix = "admin/bcastPreLine";

    @Autowired
    private ITBcastPreLineService bcastPreLineService;

    @ApiOperation(value = "门槛线台账跳转", notes = "门槛线台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("bcastPreLine:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @ApiOperation(value = "屏上在场名单", notes = "与逐条走一遍得到的名单出处同一处；停用线不摆")
    @GetMapping("/onScreen")
    @RequiresPermissions("bcastPreLine:view")
    @ResponseBody
    public AjaxResult onScreen(@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date at) {
        return AjaxResult.successData(200, bcastPreLineService.listOnScreen(at));
    }

    @ApiOperation(value = "翻册子", notes = "到期让位的、整条停用的都翻得到，只供翻看")
    @GetMapping("/archive")
    @RequiresPermissions("bcastPreLine:view")
    @ResponseBody
    public AjaxResult archive() {
        return AjaxResult.successData(200, bcastPreLineService.listLedger());
    }

    @ApiOperation(value = "定位出口", notes = "某一刻某一头按哪条线算数；套不上落无可用线，代号与立线之日留白")
    @GetMapping("/locate")
    @RequiresPermissions("bcastPreLine:view")
    @ResponseBody
    public AjaxResult locate(@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date at, Integer lineKind) {
        if (at == null || lineKind == null) {
            return AjaxResult.error("回看时刻与哪一头（1停播分钟 2影响户数）都得报齐");
        }
        return AjaxResult.successData(200, bcastPreLineService.locateLine(at, lineKind));
    }

    @ApiOperation(value = "立线回执", notes = "空着哪一栏、哪一栏没大过前头那一栏，逐条点出来")
    @PostMapping("/check")
    @RequiresPermissions("bcastPreLine:view")
    @ResponseBody
    public AjaxResult check(TBcastPreLine line) {
        return AjaxResult.successData(200, bcastPreLineService.checkLine(line));
    }

    @Log(title = "事故归层定档", action = "grade")
    @ApiOperation(value = "归层出口", notes = "两头各自不越封顶才归层；每趟落一笔并钉死两头线代号与立线之日")
    @PostMapping("/grade")
    @RequiresPermissions("bcastPreLine:view")
    @ResponseBody
    public AjaxResult grade(String bizNo,
                            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date occurAt,
                            BigDecimal stopMinutes, BigDecimal affectHouseholds) {
        BcastGradeVerdict verdict =
                bcastPreLineService.grade(bizNo, occurAt, stopMinutes, affectHouseholds);
        if (verdict == null) {
            return AjaxResult.error("事故代号与回看那一刻都得报齐，无从查线");
        }
        return AjaxResult.successData(200, verdict);
    }

    @ApiOperation(value = "历不对清点", notes = "屏上还挂着代号、事故当日那一头却找不出在场线的，一条一条摆出来")
    @GetMapping("/anomalies")
    @RequiresPermissions("bcastPreLine:view")
    @ResponseBody
    public AjaxResult anomalies() {
        return AjaxResult.successData(200, bcastPreLineService.listGradeAnomalies());
    }
}
