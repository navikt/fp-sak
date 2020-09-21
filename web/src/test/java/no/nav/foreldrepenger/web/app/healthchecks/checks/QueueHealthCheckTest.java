package no.nav.foreldrepenger.web.app.healthchecks.checks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.jms.JMSException;
import javax.jms.JMSRuntimeException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.vedtak.felles.integrasjon.jms.QueueSelftest;

@ExtendWith(MockitoExtension.class)
public class QueueHealthCheckTest {

    private QueueHealthCheck queueHealthCheck; // objektet vi tester

    @Mock
    private QueueSelftest mockJmsSelftestSupport;

    private static final String DESCR_SUFFIX = "my-oh-my";

    @BeforeEach
    public void setup() {
        queueHealthCheck = new MyQueueHealthCheck(mockJmsSelftestSupport);
    }

    @Test
    public void test_0argCtor() {
        new MyQueueHealthCheck();
    }

    @Test
    public void test_getDescription() {
        String descr = queueHealthCheck.getDescription();

        assertThat(descr).contains(DESCR_SUFFIX);
    }

    @Test
    public void test_getEndpoint_ok() {
        when(mockJmsSelftestSupport.getConnectionEndpoint()).thenReturn("TheEnd");

        String endpt = queueHealthCheck.getEndpoint();

        assertThat(endpt).isEqualTo("TheEnd");
    }

    @Test
    public void test_getEndpoint_exception() {
        when(mockJmsSelftestSupport.getConnectionEndpoint()).thenThrow(new RuntimeException("uff"));

        String endpt = queueHealthCheck.getEndpoint();

        assertThat(endpt).isNotNull();
    }

    @Test
    public void test_performCheck_connectionOk() throws JMSException {
        ExtHealthCheck.InternalResult res = queueHealthCheck.performCheck();

        verify(mockJmsSelftestSupport).testConnection();
        assertThat(res.isOk()).isTrue();
    }

    @Test
    public void test_performCheck_exception1() throws JMSException {
        doThrow(new JMSRuntimeException("meh")).when(mockJmsSelftestSupport).testConnection();

        ExtHealthCheck.InternalResult res = queueHealthCheck.performCheck();

        verify(mockJmsSelftestSupport).testConnection();
        assertThat(res.isOk()).isFalse();
        assertThat(res.getMessage()).isNotNull();
        assertThat(res.getException()).isNull();
    }

    @Test
    public void test_performCheck_exception2() throws JMSException {
        doThrow(new JMSException("meh!")).when(mockJmsSelftestSupport).testConnection();

        ExtHealthCheck.InternalResult res = queueHealthCheck.performCheck();

        verify(mockJmsSelftestSupport).testConnection();
        assertThat(res.isOk()).isFalse();
        assertThat(res.getMessage()).isNotNull();
        assertThat(res.getException()).isNull();
    }

    @Test
    public void test_performCheck_exception3() throws JMSException {
        doThrow(new RuntimeException("meh!!!")).when(mockJmsSelftestSupport).testConnection();

        ExtHealthCheck.InternalResult res = queueHealthCheck.performCheck();

        verify(mockJmsSelftestSupport).testConnection();
        assertThat(res.isOk()).isFalse();
        assertThat(res.getMessage()).isNull();
        assertThat(res.getException()).isNotNull();
    }

    // ----------------

    private static class MyQueueHealthCheck extends QueueHealthCheck {

        public MyQueueHealthCheck(QueueSelftest client) {
            super(client);
        }

        public MyQueueHealthCheck() {
            super();
        }

        @Override
        protected String getDescriptionSuffix() {
            return DESCR_SUFFIX;
        }
    }
}
