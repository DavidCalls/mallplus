package com.zscat.mallplus.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zscat.mallplus.config.MallplusProperties;
import com.zscat.mallplus.enums.ConstansValue;
import com.zscat.mallplus.enums.StatusEnum;
import com.zscat.mallplus.fenxiao.entity.FenxiaoConfig;
import com.zscat.mallplus.fenxiao.mapper.FenxiaoConfigMapper;
import com.zscat.mallplus.oms.vo.StoreContentResult;
import com.zscat.mallplus.pms.entity.PmsProduct;
import com.zscat.mallplus.pms.entity.PmsProductAttributeCategory;
import com.zscat.mallplus.pms.service.IPmsProductAttributeCategoryService;
import com.zscat.mallplus.pms.service.IPmsProductService;
import com.zscat.mallplus.sms.entity.SmsHomeAdvertise;
import com.zscat.mallplus.sms.service.ISmsHomeAdvertiseService;
import com.zscat.mallplus.sys.entity.SysStore;
import com.zscat.mallplus.sys.entity.SysUser;
import com.zscat.mallplus.sys.entity.SysUserRole;
import com.zscat.mallplus.sys.mapper.SysStoreMapper;
import com.zscat.mallplus.sys.mapper.SysUserMapper;
import com.zscat.mallplus.sys.mapper.SysUserRoleMapper;
import com.zscat.mallplus.ums.entity.UmsMember;
import com.zscat.mallplus.ums.service.IStoreService;
import com.zscat.mallplus.ums.service.IUmsMemberService;
import com.zscat.mallplus.util.OssAliyunUtil;
import com.zscat.mallplus.utils.CommonResult;
import com.zscat.mallplus.util.MatrixToImageWriter;
import com.zscat.mallplus.utils.CreateNamePicture;
import com.zscat.mallplus.utils.ValidatorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2019/12/28.
 */
@Service
@Slf4j
public class StoreServiceImpl extends ServiceImpl<SysStoreMapper, SysStore> implements IStoreService {

    @Resource
    SysStoreMapper storeMapper;
    @Resource
    IUmsMemberService memberService;
    @Resource
    SysUserMapper userMapper;
    @Resource
    SysUserRoleMapper userRoleMapper;
    @Resource
    IPmsProductAttributeCategoryService productAttributeCategoryService;
    @Resource
    IPmsProductService productService;
    @Resource
    FenxiaoConfigMapper fenxiaoConfigMapper;
    @Autowired
    OssAliyunUtil aliyunOSSUtil;
    @Autowired
    private ISmsHomeAdvertiseService advertiseService;

    @Resource
    private   MallplusProperties mallplusProperties;
    @Resource
    private BCryptPasswordEncoder passwordEncoder;
    @Override
    @Transactional
    public Object applyStore(SysStore entity) {
        entity.setStatus(StatusEnum.AuditType.FAIL.code());
        entity.setTryTime(new Date());
        entity.setCreateTime(new Date());
        SysUser user = new SysUser();
        if (ValidatorUtils.empty(entity.getName())) {
            return new CommonResult().failed("请输入店铺名称");
        }
        user.setUsername(entity.getName());
        SysUser umsAdminList = userMapper.selectByUserName(entity.getName());
        if (umsAdminList != null && umsAdminList.getId() != null) {
            return new CommonResult().failed("你已申请");
        }
        storeMapper.insert(entity);
        String url = mallplusProperties.getDomain()+"/#/pages/store/store?id=" + entity.getId();
        //要添加到二维码下面的文字
        String words = entity.getName() + "的二维码";
        //调用刚才的工具类
        ByteArrayResource qrCode = MatrixToImageWriter.createQrCode(url, words);
        InputStream inputStream = new ByteArrayInputStream(qrCode.getByteArray());
        entity.setContactQrcode(aliyunOSSUtil.upload("png", inputStream));
        storeMapper.updateById(entity);
        user.setStatus(3);
        user.setSupplyId(0L);
        String md5Password = passwordEncoder.encode("123456");
        user.setPassword(md5Password);
        user.setCreateTime(new Date());
        //user.setIcon(entity.getLogo());
        if (ValidatorUtils.empty(user.getIcon())){
            try {
                user.setIcon(CreateNamePicture.generateImg(user.getUsername(),mallplusProperties.getTemppath(),System.currentTimeMillis()+user.getUsername()));
            } catch (IOException e) {
                log.error("用户名图片生成失败");
            }
        }
        user.setNickName(entity.getName());
        user.setStoreId(entity.getId());
        user.setEmail(entity.getSupportPhone());
        user.setStoreName(entity.getName());
        userMapper.insert(user);
       /* UmsMember umsMember = memberService.getNewCurrentMember();
        if (umsMember != null && umsMember.getId() != null) {
            umsMember.setStoreId(entity.getId());
            memberService.updateById(umsMember);
        }*/

        FenxiaoConfig config = new FenxiaoConfig();
        config.setId(Long.valueOf(entity.getId()));
        config.setStoreId(entity.getId());
        config.setStatus(0);
        fenxiaoConfigMapper.insert(config);
        SysUserRole userRole = new SysUserRole();
        userRole.setRoleId(3L);
        userRole.setAdminId(user.getId());
        userRoleMapper.insert(userRole);
        return new CommonResult().success(entity);

    }

    @Override
    public StoreContentResult singeleContent(Integer id) {
        StoreContentResult result = new StoreContentResult();
        List<PmsProductAttributeCategory> categoryList = productAttributeCategoryService.page(new Page<PmsProductAttributeCategory>(0, 10), new QueryWrapper<PmsProductAttributeCategory>().eq("store_id", id)).getRecords();
        if (categoryList != null && categoryList.size() > 5) {
            result.setCategoryList(categoryList.subList(0, 5));
            result.setCategoryList1(categoryList.subList(5, categoryList.size() - 1));
        } else {
            result.setCategoryList(categoryList);
        }
        PmsProduct product = new PmsProduct();
        product.setVerifyStatus(StatusEnum.YesNoType.YES.code());
        product.setRecommandStatus(StatusEnum.YesNoType.YES.code());
        product.setPublishStatus(StatusEnum.YesNoType.YES.code());
        product.setStoreId(id);
        List<PmsProduct> recomList = productService.list(new QueryWrapper<>(product).select(ConstansValue.sampleGoodsList));
        SmsHomeAdvertise advertise = new SmsHomeAdvertise();
        advertise.setStatus(1);
        advertise.setStoreId(id);
        List<SmsHomeAdvertise> advertises = advertiseService.list(new QueryWrapper<>(advertise));
        result.setAdvertiseList(advertises);
        result.setHotProductList(recomList);

        return result;
    }
}
