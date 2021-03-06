package com.zscat.mallplus.merchant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zscat.mallplus.elias.config.CertPathConfig;
import com.zscat.mallplus.encrypt.EncryptSensitive;
import com.zscat.mallplus.merchant.entity.MerchantBankInfo;
import com.zscat.mallplus.merchant.entity.MerchatBusinessMaterials;
import com.zscat.mallplus.merchant.mapper.MerchantBankInfoMapper;
import com.zscat.mallplus.merchant.mapper.MerchatBusinessMaterialsMapper;
import com.zscat.mallplus.merchant.service.IMerchatBusinessMaterialsService;
import com.zscat.mallplus.merchat.utils.MerchantUtil;
import com.zscat.mallplus.sdk.util.PemUtil;
import com.zscat.mallplus.util.StringUtils;
import com.zscat.mallplus.utils.ValidatorUtils;
import net.sf.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.crypto.IllegalBlockSizeException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mallplus
 * @date 2020-05-14
 */
@Service
public class MerchatBusinessMaterialsServiceImpl extends ServiceImpl
        <MerchatBusinessMaterialsMapper, MerchatBusinessMaterials> implements IMerchatBusinessMaterialsService {

    @Resource
    private MerchatBusinessMaterialsMapper merchatBusinessMaterialsMapper;
    @Resource
    private MerchantBankInfoMapper merchantBankInfoMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String queryMax(String tableName,String columnName) {
        String prefix = MerchantUtil.receptionPrefix;
        String sql = "SELECT\n" +
                columnName  +
                "        FROM\n" +
                tableName +
                "        WHERE\n" +
                "        create_time >= date( now( ) )\n" +
                "        AND create_time < DATE_ADD( date( now( ) ), INTERVAL 1 DAY )\n" +
                "        ORDER BY create_time Desc\n" +
                "        LIMIT 0,1";
        List<Map<String,Object>> map = jdbcTemplate.queryForList(sql);
        String maxString = null;
        if (map!=null&&map.size()!=0){
            maxString = map.get(0).get("business_code").toString();
        }
        String equipmentType = prefix  + System.currentTimeMillis();
        String equipmentServiceNo = MerchantUtil.createSelfIncrement(equipmentType,maxString);
        if (equipmentServiceNo == null || equipmentServiceNo.equals("")){
            return null;
        }
        return equipmentServiceNo;
    }

    @Override
    public Map<String, Object> getBody(MerchatBusinessMaterials merchatBusinessMaterials,String certPath) throws IOException, IllegalBlockSizeException {
        InputStream inputStream = new FileInputStream(certPath);
        X509Certificate certificate = PemUtil.loadCertificate(inputStream);
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("business_code", merchatBusinessMaterials.getBusinessCode());
        //1.超级管理员信息
        Map<String,Object> contact_info = new HashMap<>();
        contact_info.put("contact_name",EncryptSensitive.rsaEncryptOAEP(merchatBusinessMaterials.getContactName(),certificate));
        //“超级管理员身份证号码”与“超级管理员微信openid”，二选一必填。
        if (StringUtils.isNotBlank(merchatBusinessMaterials.getContactIdNumber())|| StringUtils.isNotBlank(merchatBusinessMaterials.getOpenid())){
            if (StringUtils.isNotBlank(merchatBusinessMaterials.getContactIdNumber())){
                contact_info.put("contact_id_number",EncryptSensitive.rsaEncryptOAEP(merchatBusinessMaterials.getContactIdNumber(),certificate));
            }else {
                contact_info.put("openid",EncryptSensitive.rsaEncryptOAEP(merchatBusinessMaterials.getOpenid(),certificate));
            }
        }else {
            return null;
        }
        contact_info.put("mobile_phone",EncryptSensitive.rsaEncryptOAEP(merchatBusinessMaterials.getMobilePhone(),certificate));
        contact_info.put("contact_email",EncryptSensitive.rsaEncryptOAEP(merchatBusinessMaterials.getContactEmail(),certificate));
        requestMap.put("contact_info",contact_info);
        //2.主体资料
        Map<String,Object> subject_info = new HashMap<>();
        subject_info.put("subject_type",merchatBusinessMaterials.getSubjectType());
        if ("SUBJECT_TYPE_INDIVIDUAL".equals(merchatBusinessMaterials.getSubjectType())) {
            Map<String, Object> business_license_info = new HashMap<>();
            business_license_info.put("license_copy",merchatBusinessMaterials.getLicenseCopyMediaId());
            business_license_info.put("license_number",merchatBusinessMaterials.getLicenseNumber());
            business_license_info.put("merchant_name",merchatBusinessMaterials.getSubjectMerchantName());
            business_license_info.put("legal_person",merchatBusinessMaterials.getSubjectLegalPerson());
            subject_info.put("business_license_info",business_license_info);
        }else if ("SUBJECT_TYPE_ENTERPRISE".equals(merchatBusinessMaterials.getSubjectType())){
            Map<String, Object> business_license_info = new HashMap<>();
            business_license_info.put("license_copy",merchatBusinessMaterials.getLicenseCopyMediaId());
            business_license_info.put("license_number",merchatBusinessMaterials.getLicenseNumber());
            business_license_info.put("merchant_name",merchatBusinessMaterials.getSubjectMerchantName());
            business_license_info.put("legal_person",merchatBusinessMaterials.getSubjectLegalPerson());
            subject_info.put("business_license_info",business_license_info);
            Map<String,Object> organization_info = new HashMap<>();
            organization_info.put("organization_copy",merchatBusinessMaterials.getOrganizationCopyMediaId());
            organization_info.put("organization_code",merchatBusinessMaterials.getOrganizationCode());
            organization_info.put("org_period_begin",merchatBusinessMaterials.getOrgPeriodBegin());
            organization_info.put("org_period_end",merchatBusinessMaterials.getOrgPeriodEnd());
            subject_info.put("organization_info",organization_info);
        }else if ("SUBJECT_TYPE_INSTITUTIONS".equals(merchatBusinessMaterials.getSubjectType())){
            Map<String, Object> certificate_info = new HashMap<>();
            certificate_info.put("cert_copy",merchatBusinessMaterials.getCertCopyMediaId());
            certificate_info.put("cert_type",merchatBusinessMaterials.getCertType());
            certificate_info.put("cert_number",merchatBusinessMaterials.getCertNumber());
            certificate_info.put("merchant_name",merchatBusinessMaterials.getMerchantName());
            certificate_info.put("company_address",merchatBusinessMaterials.getCompanyAddress());
            certificate_info.put("legal_person",merchatBusinessMaterials.getLegalPerson());
            certificate_info.put("period_begin",merchatBusinessMaterials.getPeriodBegin());
            certificate_info.put("period_end",merchatBusinessMaterials.getPeriodEnd());
            subject_info.put("certificate_info",certificate_info);
            Map<String,Object> organization_info = new HashMap<>();
            organization_info.put("organization_copy",merchatBusinessMaterials.getOrganizationCopyMediaId());
            organization_info.put("organization_code",merchatBusinessMaterials.getOrganizationCode());
            organization_info.put("org_period_begin",merchatBusinessMaterials.getOrgPeriodBegin());
            organization_info.put("org_period_end",merchatBusinessMaterials.getOrgPeriodEnd());
            subject_info.put("organization_info",organization_info);
            subject_info.put("certificate_letter_copy",merchatBusinessMaterials.getCertificateLetterCopyMediaId());
        }else if ("SUBJECT_TYPE_OTHERS".equals(merchatBusinessMaterials.getSubjectType())){
            Map<String, Object> certificate_info = new HashMap<>();
            certificate_info.put("cert_copy",merchatBusinessMaterials.getCertCopyMediaId());
            certificate_info.put("cert_type",merchatBusinessMaterials.getCertType());
            certificate_info.put("cert_number",merchatBusinessMaterials.getCertNumber());
            certificate_info.put("merchant_name",merchatBusinessMaterials.getMerchantName());
            certificate_info.put("company_address",merchatBusinessMaterials.getCompanyAddress());
            certificate_info.put("legal_person",merchatBusinessMaterials.getLegalPerson());
            certificate_info.put("period_begin",merchatBusinessMaterials.getPeriodBegin());
            certificate_info.put("period_end",merchatBusinessMaterials.getPeriodEnd());
            subject_info.put("certificate_info",certificate_info);
            Map<String,Object> organization_info = new HashMap<>();
            organization_info.put("organization_copy",merchatBusinessMaterials.getOrganizationCopyMediaId());
            organization_info.put("organization_code",merchatBusinessMaterials.getOrganizationCode());
            organization_info.put("org_period_begin",merchatBusinessMaterials.getOrgPeriodBegin());
            organization_info.put("org_period_end",merchatBusinessMaterials.getOrgPeriodEnd());
            subject_info.put("organization_info",organization_info);
        }
        subject_info.put("certificate_letter_copy",merchatBusinessMaterials.getCertificateLetterCopyMediaId());
        Map<String,Object> identity_info = new HashMap<>();
        identity_info.put("id_doc_type",merchatBusinessMaterials.getIdDocType());
        if ("IDENTIFICATION_TYPE_IDCARD".equals(merchatBusinessMaterials.getIdDocType())){
            Map<String,Object> id_card_info = new HashMap<>();
            id_card_info.put("id_card_copy",merchatBusinessMaterials.getIdCardCopyMediaId());
            id_card_info.put("id_card_national",merchatBusinessMaterials.getIdCardNationalMediaId());
            id_card_info.put("id_card_name",EncryptSensitive.rsaEncryptOAEP(merchatBusinessMaterials.getIdCardName(),certificate));
            id_card_info.put("id_card_number",EncryptSensitive.rsaEncryptOAEP(merchatBusinessMaterials.getIdCardNumber(),certificate));
            id_card_info.put("card_period_begin",MerchantUtil.dateToString(merchatBusinessMaterials.getCardPeriodBegin()));
            id_card_info.put("card_period_end",MerchantUtil.dateToString(merchatBusinessMaterials.getCardPeriodEnd()));
            identity_info.put("id_card_info",id_card_info);
        }else {
            Map<String,Object> id_doc_info = new HashMap<>();
            id_doc_info.put("id_doc_copy",merchatBusinessMaterials.getIdDocCopyMediaId());
            id_doc_info.put("id_doc_name",EncryptSensitive.rsaEncryptOAEP(merchatBusinessMaterials.getIdDocName(),certificate));
            id_doc_info.put("id_doc_number",EncryptSensitive.rsaEncryptOAEP(merchatBusinessMaterials.getIdDocNumber(),certificate));
            id_doc_info.put("doc_period_begin",MerchantUtil.dateToString(merchatBusinessMaterials.getDocPeriodBegin()));
            id_doc_info.put("doc_period_end",MerchantUtil.dateToString(merchatBusinessMaterials.getDocPeriodEnd()));
            identity_info.put("id_card_info",id_doc_info);
        }
        identity_info.put("owner",MerchantUtil.intToBool(merchatBusinessMaterials.getOwner()));
        subject_info.put("identity_info",identity_info);
        if (merchatBusinessMaterials.getOwner()==0){
            Map<String,Object> ubo_info = new HashMap<>();
            ubo_info.put("id_type",merchatBusinessMaterials.getIdType());
            if ("IDENTIFICATION_TYPE_IDCARD".equals(merchatBusinessMaterials.getIdType())){
                ubo_info.put("id_card_copy",merchatBusinessMaterials.getUboIdCardCopyMediaId());
                ubo_info.put("id_card_national",merchatBusinessMaterials.getUboIdCardNationalMediaId());
            }else {
                ubo_info.put("id_doc_copy",merchatBusinessMaterials.getUboIdDocCopyMediaId());
            }
            ubo_info.put("name",EncryptSensitive.rsaEncryptOAEP(merchatBusinessMaterials.getUboName(),certificate));
            ubo_info.put("id_number",EncryptSensitive.rsaEncryptOAEP(merchatBusinessMaterials.getUboIdNumber(),certificate));
            ubo_info.put("id_period_begin",MerchantUtil.dateToString(merchatBusinessMaterials.getUboIdPeriodBegin()));
            ubo_info.put("id_period_end",MerchantUtil.dateToString(merchatBusinessMaterials.getUboIdPeriodEnd()));
            subject_info.put("ubo_info",ubo_info);
        }
        requestMap.put("subject_info",subject_info);
        Map<String,Object> business_info = new HashMap<>();
        business_info.put("merchant_shortname",merchatBusinessMaterials.getMerchantShortname());
        business_info.put("service_phone",merchatBusinessMaterials.getServicePhone());
        Map<String,Object> sales_info = new HashMap<>();
        List<String> list = Arrays.asList(merchatBusinessMaterials.getSalesScenesType().split(","));
        sales_info.put("sales_scenes_type", JSONArray.fromObject(list));
        for (String type:list){
            if ("SALES_SCENES_STORE".equals(type)){
                Map<String,Object> biz_store_info = new HashMap<>();
                biz_store_info.put("biz_store_name",merchatBusinessMaterials.getBizStoreName());
                biz_store_info.put("biz_address_code",merchatBusinessMaterials.getBizAddressCode());
                biz_store_info.put("biz_store_address",merchatBusinessMaterials.getBizStoreAddress());
                List<String> store_entrance_pic = Arrays.asList(merchatBusinessMaterials.getStoreEntrancePicMediaId().split(","));
                biz_store_info.put("store_entrance_pic",JSONArray.fromObject(store_entrance_pic));
                List<String> indoor_pic = Arrays.asList(merchatBusinessMaterials.getIndoorPicMediaId().split(","));
                biz_store_info.put("indoor_pic",JSONArray.fromObject(indoor_pic));
                if (StringUtils.isNotBlank(merchatBusinessMaterials.getBizSubAppid())){
                    biz_store_info.put("biz_sub_appid ",merchatBusinessMaterials.getBizSubAppid());
                }
                sales_info.put("biz_store_info",biz_store_info);
            }else if ("SALES_SCENES_MP".equals(type)){
                Map<String,Object> mp_info = new HashMap<>();
                if (StringUtils.isNotBlank(merchatBusinessMaterials.getMpAppid())|| StringUtils.isNotBlank(merchatBusinessMaterials.getMpSubAppid())){
                    if (StringUtils.isNotBlank(merchatBusinessMaterials.getMpAppid())){
                        mp_info.put("mp_appid",merchatBusinessMaterials.getMpAppid());
                    }else {
                        mp_info.put("mp_sub_appid ",merchatBusinessMaterials.getMpSubAppid());
                    }
                }if (StringUtils.isNotBlank(merchatBusinessMaterials.getMpPicsMediaId())) {
                    List<String> mpPicsList = Arrays.asList(merchatBusinessMaterials.getMpPicsMediaId().split(","));
                    mp_info.put("mp_pics", JSONArray.fromObject(mpPicsList));
                }
                sales_info.put("mp_info",mp_info);
            }else if ("SALES_SCENES_MINI_PROGRAM".equals(type)){
                Map<String,Object> mini_program_infov = new HashMap<>();
                if (StringUtils.isNotBlank(merchatBusinessMaterials.getMiniProgramAppid())|| StringUtils.isNotBlank(merchatBusinessMaterials.getMiniProgramSubAppid())){
                    if (StringUtils.isNotBlank(merchatBusinessMaterials.getMiniProgramAppid())){
                        mini_program_infov.put("mini_program_appid",merchatBusinessMaterials.getMiniProgramAppid());
                    }else {
                        mini_program_infov.put("mini_program_sub_appid",merchatBusinessMaterials.getMiniProgramSubAppid());
                    }
                }if (StringUtils.isNotBlank(merchatBusinessMaterials.getMiniProgramPicsMediaId())) {
                    List<String> mini_program_pics = Arrays.asList(merchatBusinessMaterials.getMiniProgramPicsMediaId().split(","));
                    mini_program_infov.put("mini_program_pics",JSONArray.fromObject(mini_program_pics));
                }
                sales_info.put("mini_program_infov",mini_program_infov);
            }else if ("SALES_SCENES_WEB".equals(type)){
                Map<String,Object> web_info = new HashMap<>();
                web_info.put("domain",merchatBusinessMaterials.getDomain());
                if (StringUtils.isNotBlank(merchatBusinessMaterials.getWebAuthorisationMediaId())){
                    web_info.put("web_authorisation",merchatBusinessMaterials.getWebAuthorisationMediaId());
                }
                if (StringUtils.isNotBlank(merchatBusinessMaterials.getWebAppid())) {
                    web_info.put("web_appid", merchatBusinessMaterials.getWebAppid());
                }
                sales_info.put("web_info",web_info);
            }else if ("SALES_SCENES_APP".equals(type)){
                Map<String,Object> app_info = new HashMap<>();
                if (StringUtils.isNotBlank(merchatBusinessMaterials.getAppAppid())|| StringUtils.isNotBlank(merchatBusinessMaterials.getAppSubAppid())){
                    if (StringUtils.isNotBlank(merchatBusinessMaterials.getAppAppid())){
                        app_info.put("app_appid",merchatBusinessMaterials.getAppAppid());
                    }else {
                        app_info.put("app_sub_appid",merchatBusinessMaterials.getAppSubAppid());
                    }
                }
                app_info.put("app_pics",merchatBusinessMaterials.getAppPicsMediaId());
                sales_info.put("app_info",app_info);
            } else if ("SALES_SCENES_WEWORK".equals(type)){
                Map<String,Object> wework_info = new HashMap<>();
               wework_info.put("sub_corp_id ",merchatBusinessMaterials.getSubCorpId());
               List<String> wework_pics = Arrays.asList(merchatBusinessMaterials.getWeworkPicsMediaId().split(","));
                wework_info.put("wework_pics",wework_pics);
                sales_info.put("wework_info",wework_info);
            }
        }
        business_info.put("sales_info",sales_info);
        requestMap.put("business_info",business_info);
        Map<String,Object> settlement_info = new HashMap<>();
        settlement_info.put("settlement_id",merchatBusinessMaterials.getSettlementId());
        settlement_info.put("qualification_type",merchatBusinessMaterials.getQualificationType());
        if (StringUtils.isNotBlank(merchatBusinessMaterials.getQualificationsMediaId())){
            List<String> qualifications = Arrays.asList(merchatBusinessMaterials.getQualificationsMediaId().split(","));
            settlement_info.put("qualifications",qualifications);
        }
        if (StringUtils.isNotBlank(merchatBusinessMaterials.getActivitiesId())){
            settlement_info.put("activities_id",merchatBusinessMaterials.getActivitiesId());
        }
        if (StringUtils.isNotBlank(merchatBusinessMaterials.getActivitiesRate())){
            settlement_info.put("activities_rate",merchatBusinessMaterials.getActivitiesRate());
        }
        if (StringUtils.isNotBlank(merchatBusinessMaterials.getActivitiesAdditionsMedia())){
            List<String> activities_additions = Arrays.asList(merchatBusinessMaterials.getActivitiesAdditionsMedia().split(","));
            settlement_info.put("activities_additions",activities_additions);
        }
        requestMap.put("settlement_info",settlement_info);
        Map<String,Object> bank_account_info = new HashMap<>();
        bank_account_info.put("bank_account_type",merchatBusinessMaterials.getBankAccountType());
        bank_account_info.put("account_name",EncryptSensitive.rsaEncryptOAEP(merchatBusinessMaterials.getAccountName(),certificate));
        bank_account_info.put("account_bank",merchatBusinessMaterials.getAccountBank());
        bank_account_info.put("bank_address_code",merchatBusinessMaterials.getBankAddressCode());
        bank_account_info.put("account_number",EncryptSensitive.rsaEncryptOAEP(merchatBusinessMaterials.getAccountNumber(),certificate));
        if (StringUtils.isNotBlank(merchatBusinessMaterials.getBankBranchId())|| StringUtils.isNotBlank(merchatBusinessMaterials.getBankName())){
            if (StringUtils.isNotBlank(merchatBusinessMaterials.getBankBranchId())){
                bank_account_info.put("bank_branch_id",merchatBusinessMaterials.getBankBranchId());
            }else {
                bank_account_info.put("bank_name",merchatBusinessMaterials.getBankName());
            }
        }
        requestMap.put("bank_account_info",bank_account_info);
        if (StringUtils.isNotBlank(merchatBusinessMaterials.getLegalPersonCommitmentMediaId())
                || StringUtils.isNotBlank(merchatBusinessMaterials.getLegalPersonVideoMediaId())
                || StringUtils.isNotBlank(merchatBusinessMaterials.getBusinessAdditionPicsMediaId())
                || StringUtils.isNotBlank(merchatBusinessMaterials.getBusinessAdditionMsg())){
            Map<String,Object> addition_info = new HashMap<>();
            if (StringUtils.isNotBlank(merchatBusinessMaterials.getLegalPersonCommitmentMediaId())){
                addition_info.put("legal_person_commitment",merchatBusinessMaterials.getLegalPersonCommitmentMediaId());
            }
            if (StringUtils.isNotBlank(merchatBusinessMaterials.getLegalPersonVideoMediaId())){
                addition_info.put("legal_person_video",merchatBusinessMaterials.getLegalPersonVideoMediaId());
            }
            if (StringUtils.isNotBlank(merchatBusinessMaterials.getBusinessAdditionPicsMediaId())){
                List<String> business_addition_pics = Arrays.asList(merchatBusinessMaterials.getBusinessAdditionPicsMediaId());
                addition_info.put("business_addition_pics",business_addition_pics);
            }
            if (StringUtils.isNotBlank(merchatBusinessMaterials.getBusinessAdditionMsg())){
                addition_info.put("business_addition_msg",merchatBusinessMaterials.getBusinessAdditionMsg());
            }
            requestMap.put("addition_info",addition_info);
        }
        return requestMap;
    }

    @Override
    public Map<String, Object> validInfo(MerchatBusinessMaterials merchatBusinessMaterials) {
        Map<String, Object> map = new HashMap<String, Object>();
        if (ValidatorUtils.empty(merchatBusinessMaterials.getContactIdNumber())&&ValidatorUtils.empty(merchatBusinessMaterials.getOpenid())){
            map.put("success",false);
            map.put("message","超级管理员身份证件号码与超级管理员微信openid必须二选一！");
            return map;
        }
        if (merchatBusinessMaterials.getSubjectType().equals("SUBJECT_TYPE_INDIVIDUAL")){
            if (ValidatorUtils.empty(merchatBusinessMaterials.getLicenseCopyMediaId())||ValidatorUtils.empty(merchatBusinessMaterials.getLicenseNumber())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getSubjectMerchantName())||ValidatorUtils.empty(merchatBusinessMaterials.getSubjectLegalPerson())){
                map.put("success",false);
                map.put("message","主体类型是个体户必须填写营业执照！");
                return map;
            }
        }else if (merchatBusinessMaterials.getSubjectType().equals("SUBJECT_TYPE_ENTERPRISE")) {
            if (ValidatorUtils.empty(merchatBusinessMaterials.getLicenseCopy()) || ValidatorUtils.empty(merchatBusinessMaterials.getLicenseNumber())
                    || ValidatorUtils.empty(merchatBusinessMaterials.getSubjectMerchantName()) || ValidatorUtils.empty(merchatBusinessMaterials.getSubjectLegalPerson())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getOrganizationCopy())||ValidatorUtils.empty(merchatBusinessMaterials.getOrganizationCode())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getOrgPeriodBegin())||ValidatorUtils.empty(merchatBusinessMaterials.getOrgPeriodEnd())) {
                map.put("success",false);
                map.put("message","主体类型是企业必须填写营业执照和组织机构代码！");
                return map;
            }
        }else if (merchatBusinessMaterials.getSubjectType().equals("SUBJECT_TYPE_INSTITUTIONS")){
            if (ValidatorUtils.empty(merchatBusinessMaterials.getCertCopy())||ValidatorUtils.empty(merchatBusinessMaterials.getCertType())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getCertNumber())||ValidatorUtils.empty(merchatBusinessMaterials.getMerchantName())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getCompanyAddress())||ValidatorUtils.empty(merchatBusinessMaterials.getLegalPerson())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getPeriodBegin())||ValidatorUtils.empty(merchatBusinessMaterials.getPeriodEnd())
                    ||ValidatorUtils.empty(merchatBusinessMaterials.getOrganizationCopy())||ValidatorUtils.empty(merchatBusinessMaterials.getOrganizationCode())
                    ||ValidatorUtils.empty(merchatBusinessMaterials.getOrgPeriodBegin())||ValidatorUtils.empty(merchatBusinessMaterials.getOrgPeriodEnd())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getCertificateLetterCopyMediaId())){
                map.put("success",false);
                map.put("message","主体类型是党政、机关及事业单位必须填写登记证书、单位证明函照片和组织机构代码！");
                return map;
            }
        }else if (merchatBusinessMaterials.getSubjectType().equals("SUBJECT_TYPE_OTHERS")){
            if (ValidatorUtils.empty(merchatBusinessMaterials.getCertCopy())||ValidatorUtils.empty(merchatBusinessMaterials.getCertType())
                    ||ValidatorUtils.empty(merchatBusinessMaterials.getCertNumber())||ValidatorUtils.empty(merchatBusinessMaterials.getMerchantName())
                    ||ValidatorUtils.empty(merchatBusinessMaterials.getCompanyAddress())||ValidatorUtils.empty(merchatBusinessMaterials.getLegalPerson())
                    ||ValidatorUtils.empty(merchatBusinessMaterials.getPeriodBegin())||ValidatorUtils.empty(merchatBusinessMaterials.getPeriodEnd())
                    ||ValidatorUtils.empty(merchatBusinessMaterials.getOrganizationCopy())||ValidatorUtils.empty(merchatBusinessMaterials.getOrganizationCode())
                    ||ValidatorUtils.empty(merchatBusinessMaterials.getOrgPeriodBegin())||ValidatorUtils.empty(merchatBusinessMaterials.getOrgPeriodEnd())){
                map.put("success",false);
                map.put("message","主体类型是其他组织必须填写登记证书和组织机构代码！");
                return map;
            }
        }else if (merchatBusinessMaterials.getOwner()==0){
            if (ValidatorUtils.empty(merchatBusinessMaterials.getIdType())||ValidatorUtils.empty(merchatBusinessMaterials.getUboName())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getUboIdNumber())||ValidatorUtils.empty(merchatBusinessMaterials.getUboIdPeriodBegin())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getUboIdPeriodEnd())){
                map.put("success",false);
                map.put("message","若经营者/法人不是最终受益所有人，则需提填写受益所有人信息！");
                return map;
            }else if (merchatBusinessMaterials.getIdType().equals("IDENTIFICATION_TYPE_IDCARD")){
                if (ValidatorUtils.empty(merchatBusinessMaterials.getUboIdCardCopyMediaId())||ValidatorUtils.empty(merchatBusinessMaterials.getUboIdCardNationalMediaId())){
                    map.put("success",false);
                    map.put("message","受益人的证件类型为“身份证”时，上传身份证正反面照片！");
                    return map;
                }
            }else{
                if (ValidatorUtils.empty(merchatBusinessMaterials.getUboIdDocCopyMediaId())){
                    map.put("success",false);
                    map.put("message","受益人的证件类型为“来往内地通行证、来往大陆通行证、护照”时，上传证件照片！");
                    return map;
                }
            }
        }else if (merchatBusinessMaterials.getSalesScenesType().contains("SALES_SCENES_STORE")){
            if (ValidatorUtils.empty(merchatBusinessMaterials.getBizStoreName())||ValidatorUtils.empty(merchatBusinessMaterials.getBizStoreAddress())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getBizAddressCode())||ValidatorUtils.empty(merchatBusinessMaterials.getStoreEntrancePicMediaId())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getIndoorPicMediaId())){
                map.put("success",false);
                map.put("message","当“经营场景类型“选择“SALES_SCENES_STORE“，该场景资料必填！");
                return map;
            }
        }else if (merchatBusinessMaterials.getSalesScenesType().contains("SALES_SCENES_MP")){
            if (ValidatorUtils.empty(merchatBusinessMaterials.getMpAppid()) && ValidatorUtils.empty(merchatBusinessMaterials.getMpSubAppid())) {
                map.put("success",false);
                map.put("message","服务商公众号APPID与商家公众号APPID，二选一必填！");
                return map;
            }
        }else if (merchatBusinessMaterials.getSalesScenesType().contains("SALES_SCENES_MINI_PROGRAM")){
            if (ValidatorUtils.empty(merchatBusinessMaterials.getMiniProgramAppid())&&ValidatorUtils.empty(merchatBusinessMaterials.getMiniProgramSubAppid())){
                map.put("success",false);
                map.put("message","服务商小程序APPID与商家小程序APPID，二选一必填！");
                return map;
            }
        }else if (merchatBusinessMaterials.getSalesScenesType().contains("SALES_SCENES_WEB")){
            if (ValidatorUtils.empty(merchatBusinessMaterials.getDomain())){
                map.put("success",false);
                map.put("message","当“经营场景类型“选择”SALES_SCENES_WEB“，域名为必填项！");
                return map;
            }
        }else if (merchatBusinessMaterials.getSalesScenesType().contains("SALES_SCENES_APP")){
            if (ValidatorUtils.empty(merchatBusinessMaterials.getAppAppid())&&ValidatorUtils.empty(merchatBusinessMaterials.getAppSubAppid())){
                map.put("success",false);
                map.put("message","服务商应用APPID与商家应用APPID，二选一必填！");
                return map;
            }else if (ValidatorUtils.empty(merchatBusinessMaterials.getAppPicsMediaId())){
                map.put("success",false);
                map.put("message","请提供APP首页截图、尾页截图、应用内截图、支付页截图各1张！");
                return map;
            }
        }else if (merchatBusinessMaterials.getSalesScenesType().contains("SALES_SCENES_WEWORK")){
            if (ValidatorUtils.empty(merchatBusinessMaterials.getSubCorpId())||ValidatorUtils.empty(merchatBusinessMaterials.getWeworkPicsMediaId())){
                map.put("success",false);
                map.put("message","当“经营场景类型“选择”SALES_SCENES_WEWORK“，该场景资料必填！");
                return map;
            }
        } else if (merchantBankInfoMapper.selectOne(new QueryWrapper<MerchantBankInfo>().eq("bank_name",merchatBusinessMaterials.getAccountName()))==null){
            if (ValidatorUtils.empty(merchatBusinessMaterials.getBankBranchId())&&ValidatorUtils.empty(merchatBusinessMaterials.getBankName())) {
                map.put("success", false);
                map.put("message", "17家直连银行无需填写，如为其他银行，则开户银行全称（含支行）和开户银行联行号二选一！");
                return map;
            }
        } else if (merchatBusinessMaterials.getIdDocType().equals("IDENTIFICATION_TYPE_IDCARD")){
            if (ValidatorUtils.empty(merchatBusinessMaterials.getIdCardCopyMediaId())||ValidatorUtils.empty(merchatBusinessMaterials.getIdCardNationalMediaId())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getIdCardName())||ValidatorUtils.empty(merchatBusinessMaterials.getIdCardNumber())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getCardPeriodBegin())||ValidatorUtils.empty(merchatBusinessMaterials.getCardPeriodEnd())){
                map.put("success", false);
                map.put("message", "经营者/法人身份证件,证件类型为“身份证”时填写信息不完整！");
                return map;
            }
        }else {
            if (ValidatorUtils.empty(merchatBusinessMaterials.getIdDocCopyMediaId())||ValidatorUtils.empty(merchatBusinessMaterials.getIdDocName())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getIdDocNumber())||ValidatorUtils.empty(merchatBusinessMaterials.getDocPeriodBegin())
            ||ValidatorUtils.empty(merchatBusinessMaterials.getDocPeriodEnd())){
                map.put("success", false);
                map.put("message", "经营者/法人身份证件,证件类型为“非身份证”时填写信息不完整！");
                return map;
            }
        }

        map.put("success", true);
        return map;
    }
}
