package com.zscat.mallplus.config;


import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.core.parser.ISqlParser;
import com.baomidou.mybatisplus.core.parser.ISqlParserFilter;
import com.baomidou.mybatisplus.core.parser.SqlParserHelper;
import com.baomidou.mybatisplus.extension.injector.LogicSqlInjector;
import com.baomidou.mybatisplus.extension.injector.LogicSqlInjector;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.plugins.PerformanceInterceptor;
import com.baomidou.mybatisplus.extension.plugins.tenant.TenantHandler;
import com.baomidou.mybatisplus.extension.plugins.tenant.TenantSqlParser;
import com.zscat.mallplus.enums.ConstansValue;
import com.zscat.mallplus.sys.entity.SysUser;
import com.zscat.mallplus.sys.service.ISysUserService;
import com.zscat.mallplus.util.JwtTokenUtil;
import com.zscat.mallplus.util.UserUtils;
import com.zscat.mallplus.utils.ValidatorUtils;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

//Spring boot方式
@EnableTransactionManagement
@Configuration
@MapperScan("com.zscat.mallplus.*.mapper*")
public class MybatisPlusConfig {
    private static final List<String> IGNORE_TENANT_TABLES = ConstansValue.IGNORE_TENANT_TABLES;
    @Autowired
    private ISysUserService userMapper;

    /**
     * 分页插件
     */
    @Bean
    public PaginationInterceptor paginationInterceptor() {

        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();

        paginationInterceptor.setDialectType("mysql");
        /*
         * 【测试多租户】 SQL 解析处理拦截器<br>
         * 这里固定写成住户 1 实际情况你可以从cookie读取，因此数据看不到 【 麻花藤 】 这条记录（ 注意观察 SQL ）<br>
         */
        List<ISqlParser> sqlParserList = new ArrayList<>();
        TenantSqlParser tenantSqlParser = new TenantSqlParser();
        tenantSqlParser.setTenantHandler(new TenantHandler() {

            @Override
            public Expression getTenantId() {
                // 从当前系统上下文中取出当前请求的服务商ID，通过解析器注入到SQL中。
                System.out.println(UserUtils.getCurrentMember());
                Integer storeId = UserUtils.getCurrentMember().getStoreId();
                if (null == storeId) {
                    JwtTokenUtil jwtTokenUtil = new JwtTokenUtil();
                    RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
                    HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
                    String requestType = ((HttpServletRequest) request).getMethod();
                    if ("OPTIONS".equals(requestType)) {
                        return null;
                    }
                    String tokenPre = "Authorization";
                    String authHeader = request.getParameter(tokenPre);
                    if (ValidatorUtils.empty(authHeader)) {
                        authHeader = request.getHeader(tokenPre);
                     }
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String authToken = authHeader.substring("Bearer ".length());
                        String username = jwtTokenUtil.getUserNameFromToken(authToken);
                        if (ValidatorUtils.notEmpty(username)) {
                            SysUser member = userMapper.selectByUserName(username);
                            storeId = member.getStoreId();
                        }
                    }
                    if (null == storeId) {
                        //  storeId = 1;
                        System.out.println("#1129 getCurrentProviderId error.");
                        throw new RuntimeException("#1129 getCurrentProviderId error.");
                    }

                }
                return new LongValue(storeId);
            }

            @Override
            public String getTenantIdColumn() {
                return "store_id";
            }

            @Override
            public boolean doTableFilter(String tableName) {
                if (tableName.startsWith("cms") || tableName.startsWith("build") || tableName.startsWith("admin_")
                        || tableName.startsWith("QRTZ_") || tableName.startsWith("wt_sim_url_info")
                ||tableName.equals("merchat_facilitator_config")||tableName.equals("merchant_bank_info")
                        ||tableName.startsWith("sms_label")|| tableName.startsWith("sys_qiniu_config")
                        || tableName.startsWith("sys_qiniu_content")|| tableName.startsWith("wt_water_card_create")
                ||tableName.equals("ums_integration_consume_setting")
                         ||tableName.startsWith("wt_equipment_warter_card")|| tableName.startsWith("sys_dict")
                        || tableName.startsWith("ums_member_recommend")
                ||tableName.equals("sys_user_permission")){
                    return true;
                }
                return IGNORE_TENANT_TABLES.stream().anyMatch((e) -> e.equalsIgnoreCase(tableName));

            }
        });

        sqlParserList.add(tenantSqlParser);
        paginationInterceptor.setSqlParserList(sqlParserList);
        paginationInterceptor.setSqlParserFilter(new ISqlParserFilter() {
            @Override
            public boolean doFilter(MetaObject metaObject) {
                MappedStatement ms = SqlParserHelper.getMappedStatement(metaObject);
                // 过滤自定义查询此时无租户信息约束【 麻花藤 】出现
                if ("com.zscat.mallplus.sys.mapper.SysUserMapper.selectByUserName".equals(ms.getId())) {
                    return true;
                }
                if ("com.zscat.mallplus.jifen.mapper.JifenDealerIntegrationChangeHistoryMapper.selectBusinessRecord".equals(ms.getId())){
                    return true;
                }
                if ("com.zscat.mallplus.ums.mapper.UmsIntegrationChangeHistoryMapper.selectMemberRecord".equals(ms.getId())){
                    return true;
                }
                Integer storeId = UserUtils.getCurrentMember().getStoreId();
                if (storeId != null && storeId == 0) {
                    return true;
                }
                return false;
            }
        });
        return paginationInterceptor;
    }


    /**
     * 性能分析拦截器，不建议生产使用
     * 用来观察 SQL 执行情况及执行时长
     */
    @Bean
    public PerformanceInterceptor performanceInterceptor() {
        return new PerformanceInterceptor();
    }

    @Bean
    public ISqlInjector sqlInjector() {
        return new LogicSqlInjector();
    }
}
