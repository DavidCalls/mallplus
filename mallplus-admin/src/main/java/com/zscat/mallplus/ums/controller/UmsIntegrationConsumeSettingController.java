package com.zscat.mallplus.ums.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zscat.mallplus.annotation.SysLog;
import com.zscat.mallplus.ums.entity.UmsIntegrationConsumeSetting;
import com.zscat.mallplus.ums.service.IUmsIntegrationConsumeSettingService;
import com.zscat.mallplus.utils.BeanUtil;
import com.zscat.mallplus.utils.CommonResult;
import com.zscat.mallplus.utils.ValidatorUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 积分消费设置
 * </p>
 *
 * @author zscat
 * @since 2019-04-19
 */
@Slf4j
@RestController
@Api(tags = "UmsIntegrationConsumeSettingController", description = "积分消费设置管理")
@RequestMapping("/ums/UmsIntegrationConsumeSetting")
public class UmsIntegrationConsumeSettingController {
    @Resource
    private IUmsIntegrationConsumeSettingService IUmsIntegrationConsumeSettingService;

    @SysLog(MODULE = "ums", REMARK = "根据条件查询所有积分消费设置列表")
    @ApiOperation("根据条件查询所有积分消费设置列表")
    @GetMapping(value = "/list")
    @PreAuthorize("hasAuthority('ums:UmsIntegrationConsumeSetting:read')")
    public Object getUmsIntegrationConsumeSettingByPage(UmsIntegrationConsumeSetting entity,
                                                        @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                        @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        try {
            return new CommonResult().success(IUmsIntegrationConsumeSettingService.page(new Page<UmsIntegrationConsumeSetting>(pageNum, pageSize), new QueryWrapper<>(entity)));
        } catch (Exception e) {
            log.error("根据条件查询所有积分消费设置列表：%s", e.getMessage(), e);
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "ums", REMARK = "保存积分消费设置")
    @ApiOperation("保存积分消费设置")
    @PostMapping(value = "/create")
    @PreAuthorize("hasAuthority('ums:UmsIntegrationConsumeSetting:create')")
    public Object saveUmsIntegrationConsumeSetting(@RequestBody @Valid UmsIntegrationConsumeSetting entity) {
        try {
            UmsIntegrationConsumeSetting umsIntegrationConsumeSetting = new UmsIntegrationConsumeSetting();
            umsIntegrationConsumeSetting.setDealerId(entity.getDealerId());
            UmsIntegrationConsumeSetting setting1 = IUmsIntegrationConsumeSettingService.getOne(new QueryWrapper<UmsIntegrationConsumeSetting>(umsIntegrationConsumeSetting));
            if (setting1!=null){
                return new CommonResult().failed("已经添加过数据不能重复添加！");
            }
            UmsIntegrationConsumeSetting set = new UmsIntegrationConsumeSetting();
            UmsIntegrationConsumeSetting setting = IUmsIntegrationConsumeSettingService.getById(1);
            BeanUtil.copyProperties(setting,set);
            set.setDealerId(entity.getDealerId());
            set.setRegister(entity.getRegister());
            set.setSign(entity.getSign());
            set.setOrders(entity.getOrders());
            set.setOrdersStatus(entity.getOrdersStatus());
            set.setWaterFee(entity.getWaterFee());
            if (IUmsIntegrationConsumeSettingService.save(set)) {
                return new CommonResult().success();
            }
        } catch (Exception e) {
            log.error("保存积分消费设置：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "ums", REMARK = "更新积分消费设置")
    @ApiOperation("更新积分消费设置")
    @PostMapping(value = "/update/{id}")
    //  @PreAuthorize("hasAuthority('ums:UmsIntegrationConsumeSetting:update')")
    public Object updateUmsIntegrationConsumeSetting(@RequestBody UmsIntegrationConsumeSetting entity) {
        try {
            if (IUmsIntegrationConsumeSettingService.update(entity, new QueryWrapper<UmsIntegrationConsumeSetting>())) {
                return new CommonResult().success();
            }
        } catch (Exception e) {
            log.error("更新积分消费设置：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "ums", REMARK = "删除积分消费设置")
    @ApiOperation("删除积分消费设置")
    @GetMapping(value = "/delete/{id}")
    @PreAuthorize("hasAuthority('ums:UmsIntegrationConsumeSetting:delete')")
    public Object deleteUmsIntegrationConsumeSetting(@ApiParam("积分消费设置id") @PathVariable Long id) {
        try {
            if (ValidatorUtils.empty(id)) {
                return new CommonResult().paramFailed("积分消费设置id");
            }
            if (IUmsIntegrationConsumeSettingService.removeById(id)) {
                return new CommonResult().success();
            }
        } catch (Exception e) {
            log.error("删除积分消费设置：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "ums", REMARK = "给积分消费设置分配积分消费设置")
    @ApiOperation("查询积分消费设置明细-传经销商id")
    @GetMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('ums:UmsIntegrationConsumeSetting:read')")
    public Object getUmsIntegrationConsumeSettingById(@ApiParam("积分消费设置id-经销商id") @PathVariable Long id) {
        try {
            if (ValidatorUtils.empty(id)) {
                return new CommonResult().paramFailed("积分消费设置id");
            }
            UmsIntegrationConsumeSetting setting = new UmsIntegrationConsumeSetting();
            setting.setDealerId(id);
            UmsIntegrationConsumeSetting coupon = IUmsIntegrationConsumeSettingService.getOne(new QueryWrapper<UmsIntegrationConsumeSetting>(setting));
            return new CommonResult().success(coupon);
        } catch (Exception e) {
            log.error("查询积分消费设置明细：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }

    }

    @ApiOperation(value = "批量删除积分消费设置")
    @RequestMapping(value = "/delete/batch", method = RequestMethod.GET)
    @ResponseBody
    @SysLog(MODULE = "pms", REMARK = "批量删除积分消费设置")
    @PreAuthorize("hasAuthority('ums:UmsIntegrationConsumeSetting:delete')")
    public Object deleteBatch(@RequestParam("ids") List<Long> ids) {
        boolean count = IUmsIntegrationConsumeSettingService.removeByIds(ids);
        if (count) {
            return new CommonResult().success(count);
        } else {
            return new CommonResult().failed();
        }
    }

}
