package com.jirareport.common.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
public class CommonUtil {

    private static final Logger log = LoggerFactory.getLogger(CommonUtil.class);

    private static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        }
        if (principal instanceof String) {
            return (String) principal;
        }
        if (principal instanceof com.jirareport.security.JwtAuthenticationFilter.UserPrincipal) {
            return ((com.jirareport.security.JwtAuthenticationFilter.UserPrincipal) principal).getUsername();
        }
        return null;
    }

    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT));
    }

    public static String formatDate(LocalDate date, String pattern) {
        if (date == null) {
            return null;
        }
        if (StrUtil.isBlank(pattern)) {
            pattern = DEFAULT_DATE_FORMAT;
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT));
    }

    public static String formatDateTime(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        if (StrUtil.isBlank(pattern)) {
            pattern = DEFAULT_DATETIME_FORMAT;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDate parseDate(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            return null;
        }
        return DateUtil.parseDate(dateStr).toSqlDate().toLocalDate();
    }

    public static LocalDate parseDate(String dateStr, String pattern) {
        if (StrUtil.isBlank(dateStr)) {
            return null;
        }
        if (StrUtil.isBlank(pattern)) {
            pattern = DEFAULT_DATE_FORMAT;
        }
        return DateUtil.parse(dateStr, pattern).toSqlDate().toLocalDate();
    }

    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (StrUtil.isBlank(dateTimeStr)) {
            return null;
        }
        return DateUtil.parseDateTime(dateTimeStr).toLocalDateTime();
    }

    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        if (StrUtil.isBlank(dateTimeStr)) {
            return null;
        }
        if (StrUtil.isBlank(pattern)) {
            pattern = DEFAULT_DATETIME_FORMAT;
        }
        return DateUtil.parse(dateTimeStr, pattern).toLocalDateTime();
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static Date toDate(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static Date toDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}