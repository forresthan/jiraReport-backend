package com.jirareport.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jirareport.common.exception.BusinessException;
import com.jirareport.common.util.EntityAudits;
import com.jirareport.mapper.PptHtmlTemplateMapper;
import com.jirareport.mapper.entity.PptHtmlTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PptHtmlTemplateService {

    private final PptHtmlTemplateMapper mapper;

    public PptHtmlTemplateService(PptHtmlTemplateMapper mapper) {
        this.mapper = mapper;
    }

    public List<Map<String, Object>> list(String ownerId) {
        LambdaQueryWrapper<PptHtmlTemplate> q = new LambdaQueryWrapper<>();
        q.eq(PptHtmlTemplate::getOwnerId, ownerId);
        q.orderByDesc(PptHtmlTemplate::getUpdatedAt);
        return mapper.selectList(q).stream().map(this::toMap).collect(Collectors.toList());
    }

    public Map<String, Object> getById(String ownerId, Long id) {
        return toMap(requireOwned(ownerId, id));
    }

    @Transactional
    public Map<String, Object> create(String ownerId, String name, String description,
                                        String htmlContent, String metaJson, String originalFileName) {
        if (StrUtil.isBlank(name)) {
            throw new BusinessException("模板名称不能为空");
        }
        PptHtmlTemplate t = new PptHtmlTemplate();
        t.setOwnerId(ownerId);
        t.setName(name);
        t.setDescription(description);
        t.setHtmlContent(htmlContent);
        t.setMetaJson(metaJson);
        t.setOriginalFileName(originalFileName);
        t.setStatus("active");
        EntityAudits.stampForInsert(t);
        mapper.insert(t);
        return toMap(mapper.selectById(t.getId()));
    }

    @Transactional
    public Map<String, Object> update(String ownerId, Long id, String name, String description,
                                      String htmlContent, String metaJson, String status) {
        PptHtmlTemplate t = requireOwned(ownerId, id);
        if (name != null) {
            if (StrUtil.isBlank(name)) {
                throw new BusinessException("模板名称不能为空");
            }
            t.setName(name);
        }
        if (description != null) {
            t.setDescription(description);
        }
        if (htmlContent != null) {
            t.setHtmlContent(htmlContent);
        }
        if (metaJson != null) {
            t.setMetaJson(metaJson);
        }
        if (status != null) {
            t.setStatus(normalizeStatus(status));
        }
        mapper.updateById(t);
        return toMap(mapper.selectById(id));
    }

    @Transactional
    public void delete(String ownerId, Long id) {
        requireOwned(ownerId, id);
        mapper.deleteById(id);
    }

    private static String normalizeStatus(String status) {
        if ("启用".equals(status)) {
            return "active";
        }
        if ("停用".equals(status)) {
            return "archived";
        }
        return status;
    }

    private PptHtmlTemplate requireOwned(String ownerId, Long id) {
        PptHtmlTemplate t = mapper.selectById(id);
        if (t == null) {
            throw new BusinessException("模板不存在");
        }
        if (!ownerId.equals(t.getOwnerId())) {
            throw new BusinessException(403, "无权访问该模板");
        }
        return t;
    }

    private Map<String, Object> toMap(PptHtmlTemplate t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", String.valueOf(t.getId()));
        m.put("name", t.getName());
        m.put("description", t.getDescription());
        m.put("htmlContent", t.getHtmlContent());
        m.put("metaJson", t.getMetaJson());
        m.put("originalFileName", t.getOriginalFileName());
        boolean active = t.getStatus() == null || "active".equalsIgnoreCase(t.getStatus());
        m.put("status", active ? "启用" : "停用");
        m.put("createdAt", t.getCreatedAt());
        m.put("updatedAt", t.getUpdatedAt());
        return m;
    }
}
