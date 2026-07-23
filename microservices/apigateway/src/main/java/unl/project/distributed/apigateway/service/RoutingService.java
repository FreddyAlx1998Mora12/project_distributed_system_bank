package unl.project.distributed.apigateway.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RoutingService {

    private final RestClient restClient;

    @Value("${loadbalancer.url:http://load-balancer:9000}")
    private String loadBalancerUrl;

    public RoutingService(RestClient restClient) {
        this.restClient = restClient;
    }

    public Map<String, Object> route(String operation, Map<String, Object> body) {
        return restClient.post()
                .uri(loadBalancerUrl + "/route/" + operation)
                .body(body)
                .retrieve()
                .body(Map.class);
    }
}
