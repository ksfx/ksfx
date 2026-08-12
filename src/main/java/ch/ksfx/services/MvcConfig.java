package ch.ksfx.services;

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

    public MvcConfig(AgenticApiAccessInterceptor agenticApiAccessInterceptor)
    {
        this.agenticApiAccessInterceptor = agenticApiAccessInterceptor;
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
    }

}
