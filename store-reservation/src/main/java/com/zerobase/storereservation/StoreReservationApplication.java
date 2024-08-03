package com.zerobase.storereservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.persistence.EntityListeners;

@SpringBootApplication
@EnableJpaRepositories
@EnableJpaAuditing
@EnableSwagger2
public class StoreReservationApplication {

	public static void main(String[] args) {
		SpringApplication.run(StoreReservationApplication.class, args);
	}

}
