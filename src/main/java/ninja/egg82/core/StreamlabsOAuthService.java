package ninja.egg82.core;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.HttpClientConfig;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.oauth.AccessTokenRequestParams;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.pkce.PKCE;
import java.io.OutputStream;

public class StreamlabsOAuthService extends OAuth20Service {
    public StreamlabsOAuthService(StreamlabsApi20 api, String apiKey, String apiSecret, String callback, String defaultScope,
                             String responseType, OutputStream debugStream, String userAgent, HttpClientConfig httpClientConfig,
                             HttpClient httpClient) {
        super(api, apiKey, apiSecret, callback, defaultScope, responseType, debugStream, userAgent, httpClientConfig,
                httpClient);
    }

    @Override
    protected OAuthRequest createAccessTokenRequest(AccessTokenRequestParams params) {
        final DefaultApi20 api = getApi();
        final OAuthRequest request = new OAuthRequest(api.getAccessTokenVerb(), api.getAccessTokenEndpoint());

        api.getClientAuthentication().addClientAuthentication(request, getApiKey(), getApiSecret());
        request.addBodyParameter(OAuthConstants.GRANT_TYPE, OAuthConstants.AUTHORIZATION_CODE);
        request.addBodyParameter(OAuthConstants.CLIENT_ID, getApiKey());
        request.addBodyParameter(OAuthConstants.CLIENT_SECRET, getApiSecret());
        request.addBodyParameter(OAuthConstants.REDIRECT_URI, getCallback());
        request.addParameter(OAuthConstants.CODE, params.getCode());

        final String pkceCodeVerifier = params.getPkceCodeVerifier();
        if (pkceCodeVerifier != null) {
            request.addParameter(PKCE.PKCE_CODE_VERIFIER_PARAM, pkceCodeVerifier);
        }
        if (isDebug()) {
            log("created access token request with body params [%s], query string params [%s]",
                    request.getBodyParams().asFormUrlEncodedString(),
                    request.getQueryStringParams().asFormUrlEncodedString());
        }
        return request;
    }

    @Override
    public void signRequest(String accessToken, OAuthRequest request) {
        request.addHeader(OAuthConstants.HEADER,
                accessToken == null ? "Client-ID " + getApiKey() : "Bearer " + accessToken);
    }
}
