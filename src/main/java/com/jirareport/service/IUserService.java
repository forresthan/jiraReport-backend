package com.jirareport.service;

import java.util.Map;

public interface IUserService {

    Map<String, Object> login(String username, String password);

    void logout(String username);

    Map<String, Object> getUserInfo(String username);

    void storeJiraSession(String userId, String jiraSession);

    String getJiraSession(String userId);

    void removeJiraSession(String userId);
}
