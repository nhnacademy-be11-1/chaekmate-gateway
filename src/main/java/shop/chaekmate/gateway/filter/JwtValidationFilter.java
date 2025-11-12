package shop.chaekmate.gateway.filter;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import shop.chaekmate.gateway.dto.MemberInfoResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtValidationFilter implements GlobalFilter, Ordered {

    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    // 헤더 이름
    private static final String X_MEMBER_ID = "X-Member-Id";
    private static final String X_USER_ROLE = "X-User-Role";

    private final WebClient.Builder webClientBuilder;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String accessToken = extractAccessTokenFromCookie(request);

        // 토큰이 없으면 그대로 통과 (인증이 필요 없는 경로일 수 있으니까)
        if (!StringUtils.hasText(accessToken)) {
            return chain.filter(exchange);
        }

        // Auth 서버에 토큰 검증 요청함
        WebClient webClient = webClientBuilder.build();

        return webClient.get()
                .uri("lb://chaekmate-auth/auth/me") // auth서버 하나지만 그래도 lb 우선 붙임
                .header("Cookie", ACCESS_TOKEN_COOKIE_NAME + "=" + accessToken)
                .retrieve()
                .bodyToMono(MemberInfoResponse.class)
                .flatMap(memberInfo -> {
                    // 헤더에 사용자 정보 추가(core서버에 전달해주려고)
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header(X_MEMBER_ID, String.valueOf(memberInfo.memberId()))
                            .header(X_USER_ROLE, memberInfo.role())
                            .build();

                    log.debug("JWT 검증 성공: memberId={}, role={}", memberInfo.memberId(), memberInfo.role());
                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                })
                .onErrorResume(e -> {
                    log.warn("JWT 검증 실패: {}", e.getMessage());
                    // 검증 실패 시 401 반환
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return response.setComplete();
                });
    }

    private String extractAccessTokenFromCookie(ServerHttpRequest request) {
        List<String> cookies = request.getHeaders().get("Cookie");
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }

        for (String cookieHeader : cookies) {
            String[] cookiePairs = cookieHeader.split(";");
            for (String cookiePair : cookiePairs) {
                String[] keyValue = cookiePair.trim().split("=", 2);
                if (keyValue.length == 2 && ACCESS_TOKEN_COOKIE_NAME.equals(keyValue[0].trim())) {
                    return keyValue[1].trim();
                }
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        // 필터 실행 순서 (낮을수록 먼저 실행)
        return -100;
    }
}
