package no.nav.foreldrepenger.web.app.healthchecks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;

import no.nav.foreldrepenger.domene.liveness.KafkaIntegration;
import no.nav.foreldrepenger.web.app.healthchecks.checks.ExtHealthCheck;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class SelftestsTest {

    @Inject @Any Instance<ExtHealthCheck> healthChecks;
    @Inject @Any Instance<KafkaIntegration> kafkaIntegrations;

    private HealthCheckRegistry registry;

    private Selftests selftests;

    @Before
    public void setup() {
        registry = Mockito.mock(HealthCheckRegistry.class);

        System.setProperty("develop-local", "true");

        List<ExtHealthCheck> checks = new ArrayList<>();
        List<KafkaIntegration> kafkas = new ArrayList<>();

        for(ExtHealthCheck ex: healthChecks){
            ExtHealthCheck newEx = Mockito.spy(ex);
            Mockito.doReturn(false).when(newEx).erKritiskTjeneste();
            checks.add(newEx);
        }

        for(KafkaIntegration k: kafkaIntegrations){
            KafkaIntegration newK = Mockito.spy(k);
            Mockito.doReturn(true).when(newK).isAlive();
            kafkas.add(newK);
        }

        @SuppressWarnings("unchecked")
        Instance<ExtHealthCheck> testInstance = Mockito.mock(Instance.class);
        Mockito.doReturn(checks.iterator()).when(testInstance).iterator();
        @SuppressWarnings("unchecked")
        Instance<KafkaIntegration> testIntegration = Mockito.mock(Instance.class);
        Mockito.doReturn(checks.iterator()).when(testIntegration).iterator();
        selftests = new Selftests(registry, testInstance, testIntegration, "fpsak");
    }

    @Test
    public void test_run_skal_utfoere_alle_del_tester() {
        SelftestResultat samletResultat = selftests.run();

        assertThat(samletResultat).isNotNull();
        assertThat(samletResultat.getTimestamp()).isNotNull();
        assertThat(samletResultat.getAggregateResult()).isNotNull();
        List<HealthCheck.Result> resultList = samletResultat.getAlleResultater();
        assertThat(resultList).isNotNull();
    }

    @Test
    public void skal_bare_oppdatere_resultatet_hvis_det_har_gått_lenge_siden_forrige_gang() {
        // Act
        selftests.run();
        selftests.run();

        // Assert
        verify(registry, times(1)).getNames();
    }
}
