package net.consensys.web3auth.module.adapter.springsecurity;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.client.RestTemplate;

import net.consensys.web3auth.common.dto.ClientDetails;

public class Web3AuthSecurityConfiguration extends WebSecurityConfigurerAdapter {
    private final static String AUTHORIZATION_HEADER = "Authorization";

    private final RestTemplate restTemplate;
    private final String authEndpoint;
    private final ClientDetails client;
    private final String authorizationHeader;

    public Web3AuthSecurityConfiguration(String appId, String clientId, String authEndpoint) {
       this(appId, clientId, authEndpoint, AUTHORIZATION_HEADER);
    }
    
    public Web3AuthSecurityConfiguration(String appId, String clientId, String authEndpoint, String authorizationHeader) {
        this.authEndpoint = authEndpoint;
        this.authorizationHeader = authorizationHeader;
        this.restTemplate = new RestTemplate();
        this.client = restTemplate.getForObject(authEndpoint+"/admin/application/"+appId+"/client/"+clientId, ClientDetails.class);
     }
    
    @Bean
    protected AuthenticationEntryPoint authenticationEntryPoint() throws Exception {
        return new EntryPointUnauthorizedHandler(authEndpoint, client);
    }
    
    @Bean
    protected AuthorisationFilter authorisationFilter() throws Exception {
        return new AuthorisationFilter(authEndpoint, client, authorizationHeader);
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        
        http 
        .csrf().disable()
        .exceptionHandling().authenticationEntryPoint(authenticationEntryPoint())
        .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
            .addFilterBefore(authorisationFilter(), BasicAuthenticationFilter.class);
    }
}
