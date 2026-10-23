package ch.ksfx.services;

import ch.ksfx.services.project.NoProjectInterceptor;
import ch.ksfx.services.agentic.AgenticApiAccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class MvcConfig implements WebMvcConfigurer
{
    private final AgenticApiAccessInterceptor agenticApiAccessInterceptor;
    private final NoProjectInterceptor noProjectInterceptor;

    public MvcConfig(AgenticApiAccessInterceptor agenticApiAccessInterceptor, NoProjectInterceptor noProjectInterceptor)
    {
        this.agenticApiAccessInterceptor = agenticApiAccessInterceptor;
        this.noProjectInterceptor = noProjectInterceptor;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry)
    {
        registry.addViewController("/home").setViewName("home");
        registry.addViewController("/").setViewName("home");
        registry.addViewController("/login").setViewName("login");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(agenticApiAccessInterceptor).addPathPatterns("/agentic/api/**");
        // Everything a browser user reaches, except what works without a project: the page itself,
        // login/logout, the display-mode toggle, static files, the public publication viewer and
        // both APIs (agents and API clients are not users).
        registry.addInterceptor(noProjectInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/noproject", "/user/**", "/ports/ask", "/login", "/logout", "/toggledisplaymode", "/images/**", "/styles/**", "/script/**", "/vendor/**",
                        "/publishing/publicationviewer/**", "/agentic/api/**", "/api/**", "/error");
    }

}
