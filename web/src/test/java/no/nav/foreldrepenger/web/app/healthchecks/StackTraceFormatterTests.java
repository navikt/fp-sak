package no.nav.foreldrepenger.web.app.healthchecks;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class StackTraceFormatterTests {

    @Test
    public void test() {
        Exception e;
        try {
            throw new Exception("oi!");
        } catch (Exception e2) {
            e = e2;
        }

        String s = StackTraceFormatter.format(e);
        assertThat(s != null).isTrue();
        assertThat(s.contains("oi!")).isTrue();
    }
}
