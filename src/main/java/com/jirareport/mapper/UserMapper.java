package com.jirareport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jirareport.mapper.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}