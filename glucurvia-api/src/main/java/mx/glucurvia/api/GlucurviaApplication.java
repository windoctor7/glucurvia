package mx.glucurvia.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada. Escanea todos los módulos bajo mx.glucurvia: los @Service y @RestController de
 * cada módulo se descubren sin registrar nada aquí. Este módulo lo edita solo el humano.
 */
@SpringBootApplication(scanBasePackages = "mx.glucurvia")
@EnableAsync
@EnableScheduling
public class GlucurviaApplication {

  public static void main(String[] args) {
    SpringApplication.run(GlucurviaApplication.class, args);
  }
}
