package no.nav.foreldrepenger.batch;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BatchTjenesteTest {

    @Inject
    @Any
    private Instance<BatchTjeneste> batchTjenester;

    @Test
    public void skal_ha_unike_batch_navn() throws Exception {
        final List<String> services = new ArrayList<>();
        final List<String> failed = new ArrayList<>();
        for (BatchTjeneste batchTjeneste : batchTjenester) {
            if (services.contains(batchTjeneste.getBatchName())) {
                failed.add(batchTjeneste.getBatchName());
            }
            services.add(batchTjeneste.getBatchName());
        }
        System.out.println(services);
        assertThat(failed).isEmpty();
    }
}
