package ch.ksfx.services;


import ch.ksfx.dao.ApiClientDAO;
import ch.ksfx.services.security.ApiClientAuthenticationProvider;
import ch.ksfx.services.security.ApiTokenAuthenticationFilter;
import ch.ksfx.services.security.ApiUnauthorizedEntryPoint;
import ch.ksfx.services.user.KsfxUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter
{
    @Override
    protected void configure(HttpSecurity http) throws Exception
    {
        http
                .authorizeRequests()
                .antMatchers("/", "/home","/toggledisplaymode", "/images/**", "/styles/**", "/script/**", "/publishing/publicationviewer/**", "/agentic/api/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .formLogin()
                .loginPage("/login")
                .permitAll()
                .and()
                .logout()
                .logoutSuccessUrl("/")
                .permitAll()
                .and()
                // /agentic/api/** is the agent self-service scheduling API, called via curl from
                // inside the headless Claude CLI's Bash tool - no browser session/CSRF token
                // available to it, so it's permitAll() above and does its own bearer-token check
                // in AgentScheduleApiController instead. /api/** is the newer, Spring-Security-
                // integrated bearer-token API (see ApiTokenAuthenticationFilter /
                // ApiClientAuthenticationProvider below) - it's NOT permitAll above, it relies on
                // anyRequest().authenticated() plus that filter/provider instead - but its callers
                // are equally token-only, so it needs the same CSRF exemption.
                .csrf()
                .ignoringAntMatchers("/agentic/api/**", "/api/**")
                .and()
                .exceptionHandling()
                // Explicit catch-all as well as the /api/** mapping - registering ANY mapping here
                // replaces the implicit default (normally auto-derived from formLogin's login page)
                // with a DelegatingAuthenticationEntryPoint that has no fallback unless one is given
                // explicitly, so without this line every other unauthenticated request in the app
                // (not just /api/**) would get the JSON 401 too instead of being redirected to
                // /login. Order matters - DelegatingAuthenticationEntryPoint uses the first matching
                // entry, so the specific /api/** mapping must come before this catch-all.
                .defaultAuthenticationEntryPointFor(apiUnauthorizedEntryPoint(), new AntPathRequestMatcher("/api/**"))
                .defaultAuthenticationEntryPointFor(loginUrlAuthenticationEntryPoint(), new AntPathRequestMatcher("/**"))
                .and()
                .addFilterBefore(new ApiTokenAuthenticationFilter(authenticationManagerBean()), UsernamePasswordAuthenticationFilter.class);

        http.headers().frameOptions().sameOrigin();
    }

    @Override
    public void configure(WebSecurity web) throws Exception
    {
        super.configure(web);
        web.httpFirewall(allowUrlEncodedSlashHttpFirewall());
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, KsfxUserDetailsService ksfxUserDetailsService, ApiClientDAO apiClientDAO) throws Exception
    {
        auth.authenticationProvider(authenticationProvider(ksfxUserDetailsService));
        auth.authenticationProvider(apiClientAuthenticationProvider(apiClientDAO));
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(KsfxUserDetailsService ksfxUserDetailsService)
    {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        System.out.println("Encrypted Password: " + encoder.encode("12345"));

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(ksfxUserDetailsService);
        authProvider.setPasswordEncoder(encoder);

        return authProvider;
    }

    @Bean
    public ApiClientAuthenticationProvider apiClientAuthenticationProvider(ApiClientDAO apiClientDAO)
    {
        return new ApiClientAuthenticationProvider(apiClientDAO);
    }

    @Bean
    public ApiUnauthorizedEntryPoint apiUnauthorizedEntryPoint()
    {
        return new ApiUnauthorizedEntryPoint();
    }

    @Bean
    public AuthenticationEntryPoint loginUrlAuthenticationEntryPoint()
    {
        return new LoginUrlAuthenticationEntryPoint("/login");
    }

    @Bean
    public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowUrlEncodedPercent(true);
        firewall.setAllowBackSlash(true);
        firewall.setAllowUrlEncodedSlash(true);
        firewall.setAllowSemicolon(true);
        firewall.setAllowUrlEncodedDoubleSlash(true);

        return firewall;
    }
}
