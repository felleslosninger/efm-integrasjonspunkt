package integrasjonspunkt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = IntegrasjonspunktMonitorApplication.class)
@WebIntegrationTest
public class IntegrasjonspunktMonitorApplicationTests {

    @Test
    public void contextLoads() {
        // Tests that the configuration loads
    }

}
