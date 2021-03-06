package com.zscat.mallplus.sys.controller;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.ApiController;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zscat.mallplus.annotation.IgnoreAuth;
import com.zscat.mallplus.annotation.SysLog;
import com.zscat.mallplus.build.entity.BuildingCommunity;
import com.zscat.mallplus.build.entity.UserCommunityRelate;
import com.zscat.mallplus.build.mapper.BuildingCommunityMapper;
import com.zscat.mallplus.build.mapper.UserCommunityRelateMapper;
import com.zscat.mallplus.sys.entity.*;
import com.zscat.mallplus.sys.mapper.SysPermissionMapper;
import com.zscat.mallplus.sys.service.ISysPermissionService;
import com.zscat.mallplus.sys.service.ISysRoleService;
import com.zscat.mallplus.sys.service.ISysUserService;
import com.zscat.mallplus.sys.service.ISysUserStaffService;
import com.zscat.mallplus.ums.entity.SysAppletSet;
import com.zscat.mallplus.ums.entity.UmsMember;
import com.zscat.mallplus.ums.service.ISysAppletSetService;
import com.zscat.mallplus.ums.service.IUmsMemberService;
import com.zscat.mallplus.ums.service.RedisService;
import com.zscat.mallplus.ums.vo.SysDealerVo;
import com.zscat.mallplus.util.JsonUtil;
import com.zscat.mallplus.util.JwtTokenUtil;
import com.zscat.mallplus.util.StringUtils;
import com.zscat.mallplus.util.UserUtils;
import com.zscat.mallplus.utils.BeanUtil;
import com.zscat.mallplus.utils.CommonResult;
import com.zscat.mallplus.utils.PhoneUtil;
import com.zscat.mallplus.utils.ValidatorUtils;
import com.zscat.mallplus.vo.Rediskey;
import com.zscat.mallplus.vo.SmsCode;
import com.zscat.mallplus.weixinmp.entity.AccountWechats;
import com.zscat.mallplus.weixinmp.service.IAccountWechatsService;
import com.zscat.mallplus.wxminiapp.entity.AccountWxapp;
import com.zscat.mallplus.wxminiapp.service.IAccountWxappService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.ValidationException;
import java.security.Principal;
import java.util.*;

/**
 * <p>
 * 后台用户表 前端控制器
 * </p>
 *
 * @author zscat
 * @since 2019-04-14
 */
@Slf4j
@Api(value = "用户管理", description = "", tags = {"用户管理"})
@RestController
@RequestMapping("/sys/sysUser")
public class SysUserController extends ApiController {

    @Value("${jwt.tokenHeader}")
    private String tokenHeader;
    @Value("${jwt.tokenHead}")
    private String tokenHead;
    @Resource
    private ISysUserService sysUserService;
    @Resource
    private ISysAppletSetService appletSetService;
    @Resource
    private IAccountWechatsService wechatsService;
    @Resource
    private IAccountWxappService wxappService;
    @Resource
    private ISysRoleService roleService;
    @Resource
    private ISysPermissionService permissionService;
    @Resource
    private SysPermissionMapper permissionMapper;
    @Resource
    private RedisService redisService;
    @Resource
    private UserCommunityRelateMapper userCommunityRelateMapper;
    @Resource
    private BuildingCommunityMapper buildingCommunityMapper;
    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @SysLog(MODULE = "sys", REMARK = "根据条件查询所有用户列表")
    @ApiOperation("根据条件查询所有用户列表")
    @GetMapping(value = "/list")
    public Object getUserByPage(SysUser entity,
                                @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize
    ) {
        try {
            entity.setStoreId(UserUtils.getCurrentMember().getStoreId());
            QueryWrapper queryWrapper = new QueryWrapper<>(entity);
            queryWrapper.notIn("id",1);
            return new CommonResult().success(sysUserService.page(new Page<SysUser>(pageNum, pageSize), queryWrapper));
        } catch (Exception e) {
            log.error("根据条件查询所有用户列表：%s", e.getMessage(), e);
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "sys", REMARK = "添加经销商的时候需要的数据")
    @ApiOperation("添加经销商的时候需要的数据")
    @GetMapping(value = "/listDealer")
    public Object listDealer(@RequestParam Integer level,
                             @RequestParam Integer storeId,
                             @RequestParam String value) {
        List<Map<String,Object>> list = sysUserService.
                listDealer(level,value,storeId);
        return new CommonResult().success(list);
    }

    @SysLog(MODULE = "sys", REMARK = "保存用户")
    @ApiOperation("保存用户")
    @PostMapping(value = "/register")
    public Object saveUser(@RequestBody SysUser entity) {
        try {
            if (ValidatorUtils.empty(entity.getStoreId())) {
                entity.setStoreId(UserUtils.getCurrentMember().getStoreId());
            }
            entity.setStoreName(UserUtils.getCurrentMember().getStoreName());
            if (sysUserService.saves(entity)) {
                return new CommonResult().success();
            }
        } catch (Exception e) {
            log.error("保存用户：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }
        return new CommonResult().failed("用户名已存在");
    }

    @SysLog(MODULE = "sys", REMARK = "添加经销商或者入驻的时候添加")
    @ApiOperation("添加经销商或者入驻的时候添加")
    @PostMapping(value = "/create")
    @PreAuthorize("hasAuthority('sys:user:createUser')")
    @Transactional
    public Object createUser(@RequestBody @Valid  SysDealerVo entity) {
        SysUser user = entity.getUser();
        SysAppletSet appletSet = entity.getAppletSet();
        //校验基础数据
        if (user==null||user.getLevel()==null||user.getPid()==null||user.getGid()==null||user.getStatus()==null){
            return new CommonResult().failed("添加的经销商信息错误！");
        }
        if (appletSet==null){
            return new CommonResult().failed("添加的经销商基本入驻信息不能为空！");
        }
        try {
            if (ValidatorUtils.empty(user.getStoreId())) {
                user.setStoreId(UserUtils.getCurrentMember().getStoreId());
            }
            if (ValidatorUtils.empty(user.getStoreName())) {
                user.setStoreName(UserUtils.getCurrentMember().getStoreName());
            }
            //如果是后台添加的经销商信息，则需要添加之后获取它的id
            if (ValidatorUtils.empty(user.getId())){
                if (!sysUserService.saves(user)){
                    return new CommonResult().failed("保存经销商信息失败，用户已存在！");
                }
                SysUser sysUser = new SysUser();
                sysUser.setUsername(user.getUsername());
                sysUser.setStoreId(user.getStoreId());
                user = sysUserService.getOne(new QueryWrapper<>(sysUser));
            }
            //如果是需要入驻的信息
            appletSet.setUserId(user.getId());
            appletSet.setAppid("0");
            appletSet.setStoreId(user.getStoreId());
            appletSet.setLevelId(user.getLevel());
            if (appletSet.getSelfType()==2){
                //代收的话，将代收账号的APPid添加上
                SysAppletSet set = appletSetService.getById(appletSet.getParentUserId());
                if (set==null){
                    throw new ValidationException("上级账号信息不能为空！");
                }
                appletSet.setAppid(set.getAppid());
                appletSet.setAppsecret(set.getAppsecret());
                appletSet.setMchid(set.getMchid());
                appletSet.setPaySignKey(set.getPaySignKey());
                appletSet.setCertname(set.getCertname());
                appletSet.setNotifyurl(set.getNotifyurl());
                appletSet.setTemplateid1(set.getTemplateid1());
                appletSet.setTemplateid2(set.getTemplateid2());
                appletSet.setTemplateid3(set.getTemplateid3());
                appletSet.setTemplateid4(set.getTemplateid4());
                appletSet.setTemplateid5(set.getTemplateid5());
                appletSet.setTemplateid6(set.getTemplateid6());
                //添加上级的公众号和小程序
                AccountWxapp wxapp = new AccountWxapp();
                AccountWxapp accountWxapp1 = new AccountWxapp();
                accountWxapp1.setCreateBy(appletSet.getParentUserId());
                AccountWxapp accountWxapp = wxappService.getOne(new QueryWrapper<AccountWxapp>(accountWxapp1));
                if (accountWxapp==null){
                    throw new ValidationException("上级账号小程序信息不能为空！");
                }
                BeanUtil.copyProperties(accountWxapp,wxapp);
                Integer count = wxappService.getCount();
                wxapp.setUniacid(count);
                wxapp.setCreateBy(user.getId());
                wxapp.setCreateTime(new Date());
                wxappService.save(wxapp);
                //添加上级的公众号
                AccountWechats wechats = new AccountWechats();
                AccountWechats accountWechats = new AccountWechats();
                accountWechats.setCreateBy(appletSet.getParentUserId());
                AccountWechats accountWechats1 = wechatsService.getOne(new QueryWrapper<AccountWechats>(accountWechats));
                if (accountWechats1==null){
                    throw new ValidationException("上级账号公众号信息不能为空！");
                }
                BeanUtil.copyProperties(accountWechats1,wechats);
                Integer count1 = wxappService.getCount();
                wechats.setCreateBy(user.getId());
                wechats.setCreateTime(new Date());
                wechats.setUniacid(count1);
                wechatsService.save(wechats);
            }
            if (!appletSetService.save(appletSet)){
                return new CommonResult().failed("添加经销商基本入驻信息错误！");
            }
            entity.setUser(user);
            entity.setAppletSet(appletSet);
            return new CommonResult().success(entity);
        } catch (Exception e) {
            log.error("保存用户：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }
    }

    @SysLog(MODULE = "sys", REMARK = "更新用户")
    @ApiOperation("更新用户")
    @PostMapping(value = "/update/{id}")
    public Object updateUser(@RequestBody SysDealerVo entity) {
        SysUser user = entity.getUser();
        SysAppletSet appletSet = entity.getAppletSet();
        //校验基础数据
        if (user==null){
            return new CommonResult().failed("添加的经销商信息不能为空！");
        }
        if (appletSet==null){
            return new CommonResult().failed("添加的经销商基本入驻信息不能为空！");
        }
        try {
            if (sysUserService.updates(user.getId(), user)) {
                appletSet.setUserId(user.getId());
                if (appletSetService.updateById(appletSet)) {
                    return new CommonResult().success();
                }else {
                    return new CommonResult().failed();
                }
            }
        } catch (Exception e) {
            log.error("更新用户：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "sys", REMARK = "删除用户")
    @ApiOperation("删除用户")
    @GetMapping(value = "/delete/{id}")
    public Object deleteUser(@ApiParam("用户id") @PathVariable Long id) {
        try {
            if (ValidatorUtils.empty(id)) {
                return new CommonResult().paramFailed("用户id");
            }
            SysUser user = sysUserService.getById(id);
            if (user.getSupplyId() != null && user.getSupplyId() == 1) {
                return new CommonResult().paramFailed("管理员账号不能删除");
            }
            if (sysUserService.removeById(id)) {
                appletSetService.removeById(id);
                return new CommonResult().success();
            }
        } catch (Exception e) {
            log.error("删除用户：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "sys", REMARK = "给用户分配角色")
    @ApiOperation("查询用户明细")
    @GetMapping(value = "/{id}")
    public Object getUserById(@ApiParam("用户id") @PathVariable Long id) {
        try {
            if (ValidatorUtils.empty(id)) {
                return new CommonResult().paramFailed("用户id");
            }
            SysDealerVo coupon = new SysDealerVo();
            SysUser user = sysUserService.getById(id);
            user.setPassword(null);
            coupon.setUser(user);
            SysAppletSet set = appletSetService.getById(id);
            coupon.setAppletSet(set);
            return new CommonResult().success(coupon);
        } catch (Exception e) {
            log.error("查询用户明细：%s", e.getMessage(), e);
            return new CommonResult().failed();
        }

    }

    @SysLog(MODULE = "sys", REMARK = "刷新token")
    @ApiOperation(value = "刷新token")
    @RequestMapping(value = "/token/refresh", method = RequestMethod.GET)
    @ResponseBody
    public Object refreshToken(HttpServletRequest request) {
        String token = request.getHeader(tokenHeader);
        String refreshToken = sysUserService.refreshToken(token);
        if (refreshToken == null) {
            return new CommonResult().failed();
        }
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("token", token);
        tokenMap.put("tokenHead", tokenHead);
        tokenMap.put("userInfo", UserUtils.getCurrentMember());
        return new CommonResult().success(tokenMap);
    }

    @SysLog(MODULE = "sys", REMARK = "登录以后返回token")
    @ApiOperation(value = "登录以后返回token")
    @RequestMapping(value = "/login1", method = RequestMethod.POST)
    @ResponseBody
    public Object login1(@RequestParam(value = "username", defaultValue = "1") String username,
                         @RequestParam(value = "password", defaultValue = "1") String password) {
        try {
            String token = sysUserService.login(username, password);
            if (token == null) {
                return new CommonResult().paramFailed("用户名或密码错误");
            }
            Map<String, Object> tokenMap = new HashMap<>();
            tokenMap.put("token", token);
            tokenMap.put("tokenHead", tokenHead);
            tokenMap.put("userInfo", UserUtils.getCurrentMember());
            return new CommonResult().success(tokenMap);
        } catch (Exception e) {
            return new CommonResult().failed(e.getMessage());
        }
    }

    @SysLog(MODULE = "sys", REMARK = "登录以后返回token")
    @ApiOperation(value = "登录以后返回token")
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    @ResponseBody
    public Object login(@RequestBody SysUser umsAdminLoginParam, BindingResult result) {
        try {
            String token = sysUserService.login(umsAdminLoginParam.getUsername(), umsAdminLoginParam.getPassword());
            if (token == null) {
                return new CommonResult().paramFailed("用户名或密码错误");
            }
            Map<String, Object> tokenMap = new HashMap<>();
            tokenMap.put("token", token);
            tokenMap.put("tokenHead", tokenHead);
            tokenMap.put("userInfo", UserUtils.getCurrentMember());
            return new CommonResult().success(tokenMap);
        } catch (Exception e) {
            return new CommonResult().failed(e.getMessage());
        }
    }

    @SysLog(MODULE = "sys", REMARK = "获取当前登录用户信息")
    @ApiOperation(value = "获取当前登录用户信息")
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    @ResponseBody
    public Object getAdminInfo(Principal principal) {

        String username = principal.getName();
        SysUser queryU = new SysUser();
        queryU.setUsername(username);
        SysUser umsAdmin = sysUserService.getOne(new QueryWrapper<>(queryU));
        Map<String, Object> data = new HashMap<>();
        data.put("username", username);

        if (umsAdmin != null && umsAdmin.getId() != null) {
            Set<String> roles = permissionService.getRolePermission(umsAdmin);
            Set<String> permissions = permissionService.getMenuPermission(umsAdmin);
            data.put("permissions", permissions);
            data.put("roles", roles);
            data.put("sysUser", umsAdmin);
            data.put("icon", umsAdmin.getIcon());
            data.put("userId", umsAdmin.getId());
            data.put("storeId", umsAdmin.getStoreId());
            data.put("storeName", umsAdmin.getStoreName());
        }

        return new CommonResult().success(data);
    }

    @SysLog(MODULE = "sys", REMARK = "登出功能")
    @ApiOperation(value = "登出功能")
    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    @ResponseBody
    public Object logout() {
        sysUserService.removePermissRedis(UserUtils.getCurrentMember().getId());
        redisService.remove(String.format(Rediskey.user, UserUtils.getCurrentMember().getUsername()));
        return new CommonResult().success(null);
    }

    @SysLog(MODULE = "sys", REMARK = "给用户分配角色")
    @ApiOperation("给用户分配角色")
    @RequestMapping(value = "/role/update", method = RequestMethod.POST)
    @ResponseBody
    public Object updateRole(@RequestParam("adminId") Long adminId,
                             @RequestParam("roleIds") List<Long> roleIds) {
        int count = sysUserService.updateUserRole(adminId, roleIds);
        if (count >= 0) {
            //更新，删除时候，如果redis里有权限列表，重置
            if (!redisService.exists(String.format(Rediskey.menuList, adminId))) {
                List<SysPermission> list = permissionMapper.listUserPerms(adminId);
                String key = String.format(Rediskey.menuList, adminId);
                redisService.set(key, JsonUtil.objectToJson(list));
                return list;
            }
            return new CommonResult().success(count);
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "sys", REMARK = "获取指定用户的角色")
    @ApiOperation("获取指定用户的角色")
    @RequestMapping(value = "/role/{adminId}", method = RequestMethod.GET)
    @ResponseBody
    public Object getRoleList(@PathVariable Long adminId) {
        List<SysRole> roleList = sysUserService.getRoleListByUserId(adminId);
        return new CommonResult().success(roleList);
    }

    @SysLog(MODULE = "sys", REMARK = "获取指定用户的角色")
    @ApiOperation("获取指定用户的角色")
    @RequestMapping(value = "/userRoleCheck", method = RequestMethod.GET)
    @ResponseBody
    public Object userRoleCheck(@RequestParam("adminId") Long adminId) {
        List<SysRole> roleList = sysUserService.getRoleListByUserId(adminId);
        List<SysRole> allroleList = roleService.list(new QueryWrapper<>());
        if (roleList != null && roleList.size() > 0) {
            for (SysRole a : allroleList) {
                for (SysRole u : roleList) {
                    if (u != null && u.getRoleId() != null) {
                        if (a.getRoleId().equals(u.getRoleId())) {
                            a.setChecked(true);
                        }
                    }
                }
            }
            return new CommonResult().success(allroleList);
        }
        return new CommonResult().success(allroleList);
    }

    @SysLog(MODULE = "sys", REMARK = "给用户分配+-权限")
    @ApiOperation("给用户分配+-权限")
    @RequestMapping(value = "/permission/update", method = RequestMethod.POST)
    @ResponseBody
    public Object updatePermission(@RequestParam Long adminId,
                                   @RequestParam("permissionIds") List<Long> permissionIds) {
        int count = sysUserService.updatePermissionByUserId(adminId, permissionIds);
        if (count > 0) {
            return new CommonResult().success(count);
        }
        return new CommonResult().failed();
    }

    @SysLog(MODULE = "sys", REMARK = "获取用户所有权限（包括+-权限）")
    @ApiOperation("获取用户所有权限（包括+-权限）")
    @RequestMapping(value = "/permission/{adminId}", method = RequestMethod.GET)
    @ResponseBody
    public Object getPermissionList(@PathVariable Long adminId) {
        List<SysPermission> permissionList = sysUserService.getPermissionListByUserId(adminId);
        return new CommonResult().success(permissionList);
    }

    @ApiOperation("修改展示状态")
    @RequestMapping(value = "/update/updateShowStatus", method = RequestMethod.POST)
    @ResponseBody
    @SysLog(MODULE = "sys", REMARK = "修改展示状态")
    public Object updateShowStatus(@RequestParam("ids") List<Long> ids,
                                   @RequestParam("showStatus") Integer showStatus) {
        SysUser role = new SysUser();
        role.setId(ids.get(0));
        role.setStatus(showStatus);
        sysUserService.updateById(role);

        return new CommonResult().success();

    }

    @ApiOperation("修改密码")
    @RequestMapping(value = "/updatePassword")
    @ResponseBody
    @SysLog(MODULE = "sys", REMARK = "修改密码")
    public Object updatePassword(@RequestParam("password") String password,
                                 @RequestParam("renewPassword") String renewPassword,
                                 @RequestParam("newPassword") String newPassword) {
        if (ValidatorUtils.empty(password)) {
            return new CommonResult().failed("参数为空");
        }
        if (ValidatorUtils.empty(renewPassword)) {
            return new CommonResult().failed("参数为空");
        }
        if (ValidatorUtils.empty(newPassword)) {
            return new CommonResult().failed("参数为空");
        }
        if (!renewPassword.equals(newPassword)) {
            return new CommonResult().failed("新密码不一致!");
        }
        try {
            sysUserService.updatePassword(password, newPassword);
        } catch (Exception e) {
            return new CommonResult().failed(e.getMessage());
        }
        return new CommonResult().success();

    }

    @SysLog(MODULE = "sys", REMARK = "获取用户的小区")
    @ApiOperation("获取相应角色权限")
    @RequestMapping(value = "/community/{userId}", method = RequestMethod.GET)
    @ResponseBody
    public Object communityList(@PathVariable Long userId) {
        List<UserCommunityRelate> permissionList = userCommunityRelateMapper.selectList(new QueryWrapper<UserCommunityRelate>().eq("user_id", userId));
        return new CommonResult().success(permissionList);
    }

    @SysLog(MODULE = "sys", REMARK = "获取用户的小区")
    @ApiOperation("获取相应角色权限")
    @RequestMapping(value = "/userCommunityRelate", method = RequestMethod.POST)
    @ResponseBody
    public Object userCommunityRelate(@RequestBody UserCommunityRelate entity) {
        return new CommonResult().success(sysUserService.userCommunityRelate(entity));
    }

    @SysLog(MODULE = "sys", REMARK = "获取用户的小区")
    @ApiOperation("获取相应角色权限")
    @RequestMapping(value = "/communityUser/{userId}", method = RequestMethod.GET)
    @ResponseBody
    public Object communityUser(@PathVariable Long userId) {
        List<UserCommunityRelate> permissionList = userCommunityRelateMapper.selectList(new QueryWrapper<UserCommunityRelate>().eq("user_id", userId));
        List<UserCommunityRelate> newList = new ArrayList<>();
        for (UserCommunityRelate relate : permissionList) {
            BuildingCommunity community = buildingCommunityMapper.selectById(relate.getCommunityId());
            if (community != null) {
                relate.setName(community.getName());
                newList.add(relate);
            }
        }
        return new CommonResult().success(newList);
    }

    @PutMapping("/resetPwd")
    public Object resetPwd(@RequestBody SysUser user) {
        return sysUserService.resetPwd(user);
    }

    /**
     * 发送短信验证码
     *
     * @param phone
     * @return
     */
    @IgnoreAuth
    @ApiOperation("获取验证码- 模板 - 区分忘记密码/修改手机号/")
    @PostMapping(value = "/sms/codes")
    public Object sendSmsCode(@RequestParam String phone,@RequestParam String type) {
        try {
            if (!PhoneUtil.checkPhone(phone)) {
                throw new IllegalArgumentException("手机号格式不正确");
            }
            if (ValidatorUtils.empty(type)){
                throw new IllegalArgumentException("验证码类型不能为空！");
            }
            SmsCode smsCode = sysUserService.generateCode(phone,type);

            return new CommonResult().success(smsCode);
        } catch (Exception e) {
            e.printStackTrace();
            return new CommonResult().failed(e.getMessage());
        }
    }

    @IgnoreAuth
    @ApiOperation("重置密码-忘记密码发送短信验证码")
    @PostMapping(value = "/resetPassword")
    public Object resetPassword(@RequestParam String phone,
                                @RequestParam String password,
                                @RequestParam String confimpassword,
                                @RequestParam String authCode) {
        if (phone == null || "".equals(phone)) {
            return new CommonResult().validateFailed("用户名或密码错误");
        }
        if (password == null || "".equals(password)) {
            return new CommonResult().validateFailed("用户名或密码错误");
        }
        if (confimpassword == null || "".equals(confimpassword)) {
            return new CommonResult().validateFailed("用户名或密码错误");
        }
        return sysUserService.resetPassword(phone, password, confimpassword, authCode);
    }

    @ApiOperation("修改用户名称")
    @PostMapping(value = "/resetUsername")
    @PreAuthorize("hasAuthority('sys:user:resetUsername')")
    public Object resetUsername(@RequestParam String username, @RequestParam Long id) {
        if (StringUtils.isNotBlank(username)||id==null){
            return new CommonResult().failed("参数不能为空！");
        }
        sysUserService.updateUsernameById(username,id);
        return new CommonResult().success();
    }

    @ApiOperation("修改手机号-经销商")
    @PostMapping(value = "/resetPhone")
    @PreAuthorize("hasAuthority('sys:user:resetPhone')")
    public Object resetPhone(@RequestParam String oldPhone,
                                @RequestParam String newPhone,
                                @RequestParam Long id,
                                @RequestParam String authCode) {
        if (oldPhone == null || "".equals(oldPhone)) {
            return new CommonResult().validateFailed("原手机号不能为空！");
        }
        if (newPhone == null || "".equals(newPhone)) {
            return new CommonResult().validateFailed("新手机号不能为空！");
        }
        if ( authCode== null || "".equals(authCode)) {
            return new CommonResult().validateFailed("验证码不能为空！");
        }
        if (oldPhone.equals(newPhone)){
            return new CommonResult().validateFailed("新旧手机号不能一致！");
        }
        return sysUserService.updatePhoneById(oldPhone, newPhone, authCode,id);
    }

    @ApiOperation("修改下级经销商申请状态-经销商")
    @PutMapping(value = "/resetApplyStatus")
    @PreAuthorize("hasAuthority('sys:user:resetApplyStatus')")
    public Object resetApplyStatus(@RequestBody SysUser user) {
        if (user.getId()==null||user.getApplyStatus()==null){
            return new CommonResult().failed("参数不能为空！");
        }
        sysUserService.updateById(user);
        return new CommonResult().success("操作成功！");
    }

    @ApiOperation("获取经销商的来源-经销商")
    @GetMapping(value = "/getOriginByUniacid")
    public Object getOriginByUniacid(@RequestParam Integer uniacid) {
        if (uniacid==null){
            return new CommonResult().success();
        }
        JSONObject j = new JSONObject();
        AccountWechats accountWechats = new AccountWechats();
        accountWechats.setUniacid(uniacid);
        AccountWechats wechats = wechatsService.getOne(new QueryWrapper<>(accountWechats));
        if (wechats!=null){
            j.put("name",wechats.getName());
            SysUser user = sysUserService.getById(wechats.getCreateBy());
            if (user.getLevel()==1){
                j.put("gName",user.getUsername());
            }else if (user.getLevel()==2){
                j.put("pName",user.getUsername());
                SysUser sysUser = sysUserService.getById(user.getPid());
                j.put("gName",sysUser.getUsername());
            }else if (user.getLevel()==3){
                j.put("sName",user.getUsername());
                SysUser user1 = sysUserService.getById(user.getPid());
                j.put("pName",user1.getUsername());
                SysUser user2 = sysUserService.getById(user.getGid());
                j.put("gName",user2.getUsername());
            }
            return new CommonResult().success(j);
        }
        AccountWxapp accountWxapp = new AccountWxapp();
        accountWxapp.setUniacid(uniacid);
        AccountWxapp wxapp = wxappService.getOne(new QueryWrapper<>(accountWxapp));
        if (wxapp==null){
            return new CommonResult().success();
        }
        j.put("name",wxapp.getName());
        SysUser user = sysUserService.getById(wxapp.getCreateBy());
        if (user.getLevel()==1){
            j.put("gName",user.getUsername());
        }else if (user.getLevel()==2){
            j.put("pName",user.getUsername());
            SysUser sysUser = sysUserService.getById(user.getPid());
            j.put("gName",sysUser.getUsername());
        }else if (user.getLevel()==3){
            j.put("sName",user.getUsername());
            SysUser user1 = sysUserService.getById(user.getPid());
            j.put("pName",user1.getUsername());
            SysUser user2 = sysUserService.getById(user.getGid());
            j.put("gName",user2.getUsername());
        }
        return new CommonResult().success(j);
    }
}

