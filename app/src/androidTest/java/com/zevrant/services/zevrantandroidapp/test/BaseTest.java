package com.zevrant.services.zevrantandroidapp.test;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;

import android.content.Context;

import com.zevrant.services.zevrantandroidapp.secrets.Secrets;
import com.zevrant.services.zevrantandroidapp.secrets.SecretsInitializer;
import com.zevrant.services.zevrantandroidapp.services.CredentialsService;
import com.zevrant.services.zevrantandroidapp.services.JsonParser;
import com.zevrant.services.zevrantuniversalcommon.rest.oauth.response.OAuthToken;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class BaseTest {

    public Context getTargetContext() {
        return getInstrumentation().getTargetContext();
    }

    public Context getTestContext() {
        return getInstrumentation().getContext();
    }

    @Before
    public void setup() throws NoSuchFieldException, IllegalAccessException, IOException {
        SecretsInitializer.init();
        URL url = new URL("https://develop.zevrant-services.com" +
                "/auth/realms/zevrant-services/protocol/openid-connect/token");

        String username = "test-admin";
        String urlParameters = "client_id=android&username=".concat(username).concat("&password=").concat(URLEncoder.encode(Secrets.getPassword(username), String.valueOf(StandardCharsets.UTF_8)))
                .concat("&grant_type=password");

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        urlConnection.setRequestProperty("Charset", "UTF-8");

        urlConnection.connect();

        DataOutputStream outputStream = new DataOutputStream(urlConnection.getOutputStream());
        outputStream.write(urlParameters.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
        outputStream.close();

        assertThat("Login failed for user ".concat(""), urlConnection.getResponseCode(), is(200));
        String response = "";
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        StringBuilder responseBuilder = new StringBuilder();
        bufferedReader.lines().forEach(responseBuilder::append);
        response = responseBuilder.toString();

        bufferedReader.close();
        assertThat("Login response was null", response, is(not(nullValue())));
        assertThat("No Access Token found in token response", response, containsString("access_token"));
        assertThat("No Refresh Token found in token response", response, containsString("refresh_token"));
        OAuthToken parsedToken = JsonParser.readValueFromString(response, OAuthToken.class);
        assertThat("Parsed token is null", parsedToken, is(not(nullValue())));
        parsedToken.setExpiresInDateTime(LocalDateTime.ofEpochSecond(LocalDateTime.now().toEpochSecond(ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())) - parsedToken.getExpiresIn(), 0, ZoneId.of("US/Eastern").getRules().getOffset(LocalDateTime.now())));
        assertThat("Parsed OAuth token did not contain a access token.", StringUtils.isNotBlank(parsedToken.getAccessToken()), is(true));
        CredentialsService.manageOAuthToken(parsedToken, true);
        OAuthToken token = CredentialsService.manageOAuthToken(null, false);
        assertThat("Retrieved token does not match parsed token", parsedToken.equals(token), is(true));
        Field tokenField = CredentialsService.class.getDeclaredField("oAuthToken");
        tokenField.setAccessible(true);
        tokenField.set(null, null);
        tokenField.setAccessible(false);
        token = CredentialsService.manageOAuthToken(null, false);
        assertThat("Decrypted access token does not match parsed token", token.getAccessToken(), is(parsedToken.getAccessToken()));
        assertThat("Decrypted refresh token does not match parsed token", token.getRefreshToken(), is(parsedToken.getRefreshToken()));
        assertThat("Decrypted expiration date time does not match parsed token", token.getExpirationDateTime(), is(parsedToken.getExpirationDateTime()));

    }
}
