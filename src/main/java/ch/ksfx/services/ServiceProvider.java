package ch.ksfx.services;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class ServiceProvider
{
    private ApplicationContext applicationContext;

    public ServiceProvider(ApplicationContext applicationContext)
    {
        this.applicationContext = applicationContext;
    }

    public Object getService(Class clazz)
    {
        return applicationContext.getBean(clazz);
    }
}
