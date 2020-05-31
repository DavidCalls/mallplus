package com.zscat.mallplus.water.service;

import com.zscat.mallplus.water.entity.SimEntity;
import com.zscat.mallplus.water.entity.WtSimCard;
import com.zscat.mallplus.water.entity.WtSimUrlInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author lyn
 * @date 2020-05-22
 */

public interface IWtSimUrlInfoService extends IService<WtSimUrlInfo> {

    //物联网卡余量查询
    SimEntity getChaxun(Long id,String cardno);
}
