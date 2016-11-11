package com.zhxg.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class BeanTools implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    private static Logger logger = LoggerFactory.getLogger(BeanTools.class);

    /**
     * 根据beanName获取bean
     *
     * @param classname
     * @return
     * @throws Exception
     */
    public static Object getBean(String classname) {
        try {
            Object _restTemplate = applicationContext.getBean(classname);
            return _restTemplate;
        } catch (Exception e) {
            logger.error("{}获取Bean失败：" + e);
            return "";
        }
    }

    /**
     * 获取StringRedisTemplate实例
     *
     * @param clazz
     * @return
     * @throws Exception
     */
    public static StringRedisTemplate getBean(Class<StringRedisTemplate> clazz) {
        return applicationContext.getBean(clazz);

    }


    @Override
    public void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }
}
