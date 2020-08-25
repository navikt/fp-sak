package no.nav.foreldrepenger.domene.vedtak.svp;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakStatusEventPubliserer;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
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
    public void har_løpende_ytelsesvedtak_() {
        //Arrange
        LocalDate maksDatoUttak = LocalDate.now().minusDays(0);

        ScenarioMorSøkerSvangerskapspenger scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        Behandling behandling = scenario.lagMocked();
        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null, null);
        Mockito.when(uttakInputTjeneste.lagInput(behandling)).thenReturn(uttakInput);
        Mockito.when(maksDatoUttakTjeneste.beregnMaksDatoUttak(uttakInput)).thenReturn(Optional.of(maksDatoUttak));

        var oppdaterFagsakStatusSVP = new OppdaterFagsakStatusImpl(repositoryProvider.getBehandlingRepository(),repositoryProvider.getFagsakRepository(),fagsakStatusEventPubliserer,repositoryProvider.getBehandlingsresultatRepository(),maksDatoUttakTjeneste, uttakInputTjeneste);
        //Act
        boolean ingenLøpendeYtelsesvedtak = oppdaterFagsakStatusSVP.ingenLøpendeYtelsesvedtak(behandling);

        //Assert
        assertThat(ingenLøpendeYtelsesvedtak).as("Maksdato uttak er ikke utløpt").isFalse();

    }

    @Test
    public void ingen_løpende_ytelsesvedtak() {
        //Arrange
        LocalDate maksDatoUttak = LocalDate.now().minusDays(1);

        ScenarioMorSøkerSvangerskapspenger scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        Behandling behandling = scenario.lagMocked();
        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null, null);
        Mockito.when(uttakInputTjeneste.lagInput(behandling)).thenReturn(uttakInput);
        Mockito.when(maksDatoUttakTjeneste.beregnMaksDatoUttak(uttakInput)).thenReturn(Optional.of(maksDatoUttak));

        var oppdaterFagsakStatusSVP = new OppdaterFagsakStatusImpl(repositoryProvider.getBehandlingRepository(),repositoryProvider.getFagsakRepository(),fagsakStatusEventPubliserer,repositoryProvider.getBehandlingsresultatRepository(),maksDatoUttakTjeneste, uttakInputTjeneste);

        //Act
        boolean ingenLøpendeYtelsesvedtak = oppdaterFagsakStatusSVP.ingenLøpendeYtelsesvedtak(behandling);

        //Assert
        assertThat(ingenLøpendeYtelsesvedtak).as("Maksdato utløpt").isTrue();

    }

}
