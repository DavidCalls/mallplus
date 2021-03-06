package com.zscat.mallplus.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zscat.mallplus.annotation.Phone;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 后台用户表
 * </p>
 *
 * @author zscat
 * @since 2019-04-14
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("uniacid")
    private Integer uniacid;
    @TableField("uid")
    private Integer uid;
    @TableField("groupid")
    private String groupid;
    @TableField("pid")
    private Long pid;
    @TableField("gid")
    private Long gid;

    @TableField("level")
    private Integer level;

    @NotEmpty(message = "用户名不能为空！")
    @Phone
    private String username;

    private String password;

    /**
     * 头像
     */
    private String icon;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 昵称
     */
    @TableField("nick_name")
    private String nickName;
    @TableField("store_name")
    private String storeName;

    /**
     * 备注信息
     */
    private String note;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 最后登录时间
     */
    @TableField("login_time")
    private Date loginTime;

    /**
     * 帐号启用状态：0->禁用；1->启用
     */
    private Integer status;

    /**
     * 供应商
     */
    @TableField("supply_id")
    private Long supplyId;


    //角色
    @TableField(exist = false)
    private String roleIds;

    @TableField(exist = false)
    private String confimpassword;

    @TableField(exist = false)
    private String code;

    @TableField("store_id")
    @NotNull(message = "门店不能为空！")
    private Integer storeId;

    @TableField("dealer_name")
    @NotEmpty(message = "商户名称不能为空！")
    @Length(min = 2,max = 50,message = "商户名称必须在2-50之间")
    private String dealerName;
    @TableField("dealer_phone")
    @Phone
    private String dealerPhone;
    @TableField("realname")
    @NotEmpty(message = "联系人不能为空！")
    @Length(min = 2,max = 50,message = "联系人必须在2-50之间")
    private String realname;
    @TableField("phone")
    @Phone
    private String phone;
    /**
     * 省
     **/
    @TableField("province")
    private Long province;


    /**
     * 市
     **/
    @TableField("city")
    private Long city;


    /**
     * 县
     **/
    @TableField("county")
    private Long county;
    @TableField("address")
    private String address;
    @TableField("type")
    @NotNull(message = "类型不能为空！")
    @Range(min = 0,max = 3,message = "类型选择错误！")
    private Integer type;
    @TableField("self_pick")
    private Integer selfPick;
    @TableField("weixin_openid")
    private String weixinOpenid;
    @TableField("avatar")
    private String avatar;
    /**
     * 申请状态 0申请中 1待入驻 2已入驻 3驳回
     */
    @TableField("apply_status")
    @NotNull(message = "申请状态不能为空！")
    private Integer applyStatus;

    @TableField("apply_time")
    @JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm")
    @ApiModelProperty(value = "创建日期")
    private Date applyTime;

    /**
     * 余额
     */
    @TableField("balance")
    private BigDecimal balance;

    /**
     * 积分
     */
    @TableField("integration")
    private Integer integration;

    /**
     * 积分状态 积分状态0冻结1正常
     */
    @TableField("integration_status")
    private Integer integrationStatus;

}
