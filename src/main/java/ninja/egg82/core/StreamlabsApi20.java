package ninja.egg82.core;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.extractors.OAuth2AccessTokenJsonExtractor;
import com.github.scribejava.core.extractors.TokenExtractor;
import com.github.scribejava.core.httpclient.HttpClient;
import com.github.scribejava.core.httpclient.HttpClientConfig;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.model.ParameterList;
import java.io.OutputStream;
import java.util.Map;

public class StreamlabsApi20 extends DefaultApi20 {
    protected StreamlabsApi20() { }

    private static class InstanceHolder {
        private static final StreamlabsApi20 INSTANCE = new StreamlabsApi20();
    }

    public static StreamlabsApi20 instance() { return InstanceHolder.INSTANCE; }

    @Override
    public String getAccessTokenEndpoint() { return "https://streamlabs.com/api/v1.0/token"; }

    @Override
    public String getAuthorizationUrl(String responseType, String apiKey, String callback, String scope, String state,
                                      Map<String, String> additionalParams) {
        final ParameterList parameters = new ParameterList(additionalParams);
        parameters.add(OAuthConstants.RESPONSE_TYPE, responseType);
        parameters.add(OAuthConstants.CLIENT_ID, apiKey);
        parameters.add(OAuthConstants.REDIRECT_URI, callback);
        parameters.add(OAuthConstants.SCOPE, scope);

        if (state != null) {
            parameters.add(OAuthConstants.STATE, state);
        }

        return parameters.appendTo("https://streamlabs.com/api/v1.0/authorize");
    }

    @Override
    protected String getAuthorizationBaseUrl() { throw new UnsupportedOperationException("use getAuthorizationUrl instead"); }

    @Override
    public StreamlabsOAuthService createService(String apiKey, String apiSecret, String callback, String defaultScope,
                                                String responseType, OutputStream debugStream, String userAgent, HttpClientConfig httpClientConfig,
                                                HttpClient httpClient) {
        return new StreamlabsOAuthService(this, apiKey, apiSecret, callback, defaultScope, responseType, debugStream,
                userAgent, httpClientConfig, httpClient);
    }

    @Override
    public TokenExtractor<OAuth2AccessToken> getAccessTokenExtractor() { return OAuth2AccessTokenJsonExtractor.instance(); }
}
