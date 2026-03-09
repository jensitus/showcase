package at.cibseven.cibdemo;

import org.cibseven.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableProcessApplication
public class CibdemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(CibdemoApplication.class, args);
	}

}
