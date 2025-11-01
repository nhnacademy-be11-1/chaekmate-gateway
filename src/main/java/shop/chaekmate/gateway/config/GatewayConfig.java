package shop.chaekmate.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()

                // --- 코어 서버
                .route("core-service", r -> r
                        .path("/books/**", "/categories/**", "/tags/**")
                        .or()
                        .path("/wrappers/**")
                        .uri("lb://chaekmate-core"))

                // -- 코어 서버 (관리자 인가 필요)
                .route("core-admin-service", r -> r
                        .path("/admin/books/**", "/admin/categories/**", "/admin/tags/**")
                        .or()
                        .path("/admin/wrappers/**")
                        .uri("lb://chaekmate-core"))

                // -- 검색 서버
                .route("search-service", r -> r
                        .path("/search/**")
                        .uri("lb://chaekmate-search"))

                .build();
    }
}
