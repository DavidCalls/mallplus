package com.zscat.mallplus.oms.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zscat.mallplus.annotation.SysLog;
import com.zscat.mallplus.oms.entity.OmsOrderSetting;
import com.zscat.mallplus.oms.service.IOmsOrderSettingService;
import com.zscat.mallplus.utils.CommonResult;
import com.zscat.mallplus.utils.ValidatorUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 订单设置表
 * </p>
 *
 * @author zscat
 * @since 2019-04-19
 */
@Slf4j
@RestController
@Api(tags = "oms", description = "订单设置表管理")
@RequestMapping("/oms/OmsOrderSetting")
public class OmsOrderSettingController {
    @Resource
    private IOmsOrderSettingService IOmsOrderSettingService;

    @SysLog(MODULE = "oms", REMARK = "根据条件查询所有订单设置表列表")
    @ApiOperation("根据条件查询所有订单设置表列表")
    @GetMapping(value = "/list")
    @PreAuthorize("hasAuthority('oms:OmsOrderSetting:read')")
    public Object getOmsOrderSettingByPage(OmsOrderSetting entity,
                                           @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                           @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        try {
            return new CommonResult().success(IOmsOrderSettingService.page(new Page<OmsOrderSetting>(pageNum, pageSize), new QueryWrapper<>(entity)));
        } catch (Exception e) {
            log.error("根据条件查询所有订单设置表列表：%s", e.getMessage(), e);
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "oms", REMARK = "保存订单设置表")
    @ApiOperation("保存订单设置表")
    @PostMapping(value = "/create")
    public Object saveOmsOrderSetting(@RequestBody OmsOrderSetting entity) {
        try {
            if (IOmsOrderSettingService.save(entity)) {
                return new CommonResult().success();
            }
        } catch (Exception e) {
            log.error("保存订单设置表：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "oms", REMARK = "更新订单设置表")
    @ApiOperation("更新订单设置表")
    @PostMapping(value = "/update/{id}")
    public Object updateOmsOrderSetting(@RequestBody OmsOrderSetting entity) {
        try {
            entity.setId(1L);
            if (IOmsOrderSettingService.update(entity, new QueryWrapper<OmsOrderSetting>())) {
                return new CommonResult().success();
            }
        } catch (Exception e) {
            log.error("更新订单设置表：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "oms", REMARK = "删除订单设置表")
    @ApiOperation("删除订单设置表")
    @GetMapping(value = "/delete/{id}")
    public Object deleteOmsOrderSetting(@ApiParam("订单设置表id") @PathVariable Long id) {
        try {
            if (ValidatorUtils.empty(id)) {
                return new CommonResult().paramFailed("订单设置表id");
            }
            if (IOmsOrderSettingService.removeById(id)) {
                return new CommonResult().success();
            }
        } catch (Exception e) {
            log.error("删除订单设置表：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "oms", REMARK = "给订单设置表分配订单设置表")
    @ApiOperation("查询订单设置表明细")
    @GetMapping(value = "/{id}")
    public Object getOmsOrderSettingById(@ApiParam("订单设置表id") @PathVariable Long id) {
        try {

            OmsOrderSetting coupon = IOmsOrderSettingService.getOne(new QueryWrapper<>());
            if (coupon == null) {
                coupon = new OmsOrderSetting();
                coupon.setId(1L);
                IOmsOrderSettingService.save(coupon);
            }
            return new CommonResult().success(coupon);
        } catch (Exception e) {
            log.error("查询订单设置表明细：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }

    }

    @ApiOperation(value = "批量删除订单设置表")
    @RequestMapping(value = "/delete/batch", method = RequestMethod.GET)
    @ResponseBody
    @SysLog(MODULE = "pms", REMARK = "批量删除订单设置表")
    public Object deleteBatch(@RequestParam("ids") List<Long> ids) {
        boolean count = IOmsOrderSettingService.removeByIds(ids);
        if (count) {
            return new CommonResult().success(count);
        } else {
            return new CommonResult().failed();
        }
    }

}
