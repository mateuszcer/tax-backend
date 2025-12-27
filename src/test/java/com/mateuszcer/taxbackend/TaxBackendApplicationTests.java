package com.mateuszcer.taxbackend;

import com.mateuszcer.taxbackend.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class TaxBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
