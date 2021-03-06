package com.zscat.mallplus.single;

import com.alipay.api.domain.OrderItem;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zscat.mallplus.annotation.IgnoreAuth;
import com.zscat.mallplus.annotation.SysLog;
import com.zscat.mallplus.cms.entity.CmsSubject;
import com.zscat.mallplus.config.QueryTripartiteExpressLogic;
import com.zscat.mallplus.enums.AllEnum;
import com.zscat.mallplus.enums.ConstansValue;
import com.zscat.mallplus.enums.OrderStatus;
import com.zscat.mallplus.exception.ApiMallPlusException;
import com.zscat.mallplus.oms.entity.OmsOrder;
import com.zscat.mallplus.oms.entity.OmsOrderItem;
import com.zscat.mallplus.oms.entity.OmsOrderOperateHistory;
import com.zscat.mallplus.oms.entity.OmsOrderReturnApply;
import com.zscat.mallplus.oms.service.IOmsOrderItemService;
import com.zscat.mallplus.oms.service.IOmsOrderOperateHistoryService;
import com.zscat.mallplus.oms.service.IOmsOrderService;
import com.zscat.mallplus.oms.vo.ExpressInfo;
import com.zscat.mallplus.oms.vo.ExpressParam;
import com.zscat.mallplus.oms.vo.OrderParam;
import com.zscat.mallplus.pms.entity.PmsProduct;
import com.zscat.mallplus.pms.service.IPmsProductConsultService;
import com.zscat.mallplus.pms.service.IPmsProductService;
import com.zscat.mallplus.sms.service.ISmsGroupService;
import com.zscat.mallplus.ums.entity.UmsMember;
import com.zscat.mallplus.ums.mapper.UmsMemberMapper;
import com.zscat.mallplus.ums.service.IUmsMemberService;
import com.zscat.mallplus.ums.service.impl.RedisUtil;
import com.zscat.mallplus.util.JsonUtils;
import com.zscat.mallplus.utils.CommonResult;
import com.zscat.mallplus.utils.HttpUtils;
import com.zscat.mallplus.utils.ValidatorUtils;
import com.zscat.mallplus.vo.ApplyRefundVo;
import com.zscat.mallplus.vo.OrderStatusCount;
import com.zscat.mallplus.vo.Rediskey;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Auther: shenzhuan
 * @Date: 2019/4/2 15:02
 * @Description:
 */
@Slf4j
@RestController
@Api(tags = "OmsController", description = "订单管理系统")
@RequestMapping("/api/single/oms")
public class SingeOmsController extends ApiBaseAction {
    // 物流信息缓存间隔暂定3个小时
    private static final Long maxCacheTime = 1000 * 60 * 60 * 3L;
    @Resource
    private UmsMemberMapper memberMapper;
    @Resource
    private ISmsGroupService groupService;
    @Resource
    private IOmsOrderService orderService;
    @Resource
    private IOmsOrderItemService orderItemService;
    @Autowired
    private IPmsProductConsultService pmsProductConsultService;
    @Autowired
    private IPmsProductService productService;
    @Autowired
    private IUmsMemberService memberService;
    @Resource
    private com.zscat.mallplus.oms.service.IOmsOrderReturnReasonService IOmsOrderReturnReasonService;
    @Resource
    private com.zscat.mallplus.oms.service.IOmsOrderReturnApplyService IOmsOrderReturnApplyService;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private IOmsOrderOperateHistoryService orderOperateHistoryService;
    @IgnoreAuth
    @SysLog(MODULE = "oms", REMARK = "查询订单列表")
    @ApiOperation(value = "查询订单列表")
    @GetMapping(value = "/order/list")
    public Object orderList(OmsOrder order,
                            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum) {

        IPage<OmsOrder> page = null;
        UmsMember umsMember = memberService.getNewCurrentMember();
        List<String> ids = memberMapper.queryIdList(umsMember.getPhone());
        QueryWrapper<OmsOrder> queryWrapper = new QueryWrapper<OmsOrder>().eq("member_id", umsMember.getId()).isNull("pid");
        ids.forEach(item->{
            queryWrapper.or().eq("member_id",item);
        });
        if (order.getStatus() != null && order.getStatus() == 0) {//全部订单
            page = orderService.page(new Page<OmsOrder>(pageNum, pageSize), queryWrapper.orderByDesc("create_time").select(ConstansValue.sampleOrderList));
        } else {
//            order.setMemberId(umsMember.getId());
            page = orderService.page(new Page<OmsOrder>(pageNum, pageSize), queryWrapper.orderByDesc("create_time").select(ConstansValue.sampleOrderList));
        }
        for (OmsOrder omsOrder : page.getRecords()) {
            List<OmsOrderItem> itemList = orderItemService.list(new QueryWrapper<OmsOrderItem>().eq("order_id", omsOrder.getId()).eq("type", AllEnum.OrderItemType.GOODS.code()));
            omsOrder.setOrderItemList(itemList);
        }
        return new CommonResult().success(page);
    }

    @IgnoreAuth
    @SysLog(MODULE = "oms", REMARK = "查询订单列表")
    @ApiOperation(value = "查询订单列表")
    @GetMapping(value = "/sampleOrderList")
    public Object sampleOrderList(OmsOrder order,
                                  @RequestParam(value = "pageSize", required = false, defaultValue = "10") Integer pageSize,
                                  @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum) {

        IPage<OmsOrder> page = null;
        UmsMember umsMember = memberService.getNewCurrentMember();
        List<String> ids = memberMapper.queryIdList(umsMember.getPhone());
        QueryWrapper<OmsOrder> queryWrapper = new QueryWrapper<OmsOrder>().eq("member_id", umsMember.getId()).isNull("pid");
        ids.forEach(item->{
            queryWrapper.or().eq("member_id",item);
        });
        page = orderService.page(new Page<OmsOrder>(pageNum, pageSize), queryWrapper.orderByDesc("create_time").select(ConstansValue.sampleOrderList));

        return new CommonResult().success(page);
    }

    @ApiOperation("获取订单详情:订单信息、商品信息、操作记录")
    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    @ResponseBody
    public Object detail(@RequestParam(value = "id", required = false, defaultValue = "0") Long id) {
        OmsOrder orderDetailResult = null;
        orderDetailResult = orderService.getById(id);
        OmsOrderItem query = new OmsOrderItem();
        query.setOrderId(id);
        List<OmsOrderItem> orderItemList = orderItemService.list(new QueryWrapper<>(query));
        orderDetailResult.setOrderItemList(orderItemList);
        UmsMember member = memberMapper.selectById(orderDetailResult.getMemberId());
        if (member != null) {
            orderDetailResult.setBlance(member.getBlance());
        }
        return new CommonResult().success(orderDetailResult);
    }

    @ApiOperation("获取订单详情:订单信息、商品信息、操作记录")
    @RequestMapping(value = "/itemDetail", method = RequestMethod.GET)
    @ResponseBody
    public Object itemDetail(@RequestParam(value = "id", required = false, defaultValue = "0") Long id) {
        return new CommonResult().success(orderItemService.getById(id));
    }

    @SysLog(MODULE = "小程序订单管理", REMARK = "取消订单")
    @ApiOperation("关闭订单")
    @RequestMapping(value = "/closeOrder", method = RequestMethod.POST)
    public Object closeOrder(@ApiParam("订单id") @RequestParam Long orderId) {
        try {
            if (ValidatorUtils.empty(orderId)) {
                return new CommonResult().paramFailed("订单id is empty");
            }
            OmsOrder newE = orderService.getById(orderId);
            if (newE.getStatus() != OrderStatus.INIT.getValue()) {
                return new CommonResult().paramFailed("订单已支付，不能关闭");
            }
            if (orderService.closeOrder(newE)) {
                return new CommonResult().success();
            }
        } catch (Exception e) {
            return new CommonResult().failed(e.getMessage());
        }
        return new CommonResult().failed();
    }
    @SysLog(MODULE = "小程序订单管理", REMARK = "取消订单")
    @ApiOperation("关闭订单")
    @RequestMapping(value = "/deleteOrder", method = RequestMethod.POST)
    public Object deleteOrder(@ApiParam("订单id") @RequestParam Long orderId) {
        try {
            if (ValidatorUtils.empty(orderId)) {
                return new CommonResult().paramFailed("订单id is empty");
            }
            return new CommonResult().success(orderService.removeById(orderId));
        } catch (Exception e) {
            return new CommonResult().failed(e.getMessage());
        }

    }

    @SysLog(MODULE = "订单管理", REMARK = "订单确认收货")
    @ApiOperation("订单确认收货")
    @RequestMapping(value = "/confimDelivery", method = RequestMethod.POST)
    @ResponseBody
    public Object confimDelivery(@ApiParam("订单id") @RequestParam Long id) {
        try {
            return orderService.confimDelivery(id);
        } catch (Exception e) {
            return new CommonResult().failed(e.getMessage());
        }
    }

    @SysLog(MODULE = "订单管理", REMARK = "订单申请退款")
    @ApiOperation("申请退款")
    @RequestMapping(value = "/applyRefund", method = RequestMethod.POST)
    @ResponseBody
    public Object applyRefund(@ApiParam("订单id") @RequestParam Long id) {
        try {
            return orderService.applyRefund(id);
        } catch (Exception e) {
            log.error("订单申请退款：%s", e.getMessage(), e);
            return new CommonResult().failed(e.getMessage());
        }
    }

    @SysLog(MODULE = "oms", REMARK = "添加订单评论")
    @ApiOperation(value = "添加订单评论")
    @PostMapping(value = "/orderevaluate")
    public Object addGoodsConsult(@RequestParam(value = "orderId", defaultValue = "1") Long orderId,
                                  @RequestParam(value = "items", defaultValue = "10") String items) throws Exception {
        return orderService.orderComment(orderId, items);
    }

    @SysLog(MODULE = "oms", REMARK = "订单退货申请")
    @ApiOperation(value = "订单退货申请")
    @PostMapping(value = "/applyRe")
    public Object applyRe(@RequestParam(value = "items", defaultValue = "10") String items) throws Exception {
        ApplyRefundVo itemss = JsonUtils.fromJson(items, ApplyRefundVo.class);
        return orderService.applyRe(itemss);
    }

    @ResponseBody
    @GetMapping("/couponSelectList")
    public Object couponHistoryDetailList(OrderParam orderParam) {
        try {
            return new CommonResult().success(orderService.couponHistoryDetailList(orderParam));
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @ResponseBody
    @GetMapping("/submitPreview")
    public Object submitPreview(OrderParam orderParam) {
        try {
            Object result = orderService.submitPreview(orderParam);
            return new CommonResult().success(result);
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @ResponseBody
    @GetMapping("/submitStorePreview")
    public Object submitStorePreview(OrderParam orderParam) {
        try {
            Object result = orderService.submitStorePreview(orderParam);
            return new CommonResult().success(result);
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 团购商品订单预览
     *
     * @param orderParam
     * @return
     */
    @ResponseBody
    @GetMapping("/preGroupActivityOrder")
    public Object preGroupActivityOrder(OrderParam orderParam) {
        try {
            return orderService.preGroupActivityOrder(orderParam);
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 提交订单
     *
     * @param orderParam
     * @return
     */
    @ApiOperation("根据购物车信息生成订单")
    @RequestMapping(value = "/generateStoreOrder")
    @ResponseBody
    public Object generateStoreOrder(OrderParam orderParam) {
        try {
            return orderService.generateStoreOrder(orderParam);
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 提交订单
     *
     * @param orderParam
     * @return
     */
    @ApiOperation("根据购物车信息生成订单")
    @RequestMapping(value = "/generateOrder")
    @ResponseBody
    public Object generateOrder(OrderParam orderParam) {
        try {
            return orderService.generateOrder(orderParam);
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 提交订单之后马上请求的接口，判定是手动发货还是自动发货，
     * 若是手动发货则不改状态
     * 若是自动发货则改成已发 货即待收货
     * @param orderId
     * @return
     */
    @ApiOperation("提交订单之后马上请求的接口，判定是手动发货还是自动发货")
    @RequestMapping(value = "/generateOrderAfter")
    @ResponseBody
    public Object generateOrderAfter(String orderId) {
        try {
            //获取订单信息
            OmsOrder order = orderService.getById(orderId);
            List<OmsOrderItem> list = orderItemService.list(new QueryWrapper<OmsOrderItem>().eq("order_id",orderId));
            Integer status = OrderStatus.DELIVERED.getValue();
            for (OmsOrderItem item:list){
                PmsProduct product = productService.getById(item.getProductId());
                if (product.getAutoDelivery()==0){
                    status = OrderStatus.TO_DELIVER.getValue();
                    break;
                }
            }
            order.setStatus(status);
            orderService.updateById(order);
            return new CommonResult().success();
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @ApiOperation("发起拼团")
    @RequestMapping(value = "/addGroup")
    @ResponseBody
    public Object addGroup(OrderParam orderParam) {
        try {
            return new CommonResult().success(orderService.addGroup(orderParam));
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @ApiOperation("提交拼团")
    @RequestMapping(value = "/acceptGroup")
    @ResponseBody
    public Object acceptGroup(OrderParam orderParam) {
        try {
            return orderService.acceptGroup(orderParam);
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 查看物流
     * orderCode: this.id,
     *         shipperCode: this.orderInfo.deliverySn,
     *         logisticCode: this.orderInfo.deliveryId
     */
    @ApiOperation("查看物流#快鸟物流查询配置")
    @ResponseBody
    @RequestMapping("/wuliuapi")
    public Object getWayBillInfo(ExpressParam expressInfoDo) throws Exception {
        ExpressInfo expressInfo = orderService.getExpressInfo(expressInfoDo.getOrderCode(),
                expressInfoDo.getShipperCode(), expressInfoDo.getLogisticCode());
        if(!expressInfo.isSuccess())   return new CommonResult().failed(expressInfo.getReason());
        return new CommonResult().success(expressInfo);
    }

    /**
     * 查看物流
     * @param
     */
    @ApiOperation("查看物流#快递100")
    @ResponseBody
    @RequestMapping("/wuliu100api")
    public Object getWayBillInfo100(ExpressParam expressInfoDo) throws Exception {
        List<Map<String, String>> expressInfo = orderService.getTransitSteps(expressInfoDo.getOrderCode(),
                expressInfoDo.getShipperCode());
        return new CommonResult().success(expressInfo);
    }
    @Resource
    QueryTripartiteExpressLogic queryTripartiteExpressLogic;

    /**
     * 快递鸟 查看物流
     */
    @ApiOperation("查看物流")
    @ResponseBody
    @RequestMapping("/logisticbyapi")
    public Object getWayBillInfo1(@RequestParam(value = "orderId", required = false, defaultValue = "0") Long orderId, String type) throws Exception {
        try {
            Map<String, Object> returnMap = new HashMap<>();
            OmsOrder order = orderService.getById(orderId);
            if (order == null) {
                return new CommonResult().failed("未查询到物流信息");
            }
            if (true){  // TODO 正式改为false
                //调用快递公司接口，获取物流信息
                String returnStr = queryTripartiteExpressLogic.exec("210001633605");
                if (StringUtils.isEmpty(returnStr)) {
                    log.error("查询物流信息失败,returnStr={}，信息来至于三方快递接口",returnStr);
                    return new CommonResult().failed("未查询到物流信息");
                }
                returnMap = JsonUtils.readJsonToMap(returnStr);
                if (returnMap != null) {
                    returnMap.put("queryTime", System.currentTimeMillis());
                }
                return new CommonResult().success(returnMap);
            }
            UmsMember member = memberService.getNewCurrentMember();

            if (!order.getMemberId().equals(member.getId())) {
                return new CommonResult().failed("非当前用户订单");
            }
            String deliverySn = order.getDeliverySn();
            if (StringUtils.isEmpty(deliverySn)){
                log.error("未查询到运单号，orderId={},deliverySn={}",orderId,order.getDeliverySn());
                return new CommonResult().failed("未查询到运单号");
            }


            log.info("开始查询运单号为{}的物流信息", deliverySn);

            // 从redis里获取物流信息
            if (redisUtil.hExists(Rediskey.KDWL_INFO_CACHE, deliverySn)) {
                // redis已经有缓存
                Object kdwlInfo = redisUtil.hGet(Rediskey.KDWL_INFO_CACHE, deliverySn);
                if (ValidatorUtils.notEmpty(kdwlInfo)) {
                    returnMap = JsonUtils.readJsonToMap(kdwlInfo.toString());
                    Long cacheBeginTime = (Long) returnMap.get("queryTime");
                    // 计算已经缓存时长
                    if (System.currentTimeMillis() - cacheBeginTime < maxCacheTime) {
                        // 直接取缓存
                        // kdwlInfoToMap.remove("queryTime");
                        log.info("查询运单号为{}的物流信息结束,物流信息：{},信息来至于Redis缓存", deliverySn, kdwlInfo);
                        return new CommonResult().success(returnMap);
                    }
                }
            }

            //调用快递公司接口，获取物流信息
            String returnStr = orderService.queryExpressInfo(order);
            if (StringUtils.isEmpty(returnStr)) {
                log.error("查询物流信息失败,returnStr={}，信息来至于三方快递接口",returnStr);
                return new CommonResult().failed("未查询到物流信息");
            }

            returnMap = JsonUtils.readJsonToMap(returnStr);
            if (returnMap != null) {
                returnMap.put("queryTime", System.currentTimeMillis());
            }
            redisUtil.hPut(Rediskey.KDWL_INFO_CACHE, deliverySn, JsonUtils.objectToJson(returnMap));
            log.info("查询运单号为={},的物流信息结束,物流信息：{}", deliverySn, returnStr);

            return new CommonResult().success(returnMap);
        } catch (Exception e) {
            log.error("get waybillInfo error. error=" + e.getMessage(), e);
            return new CommonResult().failed("获取物流信息失败，请稍后重试");
        }
    }

    @IgnoreAuth
    @ApiOperation("获取订单不同状态的数量")
    @SysLog(MODULE = "applet", REMARK = "获取订单不同状态的数量")
    @GetMapping("/order.getorderstatusnum")
    public Object getOrderStatusSum() {
        Map<String, Object> objectMap = new HashMap<>();
        UmsMember umsMember = memberService.getNewCurrentMember();
        OrderStatusCount count = new OrderStatusCount();
        if (umsMember != null && umsMember.getId() != null) {
            OmsOrder param = new OmsOrder();
            param.setMemberId(umsMember.getId());
            List<OmsOrder> list = orderService.list(new QueryWrapper<>(param));
            int status0 = 0;
            int status1 = 0;
            int status2 = 0;
            int status3 = 0;
            int status4 = 0;
            int status5 = 0;
            int status14 = 0;

            int statusAll = 0;
            BigDecimal payAmount = BigDecimal.ZERO;
            for (OmsOrder consult : list) {
                if (consult.getStatus() == OrderStatus.INIT.getValue()) {
                    status0++;
                }
                if (consult.getStatus() == OrderStatus.REFUND.getValue()) {
                    status14++;
                }
                if (consult.getStatus() == OrderStatus.TO_DELIVER.getValue()) {
                    status1++;
                    payAmount = payAmount.add(consult.getPayAmount());
                }
                if (consult.getStatus() == OrderStatus.DELIVERED.getValue()) {
                    status2++;
                    payAmount = payAmount.add(consult.getPayAmount());

                }
                if (consult.getStatus() == OrderStatus.TO_COMMENT.getValue()) {
                    status3++;
                    payAmount = payAmount.add(consult.getPayAmount());

                }
                if (consult.getStatus() == OrderStatus.TRADE_SUCCESS.getValue()) {
                    status4++;
                    payAmount = payAmount.add(consult.getPayAmount());

                }

            }
            statusAll = status1 + status2 + status3 + status4 + status5;
            count.setPayAmount(payAmount);
            count.setStatusAll(statusAll);
            count.setStatus0(status0);
            count.setStatus1(status1);
            count.setStatus2(status2);
            count.setStatus3(status3);
            count.setStatus4(status4);
            count.setStatus5(status5);
            count.setStatus14(status14);
        }
        objectMap.put("user", umsMember);
        objectMap.put("count", count);

        return new CommonResult().success(objectMap);
    }

    @IgnoreAuth
    @SysLog(MODULE = "oms", REMARK = "售后单列表")
    @ApiOperation(value = "售后单列表")
    @GetMapping(value = "/order/aftersaleslist")
    public Object afterSalesList(OmsOrderReturnApply order,
                                 @RequestParam(value = "pageSize", required = false, defaultValue = "100") Integer pageSize,
                                 @RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum) {

        IPage<OmsOrderReturnApply> page = IOmsOrderReturnApplyService.page(new Page<OmsOrderReturnApply>(pageNum, pageSize), new QueryWrapper<>(order).orderByDesc("create_time"));
        for (OmsOrderReturnApply omsOrder : page.getRecords()) {
            List<OmsOrderItem> itemList = orderItemService.list(new QueryWrapper<OmsOrderItem>().eq("order_id", omsOrder.getOrderId()).eq("type", AllEnum.OrderItemType.GOODS.code()));
            omsOrder.setOrderItemList(itemList);
        }
        return new CommonResult().success(page);
    }

    @IgnoreAuth
    @SysLog(MODULE = "oms", REMARK = "售后单详情")
    @ApiOperation(value = "售后单详情")
    @GetMapping(value = "/order/aftersalesinfo")
    public Object afterSalesInfo(@RequestParam Long id) {
        OmsOrderReturnApply omsOrder = IOmsOrderReturnApplyService.getById(id);
        List<OmsOrderItem> itemList = orderItemService.list(new QueryWrapper<OmsOrderItem>().eq("order_id", omsOrder.getId()).eq("type", AllEnum.OrderItemType.GOODS.code()));
        omsOrder.setOrderItemList(itemList);
        omsOrder.setOrder(orderService.getById(omsOrder.getOrderId()));
        return new CommonResult().success(omsOrder);
    }

    @SysLog(MODULE = "cms", REMARK = "订单售后状态")
    @ApiOperation(value = "订单售后状态")
    @GetMapping(value = "/order.aftersalesstatus")
    public Object afterSalesStatus(CmsSubject subject, BindingResult result) {
        CommonResult commonResult;
        UmsMember member = memberService.getNewCurrentMember();

        return null;
    }


    @SysLog(MODULE = "cms", REMARK = "用户发送退货包裹")
    @ApiOperation(value = "用户发送退货包裹")
    @PostMapping(value = "/order.sendreship")
    public Object sendShip(CmsSubject subject, BindingResult result) {
        CommonResult commonResult;
        UmsMember member = memberService.getNewCurrentMember();

        return null;
    }

    @SysLog(MODULE = "cms", REMARK = "售后原因")
    @ApiOperation(value = "售后原因")
    @PostMapping(value = "/order/getRefundReason")
    public Object getRefundReason() {
        return new CommonResult().success(IOmsOrderReturnReasonService.list(new QueryWrapper<>()));
    }

    @ApiOperation("保存订单售后申请")
    @RequestMapping(value = "/saveSingleOrderReturnApply")
    @ResponseBody
    public Object saveSingleOrderReturnApply(OmsOrderReturnApply returnApply) {
        try {
            OmsOrderItem item = orderItemService.getById(returnApply.getOrderId());

            OmsOrderReturnApply apply = new OmsOrderReturnApply();
            UmsMember member = memberService.getNewCurrentMember();

            apply.setStatus(AllEnum.OmsOrderReturnApplyStatus.INIT.code());
            apply.setCreateTime(new Date());
            apply.setReturnAmount(returnApply.getReturnAmount());
            apply.setDescription(returnApply.getDescription());
            apply.setOrderId(item.getOrderId());
            apply.setOrderSn(item.getOrderSn());
            apply.setMemberUsername(member.getUsername());
            apply.setProductAttr(item.getProductAttr());
            apply.setProductCount(item.getProductQuantity());
            apply.setProductBrand(item.getProductBrand());
            apply.setProductId(item.getProductId());
            apply.setProductName(item.getProductName());
            apply.setProductPic(item.getProductPic());
            apply.setProductPrice(item.getRealAmount());
            apply.setStoreId(item.getStoreId());
            apply.setProductRealPrice(item.getProductPrice());
            apply.setProofPics(returnApply.getProofPics());
            apply.setReason(returnApply.getReason());
            apply.setType(returnApply.getType());
            apply.setReturnPhone(member.getPhone());
            apply.setReturnName(member.getId()+"");


            OmsOrderOperateHistory history = new OmsOrderOperateHistory();
            history.setOrderId(item.getOrderId());
            history.setCreateTime(new Date());
            history.setOperateMan("shop");
            //  //0换货 1退钱 2退货3 退钱退货
            if (apply.getType()==0){
                history.setNote("换货");
            }else  if (apply.getType()==1){
                history.setNote("申请退款");
            }else  if (apply.getType()==2){
                history.setNote("申请退货");
            }else  if (apply.getType()==3){
                history.setNote("申请退货退款");
            }

            orderOperateHistoryService.save(history);
            return IOmsOrderReturnApplyService.save(apply);
        } catch (ApiMallPlusException e) {
            return new CommonResult().failed(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SysLog(MODULE = "oms", REMARK = "保存订单售后申请")
    @ApiOperation("保存订单售后申请")
    @PostMapping(value = "/saveOmsOrderReturnApply")
    public Object saveOmsOrderReturnApply(@RequestParam(value = "items") String items,
                                          @RequestParam(value = "type") Integer type,
                                          @RequestParam(value = "images", required = false) String[] images,
                                          @RequestParam(value = "returnAmount") BigDecimal returnAmount,
                                          @RequestParam(value = "desc", required = false) String desc) throws Exception {
        ApplyRefundVo vo = new ApplyRefundVo();
        try {
            vo.setDesc(desc);
            vo.setItems(items);
            vo.setReturnAmount(returnAmount);
            vo.setType(type);
            vo.setImages(images);
            return orderService.applyRe(vo);
        } catch (Exception e) {
            return new CommonResult().failed();
        }
    }
}
