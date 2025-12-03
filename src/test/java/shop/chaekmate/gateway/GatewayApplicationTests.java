package shop.chaekmate.gateway;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class GatewayApplicationTests {

    @Configuration
    static class TestConfig {
        @Bean
        public OpenTelemetrySdk openTelemetrySdk() {
            return Mockito.mock(OpenTelemetrySdk.class); // OTEL SDK Mock
        }
    }

	@Test
	void contextLoads() {
		// 스프링 컨텍스트 로드 테스트
	}

}
