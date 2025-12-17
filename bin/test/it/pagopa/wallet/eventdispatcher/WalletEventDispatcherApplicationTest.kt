package it.pagopa.wallet.eventdispatcher

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(locations = ["classpath:application.properties"])
class WalletEventDispatcherApplicationTest {

    @Test
    fun contextLoads() {
        assertTrue(true)
    }
}
