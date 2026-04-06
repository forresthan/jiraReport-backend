package com.jirareport.schedule;

import com.jirareport.service.RobotService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class RemindBSTask {

    private static final Logger log = LoggerFactory.getLogger(RemindBSTask.class);

    private final RobotService robotService;

    @Value("${schedule.remind-bs.enabled:true}")
    private boolean enabled;

    @Value("${schedule.remind-bs.cron:0 0 9 * * ?}")
    private String cron;

    public RemindBSTask(RobotService robotService) {
        this.robotService = robotService;
    }

    @Scheduled(cron = "${schedule.remind-bs.cron:0 0 9 * * ?}")
    public void execute() {
        if (!enabled) {
            log.info("RemindBS task is disabled, skipping execution");
            return;
        }

        log.info("Starting RemindBS scheduled task");
        try {
            Map<String, Object> result = robotService.doRemindBS();
            log.info("RemindBS task completed: {}", result);
        } catch (Exception e) {
            log.error("RemindBS task execution failed: {}", e.getMessage(), e);
        }
    }
}