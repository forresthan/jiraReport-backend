package com.jirareport.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jirareport.common.exception.BusinessException;
import com.jirareport.common.util.EntityAudits;
import com.jirareport.mapper.SummaryReportMapper;
import com.jirareport.mapper.entity.SummaryReport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SummaryReportService {

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_SUBMITTED = "submitted";
    public static final String STATUS_REVIEWED = "reviewed";

    private final SummaryReportMapper summaryReportMapper;

    public SummaryReportService(SummaryReportMapper summaryReportMapper) {
        this.summaryReportMapper = summaryReportMapper;
    }

    public List<Map<String, Object>> list(String ownerId, String projectKey, String status) {
        LambdaQueryWrapper<SummaryReport> q = new LambdaQueryWrapper<>();
        q.eq(SummaryReport::getOwnerId, ownerId);
        if (StrUtil.isNotBlank(projectKey)) {
            q.eq(SummaryReport::getProjectKey, projectKey);
        }
        if (StrUtil.isNotBlank(status)) {
            q.eq(SummaryReport::getStatus, status);
        }
        q.orderByDesc(SummaryReport::getUpdatedAt);
        return summaryReportMapper.selectList(q).stream().map(this::toBriefMap).collect(Collectors.toList());
    }

    public Map<String, Object> getById(String ownerId, Long id) {
        SummaryReport r = requireOwned(ownerId, id);
        return toDetailMap(r);
    }

    @Transactional
    public Map<String, Object> create(String ownerId, String projectKey, String projectName, String title,
                                        String content, Long templateId) {
        SummaryReport r = new SummaryReport();
        r.setOwnerId(ownerId);
        r.setProjectKey(projectKey);
        r.setProjectName(projectName);
        r.setTitle(StrUtil.blankToDefault(title, "未命名报告"));
        r.setContent(content);
        r.setStatus(STATUS_DRAFT);
        r.setTemplateId(templateId);
        EntityAudits.stampForInsert(r);
        summaryReportMapper.insert(r);
        return toDetailMap(summaryReportMapper.selectById(r.getId()));
    }

    @Transactional
    public Map<String, Object> update(String ownerId, Long id, String title, String content, String status,
                                       String projectKey, String projectName, Long templateId, String slidesJson) {
        SummaryReport r = requireOwned(ownerId, id);
        if (title != null) {
            r.setTitle(title);
        }
        if (content != null) {
            r.setContent(content);
        }
        if (status != null) {
            validateStatus(status);
            r.setStatus(status);
        }
        if (projectKey != null) {
            r.setProjectKey(projectKey);
        }
        if (projectName != null) {
            r.setProjectName(projectName);
        }
        if (templateId != null) {
            r.setTemplateId(templateId);
        }
        if (slidesJson != null) {
            r.setSlidesJson(slidesJson);
        }
        summaryReportMapper.updateById(r);
        return toDetailMap(summaryReportMapper.selectById(id));
    }

    @Transactional
    public void delete(String ownerId, Long id) {
        requireOwned(ownerId, id);
        summaryReportMapper.deleteById(id);
    }

    @Transactional
    public Map<String, Object> copy(String ownerId, Long id) {
        SummaryReport src = requireOwned(ownerId, id);
        SummaryReport r = new SummaryReport();
        r.setOwnerId(ownerId);
        r.setProjectKey(src.getProjectKey());
        r.setProjectName(src.getProjectName());
        r.setTitle(src.getTitle() + " (副本)");
        r.setContent(src.getContent());
        r.setStatus(STATUS_DRAFT);
        r.setTemplateId(src.getTemplateId());
        r.setSlidesJson(null);
        EntityAudits.stampForInsert(r);
        summaryReportMapper.insert(r);
        return toDetailMap(summaryReportMapper.selectById(r.getId()));
    }

    @Transactional
    public Map<String, Object> submit(String ownerId, Long id) {
        SummaryReport r = requireOwned(ownerId, id);
        if (!STATUS_DRAFT.equals(r.getStatus())) {
            throw new BusinessException("仅草稿可提交");
        }
        r.setStatus(STATUS_SUBMITTED);
        summaryReportMapper.updateById(r);
        return toDetailMap(r);
    }

    public SummaryReport requireOwned(String ownerId, Long id) {
        SummaryReport r = summaryReportMapper.selectById(id);
        if (r == null) {
            throw new BusinessException("报告不存在");
        }
        if (!ownerId.equals(r.getOwnerId())) {
            throw new BusinessException(403, "无权访问该报告");
        }
        return r;
    }

    private void validateStatus(String status) {
        if (!STATUS_DRAFT.equals(status) && !STATUS_SUBMITTED.equals(status) && !STATUS_REVIEWED.equals(status)) {
            throw new BusinessException("无效状态: " + status);
        }
    }

    private Map<String, Object> toBriefMap(SummaryReport r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", String.valueOf(r.getId()));
        m.put("projectKey", r.getProjectKey());
        m.put("projectName", r.getProjectName());
        m.put("title", r.getTitle());
        m.put("status", toDisplayStatus(r.getStatus()));
        m.put("createdAt", r.getCreatedAt());
        m.put("updatedAt", r.getUpdatedAt());
        return m;
    }

    private Map<String, Object> toDetailMap(SummaryReport r) {
        Map<String, Object> m = toBriefMap(r);
        m.put("content", r.getContent());
        m.put("statusCode", r.getStatus());
        m.put("templateId", r.getTemplateId());
        m.put("slidesJson", r.getSlidesJson());
        return m;
    }

    /** 与前端中文状态展示对齐 */
    private String toDisplayStatus(String code) {
        if (STATUS_SUBMITTED.equals(code)) {
            return "已提交";
        }
        if (STATUS_REVIEWED.equals(code)) {
            return "已审核";
        }
        return "草稿";
    }
}
