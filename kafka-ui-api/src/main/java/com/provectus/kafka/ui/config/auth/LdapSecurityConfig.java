package com.provectus.kafka.ui.config.auth;

import static com.provectus.kafka.ui.config.auth.AbstractAuthSecurityConfig.AUTH_WHITELIST;

import com.provectus.kafka.ui.service.rbac.AccessControlService;
import com.provectus.kafka.ui.service.rbac.extractor.RbacLdapAuthoritiesExtractor;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.ReactiveAuthenticationManagerAdapter;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.AbstractLdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.LdapUserDetailsMapper;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@ConditionalOnProperty(value = "auth.type", havingValue = "LDAP")
@Import(LdapAutoConfiguration.class)
@EnableConfigurationProperties(LdapProperties.class)
@RequiredArgsConstructor
@Slf4j
public class LdapSecurityConfig {

  private final LdapProperties props;

  @Bean
  public ReactiveAuthenticationManager authenticationManager(BaseLdapPathContextSource contextSource,
                                                             ApplicationContext context,
                                                             @Nullable AccessControlService acs) {
    var rbacEnabled = acs != null && acs.isRbacEnabled();
    BindAuthenticator ba = new BindAuthenticator(contextSource);
    if (props.getBase() != null) {
      ba.setUserDnPatterns(new String[] {props.getBase()});
    }
    if (props.getUserFilterSearchBase() != null) {
      LdapUserSearch userSearch =
          new FilterBasedLdapUserSearch(props.getUserFilterSearchBase(), props.getUserFilterSearchFilter(),
              contextSource);
      ba.setUserSearch(userSearch);
    }

    AbstractLdapAuthenticationProvider authenticationProvider;
    if (!props.isActiveDirectory()) {
      authenticationProvider = rbacEnabled
          ? new LdapAuthenticationProvider(ba, new RbacLdapAuthoritiesExtractor(context))
          : new LdapAuthenticationProvider(ba);
    } else {
      authenticationProvider = new ActiveDirectoryLdapAuthenticationProvider(props.getActiveDirectoryDomain(),
          props.getUrls()); // TODO authority extractor
      authenticationProvider.setUseAuthenticationRequestCredentials(true);
    }

    if (rbacEnabled) {
      authenticationProvider.setUserDetailsContextMapper(new UserDetailsMapper());
    }

    AuthenticationManager am = new ProviderManager(List.of(authenticationProvider));

    return new ReactiveAuthenticationManagerAdapter(am);
  }

  @Bean
  @Primary
  public BaseLdapPathContextSource contextSource() {
    LdapContextSource ctx = new LdapContextSource();
    ctx.setUrl(props.getUrls());
    ctx.setUserDn(props.getAdminUser());
    ctx.setPassword(props.getAdminPassword());
    ctx.afterPropertiesSet();
    return ctx;
  }

  @Bean
  public SecurityWebFilterChain configureLdap(ServerHttpSecurity http) {
    log.info("Configuring LDAP authentication.");
    if (props.isActiveDirectory()) {
      log.info("Active Directory support for LDAP has been enabled.");
    }

//    http.authenticationManager(authenticationManager())

    return http
        .authorizeExchange()
        .pathMatchers(AUTH_WHITELIST)
        .permitAll()
        .anyExchange()
        .authenticated()

        .and()
        .formLogin()

        .and()
        .logout()

        .and()
        .csrf().disable()
        .build();
  }

  private static class UserDetailsMapper extends LdapUserDetailsMapper {
    @Override
    public UserDetails mapUserFromContext(DirContextOperations ctx, String username,
                                          Collection<? extends GrantedAuthority> authorities) {
      UserDetails userDetails = super.mapUserFromContext(ctx, username, authorities);
      return new RbacLdapUser(userDetails);
    }
  }

}

