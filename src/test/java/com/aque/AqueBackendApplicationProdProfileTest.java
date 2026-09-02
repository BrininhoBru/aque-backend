package com.aque;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "JWT_SECRET=test-secret-key-for-prod-profile-boot-test-minimum-256-bits")
@ActiveProfiles("prod")
@Import(TestcontainersConfiguration.class)
class AqueBackendApplicationProdProfileTest {

    @Test
    void contextLoadsComPerfilProd() {
    }
}
