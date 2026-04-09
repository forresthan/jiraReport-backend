package com.jirareport.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jirareport.common.exception.BusinessException;
import com.jirareport.common.util.EntityAudits;
import com.jirareport.mapper.ReportTextTemplateMapper;
import com.jirareport.mapper.entity.ReportTextTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportTextTemplateService {

    private final ReportTextTemplateMapper templateMapper;

    public ReportTextTemplateService(ReportTextTemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    public List<Map<String, Object>> list(String ownerId, String projectKey) {
        LambdaQueryWrapper<ReportTextTemplate> q = new LambdaQueryWrapper<>();
        q.eq(ReportTextTemplate::getOwnerId, ownerId);
        if (StrUtil.isNotBlank(projectKey)) {
            q.eq(ReportTextTemplate::getProjectKey, projectKey);
        }
        q.orderByDesc(ReportTextTemplate::getUpdatedAt);
        return templateMapper.selectList(q).stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String, Object> getById(String ownerId, Long id) {
        return toMap(requireOwned(ownerId, id));
    }

    @Transactional
    public Map<String, Object> create(String ownerId, String projectKey, String name, boolean enabled, String sectionsJson) {
        if (StrUtil.isBlank(name)) {
            throw new BusinessException("模板名称不能为空");
        }
        ReportTextTemplate t = new ReportTextTemplate();
        t.setOwnerId(ownerId);
        t.setProjectKey(projectKey);
        t.setName(name);
        t.setEnabled(enabled ? 1 : 0);
        t.setSectionsJson(sectionsJson);
        EntityAudits.stampForInsert(t);
        templateMapper.insert(t);
        return toMap(templateMapper.selectById(t.getId()));
    }

    @Transactional
    public Map<String, Object> update(String ownerId, Long id, String projectKey, String name, Boolean enabled, String sectionsJson) {
        ReportTextTemplate t = requireOwned(ownerId, id);
        if (projectKey != null) {
            t.setProjectKey(projectKey);
        }
        if (name != null) {
            if (StrUtil.isBlank(name)) {
                throw new BusinessException("模板名称不能为空");
            }
            t.setName(name);
        }
        if (enabled != null) {
            t.setEnabled(enabled ? 1 : 0);
        }
        if (sectionsJson != null) {
            t.setSectionsJson(sectionsJson);
        }
        templateMapper.updateById(t);
        return toMap(templateMapper.selectById(id));
    }

    @Transactional
    public void delete(String ownerId, Long id) {
        requireOwned(ownerId, id);
        templateMapper.deleteById(id);
    }

    private ReportTextTemplate requireOwned(String ownerId, Long id) {
        ReportTextTemplate t = templateMapper.selectById(id);
        if (t == null) {
            throw new BusinessException("模板不存在");
        }
        if (!ownerId.equals(t.getOwnerId())) {
            throw new BusinessException(403, "无权访问该模板");
        }
        return t;
    }

    private Map<String, Object> toMap(ReportTextTemplate t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", String.valueOf(t.getId()));
        m.put("projectKey", t.getProjectKey());
        m.put("name", t.getName());
        m.put("enabled", t.getEnabled() != null && t.getEnabled() == 1);
        m.put("status", t.getEnabled() != null && t.getEnabled() == 1 ? "启用" : "停用");
        m.put("sectionsJson", t.getSectionsJson());
        m.put("createdAt", t.getCreatedAt());
        m.put("updatedAt", t.getUpdatedAt());
        return m;
    }
}
