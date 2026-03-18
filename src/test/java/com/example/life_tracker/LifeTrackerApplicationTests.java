package com.example.life_tracker;

import com.example.life_tracker.IT.IntegrationTestBase;
import com.example.life_tracker.IT.TestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
		"GOOGLE_API_KEY=fake-key-for-tests"
})
@Import(TestConfig.class)
class LifeTrackerApplicationTests extends IntegrationTestBase {

	@Test
	void contextLoads() {
	}

}
