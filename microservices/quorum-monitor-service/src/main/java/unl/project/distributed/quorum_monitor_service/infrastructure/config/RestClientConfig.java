package unl.project.distributed.quorum_monitor_service.infrastructure.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * RestClient con timeouts EXPLÍCITOS de conexión y lectura. Sin esto, un nodo
     * que no responde (no que rechace la conexión, sino que se quede colgado)
     * podría bloquear el hilo del chequeo indefinidamente con el timeout por
     * defecto del JDK -> rompería la detección oportuna de fallos.
     */
    @Bean
    public RestClient restClient(ClusterProperties properties) {
        long timeoutMs = properties.healthCheck().timeoutMs();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(timeoutMs))
                        .build()
        );
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));

        return RestClient.builder().requestFactory(factory).build();
    }
}
