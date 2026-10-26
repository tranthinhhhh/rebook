package vn.hcmute.rebook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class RebookApplication {
    public static void main(String[] args) {
        SpringApplication.run(RebookApplication.class, args);
    }
}
