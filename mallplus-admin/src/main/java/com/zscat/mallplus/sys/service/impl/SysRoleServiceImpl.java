package com.zscat.mallplus.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zscat.mallplus.sys.entity.SysPermission;
import com.zscat.mallplus.sys.entity.SysRole;
import com.zscat.mallplus.sys.entity.SysRolePermission;
import com.zscat.mallplus.sys.mapper.SysPermissionMapper;
import com.zscat.mallplus.sys.mapper.SysRoleMapper;
import com.zscat.mallplus.sys.mapper.SysRolePermissionMapper;
import com.zscat.mallplus.sys.service.ISysRolePermissionService;
import com.zscat.mallplus.sys.service.ISysRoleService;
import com.zscat.mallplus.sys.service.ISysUserService;
import com.zscat.mallplus.util.UserUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;

/**
 * <p>
 * 后台用户角色表 服务实现类
 * </p>
 *
 * @author zscat
 * @since 2019-04-14
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    @Resource
    private SysPermissionMapper permissionMapper;
    @Resource
    private SysRolePermissionMapper rolePermissionMapper;
    @Resource
    private SysRoleMapper roleMapper;
    @Resource
    private SysRolePermissionMapper rolePermissionRelationMapper;
    @Resource
    private ISysRolePermissionService rolePermissionRelationDao;
    @Resource
    private ISysUserService userService;

    @Override
    public List<SysPermission> getPermissionList(Long roleId) {
        return permissionMapper.getPermissionList(roleId);
    }

    @Override
    public List<SysRolePermission> getRolePermission(Long roleId) {
        return rolePermissionMapper.selectList(new QueryWrapper<SysRolePermission>().eq("role_id", roleId));
    }

    @Override
    public Set<String> selectRolePermissionByUserId(Long userId) {
        List<SysRole> perms = roleMapper.selectRolePermissionByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (SysRole perm : perms) {
            if (perm != null) {
                //  permsSet.addAll(Arrays.asList(perm.getRoleKey().trim().split(",")));
            }
        }
        return permsSet;
    }

    @Transactional
    @Override
    public boolean saves(SysRole role) {
        role.setCreateTime(new Date());
        role.setStatus(1);
//        role.setAdminCount(0);
        role.setRoleSort(0);
        roleMapper.insert(role);
        updatePermission(role.getRoleId(), role.getMenuIds());
        return true;
    }

    @Transactional
    @Override
    public boolean updates(SysRole role) {
        role.setRoleId(role.getRoleId());
        updatePermission(role.getRoleId(), role.getMenuIds());
        roleMapper.updateById(role);
        return true;
    }

    public void updatePermission(Long roleId, String permissionIds) {

        //先删除原有关系
        rolePermissionRelationMapper.delete(new QueryWrapper<SysRolePermission>().eq("role_id", roleId));
        //批量插入新关系
        List<SysRolePermission> relationList = new ArrayList<>();
        if (!StringUtils.isEmpty(permissionIds)) {
            String[] mids = permissionIds.split(",");
            for (String permissionId : mids) {
                SysRolePermission relation = new SysRolePermission();
                relation.setRoleId(roleId);
                relation.setPermissionId(Long.valueOf(permissionId));
                relationList.add(relation);
            }
            rolePermissionRelationDao.saveBatch(relationList);
        }
        userService.removePermissRedis(UserUtils.getCurrentMember().getId());
    }
}
