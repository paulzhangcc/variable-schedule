package com.example.schedule.run;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author paul
 * @description
 * @date 2018/10/19
 */
@Component
public class SchedulingHelp implements SchedulingConfigurer, ApplicationListener<ApplicationReadyEvent>, ApplicationContextAware {


    public static ApplicationContext springApplicationContext = null;
    public static ScheduledTaskRegistrar scheduledTaskRegistrar = null;

    //正在运行
    private static ConcurrentHashMap<String, TriggerTask> mapCronRunnable = new ConcurrentHashMap<>(16);

    public synchronized void addTriggerTask(final CronRunnable cronRunnable) {
        if (cronRunnable == null) {
            return;
        }
        String code = cronRunnable.getClass().getName();
        if (mapCronRunnable.containsKey(code)) {
            //TODO logger
            return;
        }
        String cronRunnableCron = cronRunnable.getCron();
        if (cronRunnableCron == null ||cronRunnableCron.length()==0){
            //TODO logger
            return;
        }
        TriggerTask triggerTask = new TriggerTask(cronRunnable, new Trigger() {
            @Override
            public Date nextExecutionTime(TriggerContext triggerContext) {
                //任务触发，可修改任务的执行周期
                String cron_result = cronRunnable.getCron();
                CronTrigger trigger = new CronTrigger(cron_result);
                Date nextExec = trigger.nextExecutionTime(triggerContext);
                return nextExec;
            }
        });
        scheduledTaskRegistrar.scheduleTriggerTask(triggerTask);
        mapCronRunnable.put(code, triggerTask);
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        scheduledTaskRegistrar = taskRegistrar;
    }

    public static ScheduledTaskRegistrar getScheduledTaskRegistrar() {
        return scheduledTaskRegistrar;
    }


    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String[] beanNamesForType = springApplicationContext.getBeanNamesForType(CronRunnable.class);
        if (beanNamesForType == null || beanNamesForType.length == 0) {
            return;
        }
        Arrays.asList(beanNamesForType).forEach(new Consumer<String>() {
            @Override
            public void accept(String beanName) {
                SchedulingHelp.this.addTriggerTask((CronRunnable)springApplicationContext.getBean(beanName));
            }
        });

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        springApplicationContext = applicationContext;
    }


    public static PropertiesConfiguration getPropertiesConfiguration() {
        return propertiesConfiguration;
    }

    static PropertiesConfiguration propertiesConfiguration =null;

    static {
        try {
            propertiesConfiguration = new PropertiesConfiguration("application.properties");
            FileChangedReloadingStrategy strategy = new FileChangedReloadingStrategy();
            //这里会导致修改配置文件后不会实时生效，生效间隔自己配置
            strategy.setRefreshDelay(5000L);// 刷新周期5秒
            propertiesConfiguration.setReloadingStrategy(strategy);
        } catch (ConfigurationException e) {
            throw new RuntimeException("读取文件失败",e);
        }
    }
}
