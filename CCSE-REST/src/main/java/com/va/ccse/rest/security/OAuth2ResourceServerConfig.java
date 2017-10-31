package com.va.ccse.rest.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

@Configuration
@EnableResourceServer
public class OAuth2ResourceServerConfig extends ResourceServerConfigurerAdapter {

	private static final String RESOURCE_ID = "SPRING_REST_API";
	
	@Override
	public void configure(ResourceServerSecurityConfigurer resources) {
		resources.resourceId(RESOURCE_ID).tokenStore(tokenStore);
	}

	//This method makes HttpSecurity configuration for specific http requests. We are using antMathchers to restrict the specific requests here
    //We catch all the 403 cases (http access denied) and redirect to our error page by using exceptionHandling().accessDeniedPage()

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http
                .csrf().disable()
        .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS).permitAll()
        .antMatchers("/").authenticated()
                .antMatchers(HttpMethod.OPTIONS,"/oauth/token").permitAll()
        .antMatchers( "/testController").hasRole("ADMIN")
        .antMatchers("/fruitsController").hasRole("USER")
                /*.antMatchers("/test123").access("#oauth2.hasScope('read') or (!#oauth2.isClient() and hasRole('USER'))")*/
        .anyRequest().authenticated()
                .and().exceptionHandling().accessDeniedPage("/testController/access");
	}
	
	@Autowired
	TokenStore tokenStore;

    //configuring our resource server to use Public key which is specified in JwtConfiguration
	@Autowired
	JwtAccessTokenConverter tokenConverter;


	//@EnableAuthorizationServer annotation is used to configure the OAuth 2 Authorization Server mechanism, together with any @Beans that implement AuthorizationServerConfigurer
    //this is responsible for managing tokens

    @Configuration
    @EnableAuthorizationServer
    protected static class AuthServerOAuth2Config extends AuthorizationServerConfigurerAdapter {

        //Configure Clients with Scopes and Grant types

        /*clientId: (required) the client id.
          scope: The scope to which the client is limited. If scope is undefined or empty (the default) the client is not limited by scope.
          authorizedGrantTypes: Grant types that are authorized for the client to use. Default value is empty.
          authorities: Authorities that are granted to the client (regular Spring Security authorities).*/

        @Override
        public void configure(ClientDetailsServiceConfigurer clients) throws Exception {

            clients.inMemory()
                    .withClient("myRestClient") // client id
                    .authorizedGrantTypes("password", "authorization_code", "client_credentials")
                    .authorities("USER")
                    .scopes("read", "trust")
                    .accessTokenValiditySeconds(120).//invalid after 2 minutes.
                    refreshTokenValiditySeconds(600)//refresh after 10 minutes.
                    .and()
                    .withClient("myRestAdmin")
                    .authorizedGrantTypes("password", "authorization_code", "refresh_token", "implicit")
                    .authorities("ADMIN")
                    .scopes("read", "write", "trust")
                    .accessTokenValiditySeconds(120).//invalid after 2 minutes.
                    refreshTokenValiditySeconds(600);//refresh after 10 minutes.
        }

        //this method is responsible for /auth/token. it defines the authorization and token endpoints and the token services
         /*this endpoint provides /oauth/authorize (the authorization endpoint),
         /oauth/token (the token endpoint), /oauth/confirm_access (user posts approval for grants here),
         /oauth/error (used to render errors in the authorization server), /oauth/check_token (used by Resource Servers to decode access tokens)
         /oauth/token_key (exposes public key for token verification if using JWT tokens).*/

        @Override
        public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
            endpoints.tokenStore(tokenStore()).tokenEnhancer(jwtTokenEnhancer()).authenticationManager(authenticationManager);
        }

        //defines the security constraints on the token endpoint. This method is used to verify if the sent method from frontend is correct. It compares using Realm
        @Override
        public void configure(AuthorizationServerSecurityConfigurer oauthServer) throws Exception {
            oauthServer.realm("oauth2/clientUser");
        }

        //in order to use password grant type we need to wire in and use the AuthenticationManager bean
        //this goes to AuthenticationManager method defined in OAuth2SecurityConfig
        @Autowired
        @Qualifier("authenticationManagerBean")
        private AuthenticationManager authenticationManager;

        @Bean
        public TokenStore tokenStore() {
            return new JwtTokenStore(jwtTokenEnhancer());
        }

        //configuring JwtAccessTokenConverter to use our KeyPair from jwt.jks
        protected JwtAccessTokenConverter jwtTokenEnhancer() {
            KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(new ClassPathResource("jwt.jks"), "ftc".toCharArray());
            JwtAccessTokenConverter converter = new JwtAccessTokenConverter();
            converter.setKeyPair(keyStoreKeyFactory.getKeyPair("ftc"));
            return converter;
        }
    }
}