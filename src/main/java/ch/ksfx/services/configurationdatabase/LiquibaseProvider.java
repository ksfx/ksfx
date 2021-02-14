package ch.ksfx.services.configurationdatabase;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class LiquibaseProvider
{
    @Bean
    @Autowired
    public SpringLiquibase liquibase(ConfigurationDatabaseProvider configurationDatabaseProvider)
    {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:dbmigration/liquibase-changeLog.xml");
        liquibase.setDataSource(new EbeanSimpleDataSource());
        return liquibase;
    }
}
