package com.zevrant.services.zevrantandroidapp.utilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Constants {

    public static final String LOG_TAG = "ZevrantServices";

    public static class JobTags {
        public static final String BACKUP_TAG = "BACKUP";
        public static final String UPDATE_TAG = "UPDATE";
    }

    public static class MediaViewerControls {
        public static final int MAX_WIDTH_DP = 175;
        public static final int MAX_HIEGHT_DP = 125;
    }

    public static class SecretNames {
        public static final String TOKEN_0 = "token0";
        public static final String REFRESH_TOKEN_1 = "refreshToken1";
        public static final String REFRESH_TOKEN_2 = "refreshToken2";
        public static final String TOKEN_EXPIRATION = "tokenExpiration";
        public static final String REFRESH_TOKEN_EXPIRATION = "refreshTokenExpiration";
    }

    public enum UserPreference {
        DEFAULT_PAGE_COUNT("6", Pattern.compile("\\d+"));

        private final String value;
        private final Pattern validatorPattern;

        UserPreference(String value, Pattern validatorPattern) {
            this.value = value;
            this.validatorPattern = validatorPattern;
        }

        public String getValue() {
            return value;
        }

        public Matcher getMatcher(String inputString) {
            return validatorPattern.matcher(inputString);
        }
    }
}
