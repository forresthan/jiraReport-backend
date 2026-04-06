package com.jirareport.security.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jirareport.common.exception.BusinessException;
import com.jirareport.mapper.UserMapper;
import com.jirareport.mapper.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final String REDIS_USER_KEY_PREFIX = "user:";
    private static final long REDIS_USER_EXPIRE = 30;

    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String redisKey = REDIS_USER_KEY_PREFIX + username;
        String cachedUser = redisTemplate.opsForValue().get(redisKey);

        User user;
        if (cachedUser != null) {
            user = parseUserFromCache(cachedUser);
        } else {
            user = loadUserFromDatabase(username);
            if (user != null) {
                redisTemplate.opsForValue().set(redisKey, user.getId().toString(), REDIS_USER_EXPIRE, TimeUnit.MINUTES);
            }
        }

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        return buildUserDetails(user);
    }

    public User loadUserByUserId(Long userId) {
        return userMapper.selectById(userId);
    }

    private User loadUserFromDatabase(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, 0));
    }

    private User parseUserFromCache(String cachedData) {
        try {
            Long userId = Long.parseLong(cachedData);
            return userMapper.selectById(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private UserDetails buildUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .disabled(user.getStatus() == 0)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .authorities(Collections.emptyList())
                .build();
    }
}