package no.nav.foreldrepenger.batch;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@CdiDbAwareTest
class BatchTjenesteTest {

    @Inject
    @Any
    private Instance<BatchTjeneste> batchTjenester;

    @Test
    void skal_ha_unike_batch_navn() {
        final List<String> services = new ArrayList<>();
        final List<String> failed = new ArrayList<>();
        for (var batchTjeneste : batchTjenester) {
            if (services.contains(batchTjeneste.getBatchName())) {
                failed.add(batchTjeneste.getBatchName());
            }
            services.add(batchTjeneste.getBatchName());
        }
        assertThat(services).isNotEmpty();
        assertThat(failed).isEmpty();
    }
}
