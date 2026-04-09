package com.jirareport.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jirareport.common.exception.BusinessException;
import com.jirareport.common.util.EntityAudits;
import com.jirareport.mapper.PptExportJobMapper;
import com.jirareport.mapper.entity.PptExportJob;
import com.jirareport.mapper.entity.PptTheme;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PptExportService {

    private static final String JOB_RUNNING = "running";
    private static final String JOB_DONE = "done";

    private final PptExportJobMapper jobMapper;
    private final SummaryReportService summaryReportService;
    private final PptThemeService pptThemeService;

    public PptExportService(PptExportJobMapper jobMapper,
                            SummaryReportService summaryReportService,
                            PptThemeService pptThemeService) {
        this.jobMapper = jobMapper;
        this.summaryReportService = summaryReportService;
        this.pptThemeService = pptThemeService;
    }

    /**
     * 创建导出任务：当前为同步占位实现（直接标记完成），后续可改为异步 + 文件存储。
     */
    @Transactional
    public Map<String, Object> startJob(String ownerId, Long reportId, String themeKey) {
        summaryReportService.requireOwned(ownerId, reportId);
        PptTheme theme = pptThemeService.requireByThemeKey(themeKey);
        if (theme == null) {
            throw new BusinessException("未知的 PPT 主题: " + themeKey);
        }

        PptExportJob job = new PptExportJob();
        job.setOwnerId(ownerId);
        job.setReportId(reportId);
        job.setThemeKey(themeKey);
        job.setStatus(JOB_RUNNING);
        job.setProgress(0);
        EntityAudits.stampForInsert(job);
        jobMapper.insert(job);

        job.setStatus(JOB_DONE);
        job.setProgress(100);
        job.setResultUrl(null);
        job.setErrorMessage(null);
        jobMapper.updateById(job);

        return toMap(jobMapper.selectById(job.getId()));
    }

    public Map<String, Object> getJob(String ownerId, Long jobId) {
        PptExportJob job = jobMapper.selectById(jobId);
        if (job == null) {
            throw new BusinessException("任务不存在");
        }
        if (!ownerId.equals(job.getOwnerId())) {
            throw new BusinessException(403, "无权访问该任务");
        }
        return toMap(job);
    }

    public List<Map<String, Object>> listJobsForReport(String ownerId, Long reportId) {
        summaryReportService.requireOwned(ownerId, reportId);
        LambdaQueryWrapper<PptExportJob> q = new LambdaQueryWrapper<>();
        q.eq(PptExportJob::getReportId, reportId);
        q.eq(PptExportJob::getOwnerId, ownerId);
        q.orderByDesc(PptExportJob::getCreatedAt);
        return jobMapper.selectList(q).stream().map(this::toMap).collect(Collectors.toList());
    }

    private Map<String, Object> toMap(PptExportJob job) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", String.valueOf(job.getId()));
        m.put("reportId", String.valueOf(job.getReportId()));
        m.put("themeKey", job.getThemeKey());
        m.put("status", job.getStatus());
        m.put("progress", job.getProgress());
        m.put("resultUrl", job.getResultUrl());
        m.put("errorMessage", job.getErrorMessage());
        m.put("createdAt", job.getCreatedAt());
        m.put("updatedAt", job.getUpdatedAt());
        return m;
    }
}
