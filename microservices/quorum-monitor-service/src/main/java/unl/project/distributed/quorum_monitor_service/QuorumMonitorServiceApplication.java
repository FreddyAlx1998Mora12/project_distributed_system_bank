package unl.project.distributed.quorum_monitor_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class QuorumMonitorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(QuorumMonitorServiceApplication.class, args);
	}

}
