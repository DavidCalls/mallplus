package com.zscat.mallplus.sys.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zscat.mallplus.annotation.SysLog;
import com.zscat.mallplus.sys.entity.AdminSysJob;
import com.zscat.mallplus.sys.service.IAdminSysJobService;
import com.zscat.mallplus.utils.CommonResult;
import com.zscat.mallplus.utils.ValidatorUtils;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 定时任务调度表 前端控制器
 * </p>
 *
 * @author zscat
 * @since 2019-11-26
 */
@Slf4j
@RestController
@RequestMapping("/sys/adminSysJob")
public class AdminSysJobController {

    @Resource
    private IAdminSysJobService adminSysJobService;

    @SysLog(MODULE = "cms", REMARK = "根据条件查询所有定时器日志表列表")
    @ApiOperation("根据条件查询所有定时器日志表列表")
    @GetMapping(value = "/list")

    public Object getAdminSysJobByPage(AdminSysJob entity,
                                       @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        try {
            return new CommonResult().success(adminSysJobService.page(new Page<AdminSysJob>(pageNum, pageSize), new QueryWrapper<>(entity)));
        } catch (Exception e) {
            log.error("根据条件查询所有定时器日志表列表：%s", e.getMessage(), e);
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "cms", REMARK = "保存定时器日志表")
    @ApiOperation("保存定时器日志表")
    @PostMapping(value = "/create")

    public Object saveAdminSysJob(@RequestBody AdminSysJob entity) {
        try {
            if (!adminSysJobService.checkCronExpressionIsValid(entity.getCronExpression())) {
                return new CommonResult().failed("表达式错误");
            }
            adminSysJobService.insertJob(entity);
            return new CommonResult().success();

        } catch (Exception e) {
            log.error("保存定时器日志表：%s", e.getMessage(), e);
            return new CommonResult().failed("表达式错误");
        }

    }

    @SysLog(MODULE = "cms", REMARK = "更新定时器日志表")
    @ApiOperation("更新定时器日志表")
    @PostMapping(value = "/update/{id}")

    public Object updateAdminSysJob(@RequestBody AdminSysJob entity) {
        try {
            if (!adminSysJobService.checkCronExpressionIsValid(entity.getCronExpression())) {
                return new CommonResult().failed("表达式错误");
            }
            adminSysJobService.updateJob(entity);
            return new CommonResult().success();

        } catch (Exception e) {
            log.error("更新定时器日志表：%s", e.getMessage(), e);
            return new CommonResult().failed("表达式错误");
        }

    }

    @SysLog(MODULE = "cms", REMARK = "删除定时器日志表")
    @ApiOperation("删除定时器日志表")
    @GetMapping(value = "/delete/{id}")

    public Object deleteAdminSysJob(@ApiParam("定时器日志表id") @PathVariable Long id) {
        try {
            if (ValidatorUtils.empty(id)) {
                return new CommonResult().paramFailed("定时器日志表id");
            }
            if (adminSysJobService.delete(id)) {
                return new CommonResult().success();
            }
        } catch (Exception e) {
            log.error("删除定时器日志表：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "cms", REMARK = "给定时器日志表分配定时器日志表")
    @ApiOperation("查询定时器日志表明细")
    @GetMapping(value = "/{id}")

    public Object getAdminSysJobById(@ApiParam("定时器日志表id") @PathVariable Long id) {
        try {
            if (ValidatorUtils.empty(id)) {
                return new CommonResult().paramFailed("定时器日志表id");
            }
            AdminSysJob coupon = adminSysJobService.getById(id);
            return new CommonResult().success(coupon);
        } catch (Exception e) {
            log.error("查询定时器日志表明细：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }

    }

    @ApiOperation(value = "批量删除定时器日志表")
    @RequestMapping(value = "/delete/batch", method = RequestMethod.GET)
    @ResponseBody
    @SysLog(MODULE = "pms", REMARK = "批量删除定时器日志表")

    public Object deleteBatch(@RequestParam("ids") List<Long> ids) {
        boolean count = adminSysJobService.deleteByIds(ids);
        if (count) {
            return new CommonResult().success(count);
        } else {
            return new CommonResult().failed();
        }
    }

    @ApiOperation("修改展示状态")
    @RequestMapping(value = "/changeStatus")
    @ResponseBody
    @SysLog(MODULE = "cms", REMARK = "修改展示状态")
    public Object changeStatus(@RequestBody AdminSysJob entity) {
        int count = adminSysJobService.changeStatus(entity);
        if (count > 0) {
            return new CommonResult().success(count);
        } else {
            return new CommonResult().failed();
        }
    }

    @ApiOperation("修改展示状态")
    @RequestMapping(value = "/run")
    @ResponseBody
    @SysLog(MODULE = "cms", REMARK = "修改展示状态")
    public Object run(@RequestBody AdminSysJob entity) {
        adminSysJobService.run(entity);
        return new CommonResult().success();

    }

    /**
     * 校验cron表达式是否有效
     */
    @PostMapping("/checkCronExpressionIsValid")
    @ResponseBody
    public Object checkCronExpressionIsValid(@RequestBody AdminSysJob job) {
        adminSysJobService.checkCronExpressionIsValid(job.getCronExpression());
        return new CommonResult().success();
    }
}


