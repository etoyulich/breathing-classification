package com.breathingdetermination;

import com.breathingdetermination.socket.DeterminationSocket;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = DeterminationSocket.class)
class BreathingDeterminationApplicationTests {

    @Test
    void contextLoads() {
    }

}
