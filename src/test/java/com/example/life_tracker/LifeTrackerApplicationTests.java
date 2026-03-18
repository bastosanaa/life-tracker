package com.example.life_tracker;

import com.example.life_tracker.integration.IntegrationTestBase;
import com.example.life_tracker.integration.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "GOOGLE_API_KEY=fake-key-for-tests")
@Import(TestConfig.class)
class LifeTrackerApplicationTests extends IntegrationTestBase {

	@Test
	void contextLoads() {
	}

}
