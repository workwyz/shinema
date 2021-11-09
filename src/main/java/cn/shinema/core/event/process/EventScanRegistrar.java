package cn.shinema.core.event.process;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;

import cn.shinema.core.event.EventScan;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class EventScanRegistrar implements ImportBeanDefinitionRegistrar {
    private static Logger LOGGER = LoggerFactory.getLogger(EventScanRegistrar.class);

    public final static String EventScanPackages = "EventScanPackages";

    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Set<String> packagesToScan = getPackagesToScan(importingClassMetadata);
        String packagesToScanStr = StringUtils.join(packagesToScan, ',');
        System.setProperty(EventScanPackages, packagesToScanStr);
        LOGGER.info("================EventScanRegistrar================={}", packagesToScanStr);
    }

    private Set<String> getPackagesToScan(AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(EventScan.class.getName()));
        String[] basePackages = attributes.getStringArray("basePackages");
        String[] value = attributes.getStringArray("value");

        Set<String> packagesToScan = new LinkedHashSet<String>(Arrays.asList(value));
        packagesToScan.addAll(Arrays.asList(basePackages));

        if (packagesToScan.isEmpty()) {
            return Collections.singleton(ClassUtils.getPackageName(metadata.getClassName()));
        }
        return packagesToScan;
    }

}
