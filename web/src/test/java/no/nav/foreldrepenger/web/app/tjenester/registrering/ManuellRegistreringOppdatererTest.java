package no.nav.foreldrepenger.web.app.tjenester.registrering;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
class ManuellRegistreringOppdatererTest {

    @Inject
    @Any
    private ManuellRegistreringOppdaterer registreringOppdaterer;

    @Test
    void test_har_instanser() {
        assertThat(registreringOppdaterer.finnSøknadMapper(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD)).isNotNull();

        assertThat(registreringOppdaterer.finnSøknadMapper(FagsakYtelseType.FORELDREPENGER, BehandlingType.REVURDERING)).isNotNull();

        assertThat(registreringOppdaterer.finnSøknadMapper(FagsakYtelseType.SVANGERSKAPSPENGER, BehandlingType.FØRSTEGANGSSØKNAD)).isNotNull();

        assertThat(registreringOppdaterer.finnSøknadMapper(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.FØRSTEGANGSSØKNAD)).isNotNull();

    }
}
