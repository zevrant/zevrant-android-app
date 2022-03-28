package com.zevrant.services.zevrantandroidapp.services;

import android.content.Context;
import android.content.SharedPreferences;

import com.zevrant.services.zevrantandroidapp.utilities.Constants;

import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class UserSettingsService {

    private final SharedPreferences sharedPreferences;

    @Inject
    public UserSettingsService(@ApplicationContext Context context) {
        sharedPreferences = context.getSharedPreferences("zevrant-services-preferences-user-settings", Context.MODE_PRIVATE);
        //TODO pull from remote stored preferences
        for(Constants.UserPreference userPreference : Constants.UserPreference.values()) {
            String preference = getPreference(userPreference);
            if(StringUtils.isBlank(preference) || !userPreference.getMatcher(preference).matches()) {
                setPreference(userPreference, userPreference.getValue());
            }
        }
    }

    public String getPreference(Constants.UserPreference preference) {
        return sharedPreferences.getString(preference.name(), null);
    }

    public boolean setPreference(Constants.UserPreference preference, String value) {
        return sharedPreferences.edit()
                .putString(preference.name(), value)
                .commit();
    }
}
