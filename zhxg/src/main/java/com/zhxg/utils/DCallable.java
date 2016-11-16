package com.zhxg.utils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;


public class DCallable implements Callable<Integer> {

    // 定时删除任务每次删除的数据条目
    private final static String LIMIT = "LIMIT 10000";
    private final static String WK_T_EVERYDAYDATA = "WK_T_EVERYDAYDATA";
    private final static String WK_T_VALIDATION_INFO = "WK_T_VALIDATION_INFO";

    private final static String WK_T_VALIDATION_INFOCNT = "WK_T_VALIDATION_INFOCNT";
    private final static String WK_T_VALIDATION_LOCATIONREF = "WK_T_VALIDATION_LOCATIONREF";
    private final static String WK_T_VALIDATION_REF = "WK_T_VALIDATION_REF";
    private Logger logger = LoggerFactory.getLogger(DCallable.class);


    private Map<String, Object> userinfo;
    private JdbcTemplate jdbcTemplate;
    AtomicInteger tld = null;

    public DCallable(Map<String, Object> userInfo, JdbcTemplate jdbcTemplate, AtomicInteger tld) {
        this.jdbcTemplate = jdbcTemplate;
        this.userinfo = userInfo;
        this.tld = tld;
    }

    @Override
    public Integer call() throws Exception {
        return this.processDataHandler(this.userinfo, this.jdbcTemplate);
    }

    /**
     * 清除用户数据
     *
     * @param userInfo
     * @param jdbcTemplate
     * @throws Exception
     */
    public int processDataHandler(Map<String, Object> userInfo, JdbcTemplate jdbcTemplate) throws Exception {
        if (null != jdbcTemplate && !userInfo.isEmpty()) {
            String ku_id = userInfo.get("KU_ID").toString();
            String ku_name = userInfo.get("KU_LID").toString();
            String ku_dbName = userInfo.get("KU_DBNAME").toString();
            // 用户未如设置保存天数，则默认为7天
            int ku_saveDays = StringUtils.isNotBlank(userInfo.get("KU_SAVEDAYS").toString())
                    ? Integer.valueOf(userInfo.get("KU_SAVEDAYS").toString()) : 7;
            // 如果未设置，默认为0，清除用户数据
            String ku_status = StringUtils.isNotBlank(userInfo.get("KU_ISSAVEOVERDUEDATA").toString())
                    ? userInfo.get("KU_ISSAVEOVERDUEDATA").toString() : "0";
            int WK_T_VALIDATION_REF_COUNT = 0;
            int WK_T_VALIDATION_LOCATIONREF_COUNT = 0;
            int WK_T_VALIDATION_INFO_COUNT = 0;
            int WK_T_VALIDATION_INFOCNT_COUNT = 0;
            int WK_T_EVERYDAYDATA_COUNT = 0;
            int YQZB_T_ENGINE_INFO_COUNT = 0;
            // 根据标识判断，是否存储用户信息
            if ("0".equals(ku_status)) {
                // 删除WK_T_VALIDATION_REF表数据
                WK_T_VALIDATION_REF_COUNT = this.deleteTableDataBy_KV_DTCTIME(jdbcTemplate, WK_T_VALIDATION_REF, ku_id,
                        ku_name, ku_dbName,
                        ku_saveDays);

                // 删除WK_T_VALIDATION_LOCATIONREF表数据
                WK_T_VALIDATION_LOCATIONREF_COUNT = this.deleteTableDataBy_KV_DTCTIME(jdbcTemplate, WK_T_VALIDATION_LOCATIONREF, ku_id, ku_name, ku_dbName,
                        ku_saveDays);

                // 删除WK_T_VALIDATION_INFO表数据
                WK_T_VALIDATION_INFO_COUNT = this.deleteTableDataBy_KV_DTCTIME(jdbcTemplate, WK_T_VALIDATION_INFO, ku_id, ku_name, ku_dbName,
                        ku_saveDays);

                // 删除WK_T_VALIDATION_INFOCNT表数据
                WK_T_VALIDATION_INFOCNT_COUNT = this.deleteTableDataBy_KV_DTCTIME(jdbcTemplate, WK_T_VALIDATION_INFOCNT, ku_id, ku_name, ku_dbName,
                        ku_saveDays);
            } else if ("1".equals(ku_status)) {
                // 备份WK_T_VALIDATION_REF表数据
                this.copyTableByName(jdbcTemplate, WK_T_VALIDATION_REF, ku_id, ku_name, ku_dbName,
                        ku_saveDays);

                // 备份WK_T_VALIDATION_LOCATIONREF表数据
                this.copyTableByName(jdbcTemplate, WK_T_VALIDATION_LOCATIONREF, ku_id, ku_name, ku_dbName,
                        ku_saveDays);

                // 备份WK_T_VALIDATION_INFO表数据
                this.copyTableByName(jdbcTemplate, WK_T_VALIDATION_INFO, ku_id, ku_name, ku_dbName,
                        ku_saveDays);

                // 备份WK_T_VALIDATION_INFOCNT表数据
                this.copyTableByName(jdbcTemplate, WK_T_VALIDATION_INFOCNT, ku_id, ku_name, ku_dbName,
                        ku_saveDays);
            }
            // 删除WK_T_EVERYDAYDATA表数据
            WK_T_EVERYDAYDATA_COUNT = this.deleteTableDataBy_KV_TIME(jdbcTemplate, WK_T_EVERYDAYDATA, ku_id, ku_name, ku_dbName, ku_saveDays);

            // 删除YQZB_T_ENGINE_INFO表数据
            YQZB_T_ENGINE_INFO_COUNT = this.delete_YQZB_T_ENGINE_INFO(jdbcTemplate, ku_id, ku_name, ku_dbName);
            this.tld.getAndIncrement();
            return WK_T_VALIDATION_REF_COUNT + WK_T_VALIDATION_LOCATIONREF_COUNT + WK_T_VALIDATION_INFO_COUNT
                    + WK_T_VALIDATION_INFOCNT_COUNT + WK_T_EVERYDAYDATA_COUNT + YQZB_T_ENGINE_INFO_COUNT;
        } else {
            this.logger.error("数据库链接丢失或用户信息丢失");
            return 0;
        }
    }


    /**
     * 删除指定表内指定天数数据
     * 根据KV_DTCTIME
     * 
     * @param jdbcTemplate
     * @param tableName
     * @param ku_id
     * @param ku_name
     * @param ku_dbName
     * @param ku_saveDays
     * @throws Exception
     */
    public int deleteTableDataBy_KV_DTCTIME(JdbcTemplate jdbcTemplate, String tableName, String ku_id, String ku_name,
            String ku_dbName, int ku_saveDays) throws Exception {
        StringBuffer deleteSQL = new StringBuffer();
        deleteSQL.append("DELETE FROM U");
        deleteSQL.append(ku_id);
        deleteSQL.append(".");
        deleteSQL.append(tableName);
        deleteSQL.append(" WHERE KV_DTCTIME <= '");
        deleteSQL.append(DateUtil.dateToyyyyMMdd(DateUtil.addDay(new Date(), -ku_saveDays)));
        deleteSQL.append("'");
        deleteSQL.append(LIMIT);
        try {

            int count = jdbcTemplate.update(deleteSQL.toString());
            this.logger.info(
                    "用户：" + ku_name + "，从" + ku_dbName + "上删除【" + tableName + "】表中" + +ku_saveDays + "天前的数据："
                            + count + "条");
            return count;
        } catch (Exception e) {
            throw new Exception("删除表数据异常，{}" + e);
        }
    }

    /**
     * 删除指定表内指定天数数据
     * 根据KV_TIME
     * 
     * @param jdbcTemplate
     * @param tableName
     * @param ku_id
     * @param ku_name
     * @param ku_dbName
     * @param ku_saveDays
     * @throws Exception
     */
    public int deleteTableDataBy_KV_TIME(JdbcTemplate jdbcTemplate, String tableName, String ku_id, String ku_name,
            String ku_dbName, int ku_saveDays) throws Exception {
        StringBuffer deleteSQL = new StringBuffer();
        deleteSQL.append("DELETE FROM U");
        deleteSQL.append(ku_id);
        deleteSQL.append(".");
        deleteSQL.append(tableName);
        deleteSQL.append(" WHERE KV_TIME <= '");
        deleteSQL.append(DateUtil.dateToyyyyMMdd(DateUtil.addDay(new Date(), -7)));
        deleteSQL.append("'");
        deleteSQL.append(LIMIT);
        try {
            int count = jdbcTemplate.update(deleteSQL.toString());
            this.logger.info(
                    "用户：" + ku_name + "，从" + ku_dbName + "上删除【" + tableName + "】表中" + +ku_saveDays + "天前的数据："
                            + count + "条");
            return count;
        } catch (Exception e) {
            throw new Exception("删除表数据异常，{}" + e);
        }
    }

    /**
     * 删除
     * YQZB_T_ENGINE_INFO表数据
     * 删除not in YQZB_T_TOPIC
     *
     * @param jdbcTemplate
     * @param ku_id
     * @param ku_name
     * @param ku_dbName
     * @throws Exception
     * @throws Exception
     */
    public int delete_YQZB_T_ENGINE_INFO(JdbcTemplate jdbcTemplate, String ku_id, String ku_name, String ku_dbName)
            throws Exception {
        StringBuffer deleteSQL = new StringBuffer();
        deleteSQL.append("DELETE FROM U");
        deleteSQL.append(ku_id);
        deleteSQL.append(".");
        deleteSQL.append("YQZB_T_ENGINE_INFO");
        deleteSQL.append(" WHERE KT_UUID NOT IN (");
        deleteSQL.append(this.getUserTopic().toString());
        deleteSQL.append(")");
        deleteSQL.append(LIMIT);
        try {
            int count = jdbcTemplate.update(deleteSQL.toString());
            this.logger.info(
                    "用户：" + ku_name + "，从" + ku_dbName + "上删除【YQZB_T_ENGINE_INFO】表中数据："
                            + count + "条");
            return count;
        } catch (Exception e) {
            throw new Exception("删除表数据异常，{}" + e);
        }
    }

    /**
     * 获取用户话题关联表
     *
     * @return
     * @throws Exception
     */
    public StringBuffer getUserTopic() {
        List<Map<String, Object>> list = this.getJdbcTemplate("user2JdbcTemplate")
                .queryForList("SELECT KT_UUID FROM yqms2.YQZB_T_TOPIC;");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < list.size(); i++) {
            sb.append("'");
            sb.append(list.get(i).get("KT_UUID").toString());
            if (i == (list.size() - 1)) {
                sb.append("'");
            } else {
                sb.append("',");
            }
        }
        return sb;
    }

    /**
     * 获取jdbc链接实例
     * 根据jdbc beanName
     *
     * @param beanName
     * @return
     * @throws Exception
     */
    public JdbcTemplate getJdbcTemplate(String beanName) {
        return (JdbcTemplate) BeanTools.getBean(beanName);
    }

    /**
     * 根据表名复制表
     * 表名为用户配置
     *
     * @param jdbcTemplate
     * @param tableName
     * @param ku_id
     * @param ku_name
     * @param ku_dbName
     * @param ku_saveDays
     * @throws Exception
     */
    public void copyTableByName(JdbcTemplate jdbcTemplate, String tableName, String ku_id, String ku_name,
            String ku_dbName, int ku_saveDays) throws Exception {
        StringBuffer createSQL = new StringBuffer();
        createSQL.append("CREATE TABLE U");
        createSQL.append(ku_id);
        createSQL.append(".");
        createSQL.append(tableName);
        createSQL.append("_");
        createSQL.append(DateUtil.dateToString(DateUtil.addDay(new Date(), -ku_saveDays)));
        createSQL.append("(");
        createSQL.append("SELECT * FROM U");
        createSQL.append(ku_id);
        createSQL.append(".");
        createSQL.append(tableName);
        createSQL.append(" WHERE KV_DTCTIME <= '");
        createSQL.append(DateUtil.dateToyyyyMMdd(DateUtil.addDay(new Date(), -ku_saveDays)));
        createSQL.append("'");
        createSQL.append(")");
        try {

            jdbcTemplate.execute(createSQL.toString());
            this.logger.info(
                    "用户：" + ku_name + "，从" + ku_dbName + "上，【" + tableName + "】表中复制" + +ku_saveDays + "天前的数据到【"
                            + tableName + "_"
                            + DateUtil.dateToString(DateUtil.addDay(new Date(), -ku_saveDays)) + "】表");
        } catch (Exception e) {
            throw new Exception("删除表数据异常，{}" + e);
        }
    }

}