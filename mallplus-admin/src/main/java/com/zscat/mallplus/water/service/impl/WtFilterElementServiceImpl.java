package com.zscat.mallplus.water.service.impl;

import com.zscat.mallplus.util.ConstantUtil;
import com.zscat.mallplus.water.entity.WtFilterElement;
import com.zscat.mallplus.water.mapper.WtFilterElementMapper;
import com.zscat.mallplus.water.service.IWtFilterElementService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lyn
 * @date 2020-05-20
 */
@Service
public class WtFilterElementServiceImpl extends ServiceImpl
        <WtFilterElementMapper, WtFilterElement> implements IWtFilterElementService {

    @Resource
    private WtFilterElementMapper wtFilterElementMapper;

    //自动更新滤芯计时类型状态区分
    public void updateState(){
        wtFilterElementMapper.updateState(ConstantUtil.delFlag,ConstantUtil.billing_mode_time
        ,ConstantUtil.filter_element_state_0,ConstantUtil.filter_element_state_1,ConstantUtil.filter_element_state_2);
    }
}
