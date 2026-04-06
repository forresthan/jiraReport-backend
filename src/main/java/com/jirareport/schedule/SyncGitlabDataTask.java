package com.jirareport.schedule;

import com.jirareport.service.GitlabService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SyncGitlabDataTask {

    private static final Logger log = LoggerFactory.getLogger(SyncGitlabDataTask.class);

    private final GitlabService gitlabService;

    @Value("${schedule.sync-gitlab.enabled:true}")
    private boolean enabled;

    @Value("${schedule.sync-gitlab.cron:0 0 2 * * ?}")
    private String cron;

    public SyncGitlabDataTask(GitlabService gitlabService) {
        this.gitlabService = gitlabService;
    }

    @Scheduled(cron = "${schedule.sync-gitlab.cron:0 0 2 * * ?}")
    public void execute() {
        if (!enabled) {
            log.info("SyncGitlab task is disabled, skipping execution");
            return;
        }

        log.info("Starting SyncGitlabData scheduled task");
        try {
            String result = gitlabService.syncAllGitlabData();
            log.info("SyncGitlabData task completed: {}", result);
        } catch (Exception e) {
            log.error("SyncGitlabData task execution failed: {}", e.getMessage(), e);
        }
    }
}