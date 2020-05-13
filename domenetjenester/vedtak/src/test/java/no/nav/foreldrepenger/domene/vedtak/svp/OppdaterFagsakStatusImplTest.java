package no.nav.foreldrepenger.domene.vedtak.svp;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakStatusEventPubliserer;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatusFelles;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class OppdaterFagsakStatusImplTest {

    @Mock
    private FagsakStatusEventPubliserer fagsakStatusEventPubliserer;
    @Mock
    private MaksDatoUttakTjeneste maksDatoUttakTjeneste;
    @Mock
    private UttakInputTjeneste uttakInputTjeneste;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void utløpt_ytelsesvedtak() {
        assertThat(erVedtakUtløpt(0)).as("Maksdato uttak er ikke utløpt").isFalse();
        assertThat(erVedtakUtløpt(1)).as("Maksdato utløpt").isTrue();
    }

    private boolean erVedtakUtløpt(int antallDagerEtterMaksdato) {
        LocalDate maksDatoUttak = LocalDate.now().minusDays(antallDagerEtterMaksdato);

        ScenarioMorSøkerSvangerskapspenger scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        Behandling behandling = scenario.lagMocked();
        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null, null);
        Mockito.when(uttakInputTjeneste.lagInput(behandling)).thenReturn(uttakInput);
        Mockito.when(maksDatoUttakTjeneste.beregnMaksDatoUttak(uttakInput)).thenReturn(Optional.of(maksDatoUttak));

        var oppdaterFagsakStatusSVP = new OppdaterFagsakStatusImpl(repositoryProvider.getBehandlingRepository(), new OppdaterFagsakStatusFelles(repositoryProvider, fagsakStatusEventPubliserer),
            maksDatoUttakTjeneste, uttakInputTjeneste);
        return oppdaterFagsakStatusSVP.ingenLøpendeYtelsesvedtak(behandling);
    }
}
