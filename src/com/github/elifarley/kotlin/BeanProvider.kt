package com.orgecc.myproj

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class BeanProvider {

    @Value("\${myproj.subject.dir}")
    lateinit var subjectDir: String

    @Autowired
    lateinit var otherService: OtherService

    @Bean open fun subjectBean() =
            dirWatcherBean(subjectDir, otherService.handler, sleepInMillis = 1000 * 60 * 3)

    @Bean open fun subjectBean2() =
            dirWatcherBean(subjectDir, otherService.handler, sleepInMillis = 1000 * 60 * 3)

}
