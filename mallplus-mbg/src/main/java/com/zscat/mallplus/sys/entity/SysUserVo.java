package com.zscat.mallplus.sys.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

/**
 * <p>
 * 后台用户表
 * </p>
 *
 * @author zscat
 * @since 2019-04-14
 */
@Data
public class SysUserVo extends SysUser {

    /**
     * 设备管理组id
     **/
    @TableField("device_group_id")
    private Long deviceGroupId;
}
