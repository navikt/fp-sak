package no.nav.foreldrepenger.domene.vedtak.fp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.FagsakStatusEventPubliserer;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatusFelles;
import no.nav.vedtak.felles.testutilities.Whitebox;

public class OppdaterFagsakStatusImplTest {

    @Mock
    private FagsakStatusEventPubliserer fagsakStatusEventPubliserer;
    @Mock
    private MaksDatoUttakTjeneste maksDatoUttakTjeneste;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private UttakInputTjeneste uttakInputTjeneste;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void utløpt_ytelsesvedtak() {
        assertThat(erVedtakUtløpt(0, 3, Period.ofYears(3))).as("Hverken maksdato uttak eller fødsel utløpt").isFalse();
        assertThat(erVedtakUtløpt(1, 3, Period.ofYears(3))).as("Maksdato utløpt").isTrue();
        assertThat(erVedtakUtløpt(1, 3, Period.ofYears(3))).as("Fødsel foreldelsesfrist utløpt").isTrue();
    }

    @Test
    public void avslått_ytelsesvedtak() {
        assertThat(erVedtakDirekteAvsluttbart(VedtakResultatType.AVSLAG)).as("Vedtak AVSLAG avsluttes direkte").isTrue();
        assertThat(erVedtakDirekteAvsluttbart(VedtakResultatType.OPPHØR)).as("Vedtak OPPHØR avsluttes direkte").isTrue();
        assertThat(erVedtakDirekteAvsluttbart(VedtakResultatType.INNVILGET)).as("Vedtak INNVILGET avsluttes direkte").isFalse();
    }

    private boolean erVedtakDirekteAvsluttbart(VedtakResultatType vedtakResultatType) {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        BehandlingVedtak behandlingVedtak = scenario.medBehandlingVedtak().medVedtakResultatType(vedtakResultatType).build();
        Behandling behandling = scenario.lagMocked();
        Whitebox.setInternalState(behandling.getBehandlingsresultat(), "behandlingVedtak", behandlingVedtak);

        var foreldelsesfrist = Period.ofYears(100); // Kun for teset
        var repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null, null);
        Mockito.when(uttakInputTjeneste.lagInput(behandling)).thenReturn(uttakInput);
        Mockito.when(maksDatoUttakTjeneste.beregnMaksDatoUttak(uttakInput)).thenReturn(Optional.empty());
        Mockito.when(repositoryProvider.getBehandlingsresultatRepository()).thenReturn(behandlingsresultatRepository);
        Mockito.when(behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())).thenReturn(Optional.of(behandling.getBehandlingsresultat()));

        var oppdaterFagsakStatusFP = new OppdaterFagsakStatusImpl(repositoryProvider, new OppdaterFagsakStatusFelles(repositoryProvider, fagsakStatusEventPubliserer),
            maksDatoUttakTjeneste, uttakInputTjeneste, foreldelsesfrist);
        return oppdaterFagsakStatusFP.ingenLøpendeYtelsesvedtak(behandling);
    }

    private boolean erVedtakUtløpt(int antallDagerEtterMaksdato, int antallÅrSidenFødsel, Period foreldelsesfrist) {
        LocalDate fødselsDato = LocalDate.now().minusYears(antallÅrSidenFødsel);
        LocalDate maksDatoUttak = LocalDate.now().minusDays(antallDagerEtterMaksdato);

        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBekreftetHendelse().medFødselsDato(fødselsDato);
        Behandling behandling = scenario.lagMocked();
        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        var uttakInput = new UttakInput(BehandlingReferanse.fra(behandling), null, null);
        Mockito.when(uttakInputTjeneste.lagInput(behandling)).thenReturn(uttakInput);
        Mockito.when(maksDatoUttakTjeneste.beregnMaksDatoUttak(uttakInput)).thenReturn(Optional.of(maksDatoUttak));
        Mockito.when(repositoryProvider.getBehandlingsresultatRepository()).thenReturn(behandlingsresultatRepository);

        var oppdaterFagsakStatusFP = new OppdaterFagsakStatusImpl(repositoryProvider, new OppdaterFagsakStatusFelles(repositoryProvider, fagsakStatusEventPubliserer),
            maksDatoUttakTjeneste, uttakInputTjeneste, foreldelsesfrist);
        return oppdaterFagsakStatusFP.ingenLøpendeYtelsesvedtak(behandling);
    }

}
