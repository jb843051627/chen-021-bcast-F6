package com.fc.v2.controller.admin;

import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TBcastStopBill;
import com.fc.v2.model.custom.BcastStopScheduleAudit;
import com.fc.v2.service.ITBcastStopBillService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 停播检修核准单 Controller（approval-chain 形状：三级签批，一个落名出口）。
 *
 * <p>屏上只有这一处把手：落名、退回、否决、退回后放行都递到服务层那一个落名出口
 * {@link ITBcastStopBillService#land}。单子走到哪一档、下一档收不收，全听出口回话；
 * 递上来的档号只当意图，屏上不留第二处能把档号直接改定的把手（不开 approve/reject/
 * rollback 三个各管一段的口子）。</p>
 *
 * @author fuce
 * @date 2026-10-03
 */
@Api(value = "停播检修核准单")
@Controller
@RequestMapping("/bcastStopBill")
public class BcastStopBillController extends BaseController {

    private final String prefix = "admin/bcastStopBill";

    @Autowired
    private ITBcastStopBillService bcastStopBillService;

    @ApiOperation(value = "核准台账跳转", notes = "核准台账跳转")
    @GetMapping("/view")
    @RequiresPermissions("bcastStopBill:view")
    public String view(ModelMap model) {
        return prefix + "/list";
    }

    @ApiOperation(value = "核准单回查", notes = "档号、应点/已记几人以出口顺着底账数出来的为准")
    @GetMapping("/detail")
    @RequiresPermissions("bcastStopBill:view")
    @ResponseBody
    public AjaxResult detail(Long id) {
        TBcastStopBill bill = bcastStopBillService.selectTBcastStopBillById(id);
        return bill == null ? AjaxResult.error("查无此单") : AjaxResult.successData(200, bill);
    }

    @Log(title = "停播检修核准单落名", action = "land")
    @ApiOperation(value = "落名出口", notes = "落名/退回/否决/放行都走这一个口子；递上来的档号只当意图")
    @PostMapping("/land")
    @RequiresPermissions("bcastStopBill:land")
    @ResponseBody
    public AjaxResult land(Long id, Integer intentNodeNo, Integer action, String signer, String comment) {
        int act = action == null ? ITBcastStopBillService.LAND_SIGN : action.intValue();
        TBcastStopBill bill = bcastStopBillService.land(id, intentNodeNo, act, signer, comment);
        // 屏上带出的档号与回话不合，按回话那一份走：把出口回话整张带回
        return bill == null ? AjaxResult.error("这一笔不作数") : AjaxResult.successData(200, bill);
    }

    @ApiOperation(value = "检修排期名单", notes = "省局点齐那一瞬同一次誊成的那一份")
    @GetMapping("/schedule")
    @RequiresPermissions("bcastStopBill:view")
    @ResponseBody
    public AjaxResult schedule(Long id) {
        return AjaxResult.successData(200, bcastStopBillService.listSchedule(id));
    }

    @ApiOperation(value = "排期对账", notes = "自省局往回收数到台站，一块一块清点")
    @GetMapping("/audit")
    @RequiresPermissions("bcastStopBill:view")
    @ResponseBody
    public AjaxResult audit(Long id) {
        BcastStopScheduleAudit audit = bcastStopBillService.auditSchedule(id);
        return audit == null ? AjaxResult.error("单子尚未排定，无从对账") : AjaxResult.successData(200, audit);
    }

    @ApiOperation(value = "落名底账", notes = "在册、沉底旧轮、否决那笔都在里头")
    @GetMapping("/signs")
    @RequiresPermissions("bcastStopBill:view")
    @ResponseBody
    public AjaxResult signs(Long id) {
        return AjaxResult.successData(200, bcastStopBillService.listSigns(id));
    }
}
