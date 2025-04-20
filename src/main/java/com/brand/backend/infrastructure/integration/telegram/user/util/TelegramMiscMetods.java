package com.brand.backend.infrastructure.integration.telegram.user.util;

public class TelegramMiscMetods {
    public static String censorUsername(String username) {
        if (username.length() <= 4) {
            return username.charAt(0) + "***" + username.charAt(username.length() - 1);
        }

        int censorStart = username.length() / 3;
        int censorEnd = username.length() - censorStart;

        StringBuilder censored = new StringBuilder();
        censored.append(username, 0, censorStart);
        censored.append("*".repeat(censorEnd - censorStart));
        censored.append(username.substring(censorEnd));

        return censored.toString();
    }
}
