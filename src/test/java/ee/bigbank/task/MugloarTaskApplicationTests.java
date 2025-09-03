package ee.bigbank.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(classes = MugloarTaskApplication.class)
class MugloarTaskApplicationTests {

    @Autowired ApplicationContext ctx;

    @Test
    void contextLoads() {
        assertThat(ctx).isNotNull();
    }

    @Test
    void springContextHasSomeBeans() {
        assertThat(ctx.getBeanDefinitionCount()).isGreaterThan(0);
        assertThat(ctx.getEnvironment()).isNotNull();
    }
}
