package com.fc.v2.controller.admin;

import java.util.Date;
import java.util.List;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TBcastPreLine;
import com.fc.v2.model.custom.BcastPreLineCheckResult;
import com.fc.v2.service.ITBcastPreLineService;
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
 * 事故定档门槛线 Controller（rule-book 形状）。
 *
 * <p>屏上不自己判在场、不自己排位次，只把手递给服务层：翻册名单、在场名单、
 * 当场定位、立线回执全出自服务层同一处，两回数出来不一样就算错。本层不落
 * 任何定档结论——定档那四问另有一个出口。</p>
 *
 * @author fuce
 * @date 2026-10-04
 */
@Api(value = "事故定档门槛线")
@Controller
@RequestMapping("/bcastPreLine")
public class BcastPreLineController extends BaseController {

    @Autowired
    private ITBcastPreLineService bcastPreLineService;

    @Log(title = "门槛线翻册（含停用线）", action = "book")
    @ApiOperation(value = "翻册", notes = "在册全部线，含已停用、退居册底的，老册照翻")
    @GetMapping("/book")
    @RequiresPermissions("bcastPreLine:book")
    @ResponseBody
    public AjaxResult book() {
        List<TBcastPreLine> rows = bcastPreLineService.listBook();
        return AjaxResult.successData(200, rows);
    }

    @Log(title = "门槛线在场名单", action = "available")
    @ApiOperation(value = "在场名单", notes = "at 时刻在场的线（顺位降序、代号降序）；与逐条走查同一出处")
    @GetMapping("/available")
    @RequiresPermissions("bcastPreLine:available")
    @ResponseBody
    public AjaxResult available(Date at) {
        List<TBcastPreLine> rows = bcastPreLineService.listAvailable(at);
        return AjaxResult.successData(200, rows);
    }

    @Log(title = "门槛线当场定位", action = "resolve")
    @ApiOperation(value = "当场定位算数的线", notes = "先比让位顺位，再比代号；一条也套不上回 null（无可用线）")
    @GetMapping("/resolve")
    @RequiresPermissions("bcastPreLine:resolve")
    @ResponseBody
    public AjaxResult resolve(Date at, Integer lineKind) {
        if (at == null) {
            return AjaxResult.error("先讲按哪一刻回看");
        }
        int kind = lineKind == null ? ITBcastPreLineService.KIND_MINUTES : lineKind.intValue();
        TBcastPreLine line = bcastPreLineService.resolveLineByKindAt(at, kind);
        return AjaxResult.successData(200, line);
    }

    @Log(title = "门槛线在场条数", action = "count")
    @ApiOperation(value = "在场条数", notes = "与在场名单同一回点出来")
    @GetMapping("/count")
    @RequiresPermissions("bcastPreLine:count")
    @ResponseBody
    public AjaxResult count(Date at) {
        return AjaxResult.successData(200, Integer.valueOf(bcastPreLineService.countAvailable(at)));
    }

    @Log(title = "门槛线立线", action = "create")
    @ApiOperation(value = "立线", notes = "三档缺一、没逐档增大立不起来；回执逐栏点出错处")
    @PostMapping("/create")
    @RequiresPermissions("bcastPreLine:create")
    @ResponseBody
    public AjaxResult create(TBcastPreLine line) {
        BcastPreLineCheckResult receipt = bcastPreLineService.checkAndCreateLine(line);
        if (!receipt.isSuccess()) {
            // 光回一句"数不对"不算交代：错处一栏一条全摆上屏
            return AjaxResult.error(org.apache.commons.lang3.StringUtils.join(receipt.getErrors(), "；"));
        }
        return AjaxResult.successData(200, receipt.getLine());
    }
}
