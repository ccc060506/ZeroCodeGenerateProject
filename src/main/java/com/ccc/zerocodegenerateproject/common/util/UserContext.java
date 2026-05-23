package com.ccc.zerocodegenerateproject.common.util;

public class UserContext {

    private static final ThreadLocal<Long> ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    public static void setCurrentUser(Long id, String username) {
        ID.set(id);
        USERNAME.set(username);
    }

    public static Long getId() {
        return ID.get();
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static void clear() {
        ID.remove();
        USERNAME.remove();
    }

}
