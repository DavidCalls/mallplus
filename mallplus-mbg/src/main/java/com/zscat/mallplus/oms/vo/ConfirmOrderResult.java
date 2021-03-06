package com.zscat.mallplus.oms.vo;


import com.zscat.mallplus.oms.entity.OmsCartItem;
import com.zscat.mallplus.pms.entity.PmsProduct;
import com.zscat.mallplus.sms.entity.SmsBasicGifts;
import com.zscat.mallplus.sms.entity.SmsGroupActivity;
import com.zscat.mallplus.sms.vo.SmsCouponHistoryDetail;
import com.zscat.mallplus.ums.entity.UmsIntegrationConsumeSetting;
import com.zscat.mallplus.ums.entity.UmsMemberReceiveAddress;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 确认单信息封装
 * https://github.com/shenzhuan/mallplus on 2018/8/30.
 */
@Data
public class ConfirmOrderResult {

    // 赠品营销
    List<SmsBasicGifts> basicGiftsList;
    SmsGroupActivity groupActivity;
    private UmsMemberReceiveAddress address;
    //包含优惠信息的购物车信息
    private List<OmsCartItem> cartPromotionItemList;
    //用户收货地址列表
    private List<UmsMemberReceiveAddress> memberReceiveAddressList;
    //用户可用优惠券列表
    private List<SmsCouponHistoryDetail> couponHistoryDetailList;
    //积分使用规则
    private UmsIntegrationConsumeSetting integrationConsumeSetting;
    //会员此次订单可以使用的积分
    private Integer memberIntegration;
    private BigDecimal integrationAmount;
    /**
     * 余额
     */
    private BigDecimal blance;
    private Integer offline = 1;// １ 送货 ２ 自取
    //计算的金额
    private CalcAmount calcAmount;
    private PmsProduct goods;
    private GroupAndOrderVo groupAndOrderVo;
    private String storeName;
    private Integer storeId;

    public static class CalcAmount {
        //订单商品总金额
        private BigDecimal totalAmount;
        //运费
        private BigDecimal freightAmount;
        //活动优惠
        private BigDecimal promotionAmount;
        //应付金额
        private BigDecimal payAmount;

        public BigDecimal getTotalAmount() {
            return totalAmount;
        }

        public void setTotalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
        }

        public BigDecimal getFreightAmount() {
            return freightAmount;
        }

        public void setFreightAmount(BigDecimal freightAmount) {
            this.freightAmount = freightAmount;
        }

        public BigDecimal getPromotionAmount() {
            return promotionAmount;
        }

        public void setPromotionAmount(BigDecimal promotionAmount) {
            this.promotionAmount = promotionAmount;
        }

        public BigDecimal getPayAmount() {
            return payAmount;
        }

        public void setPayAmount(BigDecimal payAmount) {
            this.payAmount = payAmount;
        }

    }
}
