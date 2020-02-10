package no.nav.foreldrepenger.web.app.tjenester.registrering;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class ManuellRegistreringOppdatererTest {

    @Inject
    @Any
    ManuellRegistreringOppdaterer registreringOppdaterer;

    @Test
    public void test_har_instanser() throws Exception {
        assertThat(registreringOppdaterer.finnSøknadMapper(FagsakYtelseType.FORELDREPENGER, BehandlingType.FØRSTEGANGSSØKNAD)).isNotNull();

        assertThat(registreringOppdaterer.finnSøknadMapper(FagsakYtelseType.FORELDREPENGER, BehandlingType.REVURDERING)).isNotNull();

        assertThat(registreringOppdaterer.finnSøknadMapper(FagsakYtelseType.SVANGERSKAPSPENGER, BehandlingType.FØRSTEGANGSSØKNAD)).isNotNull();

        assertThat(registreringOppdaterer.finnSøknadMapper(FagsakYtelseType.ENGANGSTØNAD, BehandlingType.FØRSTEGANGSSØKNAD)).isNotNull();

    }
}
