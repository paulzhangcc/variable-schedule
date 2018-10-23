package com.example.schedule.run.impl;

import com.example.schedule.run.CronRunnable;
import com.example.schedule.run.SchedulingHelp;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author paul
 * @description
 * @date 2018/10/19
 */
@Component
public class HelloCronRunnable implements CronRunnable {

    @Override
    public String getCron(){
        return SchedulingHelp.getPropertiesConfiguration().getString("cron.HelloCronRunnable","0/10 * * * * ?");
    }

    @Override
    //@ScheduledLock()
    public void run() {
        java.text.DateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        System.out.println("hello:============="+format.format(new Date()));
    }
}
