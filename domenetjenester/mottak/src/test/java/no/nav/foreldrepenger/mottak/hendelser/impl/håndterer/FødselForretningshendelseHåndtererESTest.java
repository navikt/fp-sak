package no.nav.foreldrepenger.mottak.hendelser.impl.håndterer;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakTestUtil;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.Kompletthetskontroller;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.KøKontroller;
import no.nav.foreldrepenger.mottak.hendelser.es.FødselForretningshendelseHåndtererImpl;
import no.nav.foreldrepenger.mottak.hendelser.håndterer.ForretningshendelseHåndtererFelles;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.es.SkjæringstidspunktTjenesteImpl;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.util.FPDateUtil;

@RunWith(CdiRunner.class)
public class FødselForretningshendelseHåndtererESTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    @Inject
    private BehandlingRepository behandlingRepository;

    private ForretningshendelseHåndtererFelles håndtererFelles;
    private FødselForretningshendelseHåndtererImpl håndterer;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private Kompletthetskontroller kompletthetskontroller = mock(Kompletthetskontroller.class);

    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    private Behandling behandling;
    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    @Mock
    private KøKontroller køKontroller;

    private Behandlingsoppretter behandlingsoppretter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        BehandlingskontrollTjeneste behandlingskontrollTjeneste = DokumentmottakTestUtil.lagBehandlingskontrollTjenesteMock(serviceProvider);

        behandlingsoppretter = new Behandlingsoppretter(repositoryProvider,
            behandlingskontrollTjeneste,
            null,
            mottatteDokumentTjeneste,
            behandlendeEnhetTjeneste);
        skjæringstidspunktTjeneste = new SkjæringstidspunktTjenesteImpl(repositoryProvider, null);
        håndtererFelles = new ForretningshendelseHåndtererFelles(historikkinnslagTjeneste, kompletthetskontroller,
            behandlingProsesseringTjeneste, behandlingsoppretter, køKontroller);
        håndterer = new FødselForretningshendelseHåndtererImpl(håndtererFelles, Period.ofWeeks(11), skjæringstidspunktTjeneste, null);
    }

    // Vil bare gjøre noe rundt årsskifte
    @Ignore
    @Test
    public void skal_opprette_revurdering_når_hendelse_er_fødsel_finnes_vedtak() {
        // Arrange
        LocalDate termindato = LocalDate.now();
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medBehandlingStegStart(BehandlingStegType.IVERKSETT_VEDTAK);
        scenario.medSøknadHendelse().medTerminbekreftelse(scenario.medSøknadHendelse().getTerminbekreftelseBuilder()
            .medUtstedtDato(termindato)
            .medTermindato(termindato)
            .medNavnPå("LEGEN MIN"))
            .medAntallBarn(1);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("Nav Navesen")
            .build();
        behandling = scenario.lagre(repositoryProvider);

        behandling = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(behandling.erUnderIverksettelse()).isTrue();

        // Act
        System.setProperty("funksjonelt.tidsoffset.offset", Period.between(LocalDate.now(), LocalDate.now().plusMonths(1)).toString());
        FPDateUtil.init();

        håndterer.håndterAvsluttetBehandling(behandling, ForretningshendelseType.FØDSEL, RE_ENDRING_BEREGNINGSGRUNNLAG);

        System.clearProperty("funksjonelt.tidsoffset.offset");
        FPDateUtil.init();


        // Assert
        behandling = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(behandling.erUnderIverksettelse()).isTrue();

        Optional<Behandling> revurdering = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(behandling.getFagsakId());
        assertThat(revurdering.get().erRevurdering()).isTrue();
    }

    @Test
    public void skal_ta_av_vent_når_hendelse_er_fødsel_mangler_registrering() {
        // Arrange
        var scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        scenario.medSøknadHendelse().medFødselsDato(LocalDate.now().minusDays(2)).medAntallBarn(1);
        scenario.medBehandlingStegStart(BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING, BehandlingStegType.KONTROLLER_FAKTA);
        behandling = scenario.lagre(repositoryProvider);

        behandling = behandlingRepository.hentBehandling(behandling.getId());

        // Act
        håndterer.håndterÅpenBehandling(behandling, RE_ENDRING_BEREGNINGSGRUNNLAG);

        // Assert
        verify(kompletthetskontroller).vurderNyForretningshendelse(eq(behandling));
    }
}
