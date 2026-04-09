package com.jirareport.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jirareport.mapper.PptThemeMapper;
import com.jirareport.mapper.entity.PptTheme;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PptThemeService {

    private final PptThemeMapper pptThemeMapper;

    public PptThemeService(PptThemeMapper pptThemeMapper) {
        this.pptThemeMapper = pptThemeMapper;
    }

    public List<Map<String, Object>> listThemes() {
        LambdaQueryWrapper<PptTheme> q = new LambdaQueryWrapper<>();
        q.orderByAsc(PptTheme::getSortOrder).orderByAsc(PptTheme::getId);
        return pptThemeMapper.selectList(q).stream().map(this::toMap).collect(Collectors.toList());
    }

    public PptTheme requireByThemeKey(String themeKey) {
        PptTheme t = pptThemeMapper.selectOne(new LambdaQueryWrapper<PptTheme>()
                .eq(PptTheme::getThemeKey, themeKey));
        if (t == null) {
            return null;
        }
        return t;
    }

    private Map<String, Object> toMap(PptTheme t) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", String.valueOf(t.getId()));
        m.put("themeKey", t.getThemeKey());
        m.put("name", t.getName());
        m.put("style", t.getStyle());
        m.put("previewMeta", t.getPreviewMeta());
        m.put("sortOrder", t.getSortOrder());
        return m;
    }
}
