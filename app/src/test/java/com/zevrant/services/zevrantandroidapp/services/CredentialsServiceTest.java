package com.zevrant.services.zevrantandroidapp.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.zevrant.services.zevrantandroidapp.exceptions.CredentialTimeoutException;
import com.zevrant.services.zevrantandroidapp.exceptions.CredentialsNotFoundException;
import com.zevrant.services.zevrantandroidapp.utilities.Constants;
import com.zevrant.services.zevrantuniversalcommon.rest.oauth.response.OAuthToken;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.runners.statements.ExpectException;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

@RunWith(JUnit4.class)
public class CredentialsServiceTest extends TestCase {
    private CredentialsService credentialsService;

    @Mock
    private Context context;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private OAuthService oAuthService;

    @Before
    public void setup() {
        context = Mockito.mock(Context.class);
        encryptionService = Mockito.mock(EncryptionService.class);
        oAuthService = Mockito.mock(OAuthService.class);
        credentialsService = new CredentialsService(encryptionService, oAuthService, context);
    }

    @Test
    public void manageOauthTokenWriteToken() throws Exception {
        doNothing().when(encryptionService).setSecret(anyString(), anyString());
        OAuthToken oAuthToken = new OAuthToken();
        oAuthToken.setAccessToken("sdlkfjghsdlkfjghsdlkfgjh.lksdjfhghsldkjfghsdflkghsdfj.sldkfjghsdlfkjghsdefrlkgjh");
        oAuthToken.setExpiresInDateTime(LocalDateTime.now().plusMinutes(15));
        oAuthToken.setRefreshToken("hjwoeiurtyweoirutyweroiutywe.Z<MNCVXbzx,mnfgvbqear,fvbn.a;SODLIREUAPWOIUFHSDIOC");
        oAuthToken.setRefreshExpiresInDateTime(LocalDateTime.now().plusMinutes(15));
        OAuthToken returnedToken = credentialsService.manageOAuthToken(oAuthToken, true);
        verify(encryptionService, times(8)).setSecret(anyString(), anyString());
        assertThat(returnedToken, is(not(nullValue())));
        assertThat(returnedToken, is(oAuthToken));
    }

    @Test(expected = RuntimeException.class)
    public void manageOauthTokenWriteNullToken() throws Exception {
        doNothing().when(encryptionService).setSecret(anyString(), anyString());
        credentialsService.manageOAuthToken(null, true);
        fail("Should throw RuntimeException");
    }

    @Test
    public void manageOauthTokenWriteTokenUpdateToken() throws Exception {
        doNothing().when(encryptionService).setSecret(anyString(), anyString());
        OAuthToken oAuthToken = new OAuthToken();
        oAuthToken.setAccessToken("sdlkfjghsdlkfjghsdlkfgjh.lksdjfhghsldkjfghsdflkghsdfj.sldkfjghsdlfkjghsdefrlkgjh");
        oAuthToken.setExpiresInDateTime(LocalDateTime.now().plusMinutes(15));
        oAuthToken.setRefreshToken("hjwoeiurtyweoirutyweroiutywe.Z<MNCVXbzx,mnfgvbqear,fvbn.a;SODLIREUAPWOIUFHSDIOC");
        oAuthToken.setRefreshExpiresInDateTime(LocalDateTime.now().plusMinutes(15));
        OAuthToken returnedToken = credentialsService.manageOAuthToken(oAuthToken, true);
        verify(encryptionService, times(8)).setSecret(anyString(), anyString());
        assertThat(returnedToken, is(not(nullValue())));
        assertThat(returnedToken, is(oAuthToken));
        doNothing().when(encryptionService).setSecret(anyString(), anyString());
        OAuthToken newAuthToken = new OAuthToken();
        newAuthToken.setAccessToken("sdlkfjghsdlkfjghsdlkfgjh.lksdjfhghsldkjfghsdflkghsdfj.sldkfjghsdlfkjghsdefrlkgjh");
        newAuthToken.setExpiresInDateTime(LocalDateTime.now().plusMinutes(15));
        newAuthToken.setRefreshToken("hjwoeiurtyweoirutyweroiutywe.Z<MNCVXbzx,mnfgvbqear,fvbn.a;SODLIREUAPWOIUFHSDIOC");
        newAuthToken.setRefreshExpiresInDateTime(LocalDateTime.now().plusMinutes(15));
        returnedToken = credentialsService.manageOAuthToken(newAuthToken, true);
        verify(encryptionService, times(16)).setSecret(anyString(), anyString());
        assertThat(returnedToken, is(not(nullValue())));
        assertThat(returnedToken, is(newAuthToken));
        assertThat(returnedToken, is(not(oAuthToken)));
    }

    @Test
    public void getAuthorizationValidUnexpiredToken() throws Exception {
        OAuthToken defaultToken = setDefaultToken();
        String authorization = credentialsService.getAuthorization();
        verify(encryptionService, never()).getSecret(anyString());
        verify(oAuthService, never()).refreshToken(any(OAuthToken.class));
        assertThat(authorization, is(not(nullValue())));
        assertThat("Authorization was empty string", authorization.isEmpty(), is(not(true)));
        assertThat(authorization, is(defaultToken.getAccessToken()));
    }

    @Test
    public void getAuthorizationValidExpiredToken() throws Exception {
        OAuthToken defaultToken = setDefaultToken();
        defaultToken.setExpiresInDateTime(defaultToken.getExpirationDateTime().minusMinutes(20));
        defaultToken.setRefreshExpiresInDateTime(defaultToken.getRefreshExpiresInDateTime().minusMinutes(20));
        defaultToken = credentialsService.manageOAuthToken(defaultToken, true);
        OAuthToken newToken = new OAuthToken();
        newToken.setAccessToken("dfghdfghfghfgh.209347t8gydfsvkjdlgn45.xzvcp';ovziuxv");
        newToken.setExpiresInDateTime(LocalDateTime.now().plusMinutes(15));
        newToken.setRefreshToken("6rt7ikjyhjmfdmh.3s6d54fg3d65f4bv1dfbv.w2354tgdcxvszdfe");
        newToken.setRefreshExpiresInDateTime(LocalDateTime.now().plusMinutes(15));
        given(oAuthService.refreshToken(defaultToken)).willReturn(newToken);
        String authorization = credentialsService.getAuthorization();
        verify(encryptionService, never()).getSecret(anyString());
        verify(oAuthService).refreshToken(any(OAuthToken.class));
        assertThat(authorization, is(not(nullValue())));
        assertThat("Authorization was empty string", authorization.isEmpty(), is(not(true)));
        assertThat(authorization, is(not(defaultToken.getAccessToken())));
        assertThat(authorization, is(newToken.getAccessToken()));
    }

    @Test(expected = CredentialTimeoutException.class)
    public void getAuthorizationValidExpiredTokenNullTokenReturned() throws Exception {
        Executor mockExecutor = Mockito.mock(Executor.class);
        given(context.getMainExecutor()).willReturn(mockExecutor);
        doNothing().when(mockExecutor).execute(any(Runnable.class));
        OAuthToken defaultToken = setDefaultToken();
        defaultToken.setExpiresInDateTime(LocalDateTime.now().minusMinutes(14));
        defaultToken.setRefreshExpiresInDateTime(defaultToken.getRefreshExpiresInDateTime().minusMinutes(14));
        defaultToken = credentialsService.manageOAuthToken(defaultToken, true);
        given(oAuthService.refreshToken(defaultToken)).willReturn(null);
        credentialsService.getAuthorization();

    }

    @Test
    public void getAuthorizationNullTokenStoredValid() throws Exception {
        OAuthToken newToken = new OAuthToken();
        newToken.setAccessToken("section0.section1.section2");
        newToken.setExpiresInDateTime(LocalDateTime.now().plusMinutes(15));
        newToken.setRefreshToken("6rt7ikjyhjmfdmh.3s6d54fg3d65f4bv1dfbv.w2354tgdcxvszdfe");
        newToken.setRefreshExpiresInDateTime(LocalDateTime.now().plusMinutes(15));
        given(encryptionService.hasSecret(Constants.SecretNames.REFRESH_TOKEN_1)).willReturn(true);
        given(encryptionService.hasSecret("token-sec1-0")).willReturn(true);
        given(encryptionService.hasSecret("token-sec2-0")).willReturn(true);
        given(encryptionService.hasSecret("refresh-token-sec1-0")).willReturn(true);
        given(encryptionService.getSecret(Constants.SecretNames.TOKEN_0)).willReturn("section0");
        given(encryptionService.getSecret("token-sec1-0")).willReturn("section1");
        given(encryptionService.getSecret("token-sec2-0")).willReturn("section2");
        given(encryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_1)).willReturn("6rt7ikjyhjmfdmh");
        given(encryptionService.getSecret("refresh-token-sec1-0")).willReturn("3s6d54fg3d65f4bv1dfbv");
        given(encryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_2)).willReturn("w2354tgdcxvszdfe");
        given(encryptionService.getSecret(Constants.SecretNames.TOKEN_EXPIRATION)).willReturn(LocalDateTime.now().plusMinutes(10).toString());
        given(encryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_EXPIRATION)).willReturn(LocalDateTime.now().plusHours(1).toString());
        String authorization = credentialsService.getAuthorization();
        verify(encryptionService, times(8)).getSecret(anyString());
        verify(oAuthService, never()).refreshToken(any(OAuthToken.class));
        assertThat(authorization, is(not(nullValue())));
        assertThat("Authorization was empty string", authorization.isEmpty(), is(not(true)));
        assertThat(authorization, is(newToken.getAccessToken()));
        OAuthToken returnedToken = credentialsService.manageOAuthToken(null, false);
        assertThat(returnedToken.getRefreshToken(), is(newToken.getRefreshToken()));
    }

    @Test
    public void getAuthorizationNullTokenStoredValidExpired() throws Exception {
        OAuthToken encryptedToken = new OAuthToken();
        encryptedToken.setAccessToken("section0.section1.section2");
        encryptedToken.setExpiresInDateTime(LocalDateTime.now().plusMinutes(15));
        encryptedToken.setRefreshToken("6rt7ikjyhjmfdmh.3s6d54fg3d65f4bv1dfbv.w2354tgdcxvszdfe");
        encryptedToken.setRefreshExpiresInDateTime(LocalDateTime.now().plusMinutes(15));
        given(encryptionService.hasSecret(Constants.SecretNames.REFRESH_TOKEN_1)).willReturn(true);
        given(encryptionService.hasSecret("token-sec1-0")).willReturn(true);
        given(encryptionService.hasSecret("token-sec2-0")).willReturn(true);
        given(encryptionService.hasSecret("refresh-token-sec1-0")).willReturn(true);
        given(encryptionService.getSecret(Constants.SecretNames.TOKEN_0)).willReturn("section0");
        given(encryptionService.getSecret("token-sec1-0")).willReturn("section1");
        given(encryptionService.getSecret("token-sec2-0")).willReturn("section2");
        given(encryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_1)).willReturn("6rt7ikjyhjmfdmh");
        given(encryptionService.getSecret("refresh-token-sec1-0")).willReturn("3s6d54fg3d65f4bv1dfbv");
        given(encryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_2)).willReturn("w2354tgdcxvszdfe");
        given(encryptionService.getSecret(Constants.SecretNames.TOKEN_EXPIRATION)).willReturn(LocalDateTime.now().plusMinutes(10).toString());
        given(encryptionService.getSecret(Constants.SecretNames.REFRESH_TOKEN_EXPIRATION)).willReturn(LocalDateTime.now().plusHours(1).toString());
        given(oAuthService.refreshToken(encryptedToken)).willReturn(encryptedToken);
        String authorization = credentialsService.getAuthorization();
        verify(encryptionService, times(8)).getSecret(anyString());
        verify(oAuthService, never()).refreshToken(any(OAuthToken.class));
        assertThat(authorization, is(not(nullValue())));
        assertThat("Authorization was empty string", authorization.isEmpty(), is(not(true)));
        assertThat(authorization, is(encryptedToken.getAccessToken()));
        OAuthToken returnedToken = credentialsService.manageOAuthToken(null, false);
        assertThat(returnedToken.getRefreshToken(), is(encryptedToken.getRefreshToken()));

    }

    private OAuthToken setDefaultToken() throws Exception {
        OAuthToken oAuthToken = new OAuthToken();
        oAuthToken.setAccessToken("sdlkfjghsdlkfjghsdlkfgjh.lksdjfhghsldkjfghsdflkghsdfj.sldkfjghsdlfkjghsdefrlkgjh");
        oAuthToken.setExpiresInDateTime(LocalDateTime.now().plusMinutes(15));
        oAuthToken.setRefreshToken("hjwoeiurtyweoirutyweroiutywe.Z<MNCVXbzx,mnfgvbqear,fvbn.a;SODLIREUAPWOIUFHSDIOC");
        oAuthToken.setRefreshExpiresInDateTime(LocalDateTime.now().plusMinutes(15));
        OAuthToken returnedToken = credentialsService.manageOAuthToken(oAuthToken, true);
        verify(encryptionService, times(8)).setSecret(anyString(), anyString());
        assertThat(returnedToken, is(not(nullValue())));
        assertThat(returnedToken, is(oAuthToken));
        return oAuthToken;
    }
}