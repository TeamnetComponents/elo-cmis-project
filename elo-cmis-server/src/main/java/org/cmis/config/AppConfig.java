package org.cmis.config;

import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @author andreeaf
 * @since 6/27/14 10:36 AM
 *
 * Clasa de configurare aplicatie ACP.
 * Se seteaza proprietati specifice JPA, Hibernate, se expune DataSource, TransactionManager, EntityManagerFactory.
 */
@Configuration
@ImportResource({ "classpath*:/elo-cmis-application.xml"})
@ComponentScan( basePackages = "org.cmis" )
@Order(Ordered.LOWEST_PRECEDENCE)
public class AppConfig {

}
