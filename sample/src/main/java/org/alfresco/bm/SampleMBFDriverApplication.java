package org.alfresco.bm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource("classpath:config/spring/app-context.xml")
public class SampleMBFDriverApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(SampleMBFDriverApplication.class, args);
    }
}
