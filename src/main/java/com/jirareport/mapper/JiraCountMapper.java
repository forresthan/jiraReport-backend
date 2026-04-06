package com.jirareport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jirareport.mapper.entity.JiraCount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JiraCountMapper extends BaseMapper<JiraCount> {
}
