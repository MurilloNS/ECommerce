package br.com.murillo.auditservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class AuditserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuditserviceApplication.class, args);
	}

}
