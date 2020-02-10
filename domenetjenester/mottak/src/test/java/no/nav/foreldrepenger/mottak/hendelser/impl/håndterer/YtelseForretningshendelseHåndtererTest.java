package no.nav.foreldrepenger.mottak.hendelser.impl.håndterer;

import static no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFelt;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakTestUtil;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.Kompletthetskontroller;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.KøKontroller;
import no.nav.foreldrepenger.mottak.hendelser.håndterer.ForretningshendelseHåndtererFelles;
import no.nav.foreldrepenger.mottak.hendelser.håndterer.YtelseForretningshendelseHåndterer;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class YtelseForretningshendelseHåndtererTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private BehandlingskontrollServiceProvider serviceProvider;

    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private HistorikkRepository historikkRepository;

    private ForretningshendelseHåndtererFelles håndtererFelles;
    private YtelseForretningshendelseHåndterer håndterer;
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
            behandlendeEnhetTjeneste,
            historikkinnslagTjeneste);
        håndtererFelles = new ForretningshendelseHåndtererFelles(historikkinnslagTjeneste, kompletthetskontroller,
            behandlingProsesseringTjeneste, behandlingsoppretter, køKontroller);
        håndterer = new YtelseForretningshendelseHåndterer(repositoryProvider, håndtererFelles);
    }

    @Test
    public void skal_opprette_revurdering_når_hendelse_er_endring_og_behandling_er_iverksett_vedtak() {
        // Arrange
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medBehandlingStegStart(BehandlingStegType.IVERKSETT_VEDTAK);
        scenario.medBehandlingVedtak()
            .medVedtakstidspunkt(LocalDateTime.now())
            .medVedtakResultatType(VedtakResultatType.INNVILGET)
            .medAnsvarligSaksbehandler("Nav Navesen")
            .build();
        behandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(behandling, LocalDate.now().minusYears(1), LocalDate.now(), false);
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        behandling = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(behandling.erUnderIverksettelse()).isTrue();

        // Act
        håndterer.håndterAvsluttetBehandling(behandling, ForretningshendelseType.YTELSE_ENDRET, RE_ENDRING_BEREGNINGSGRUNNLAG);

        // Assert
        behandling = behandlingRepository.hentBehandling(behandling.getId());
        assertThat(behandling.erUnderIverksettelse()).isTrue();

        Optional<Behandling> revurdering = behandlingRepository.hentSisteBehandlingForFagsakId(behandling.getFagsakId());
        assertThat(revurdering.get().erRevurdering()).isTrue();

        List<Historikkinnslag> historikkinnslag = historikkRepository.hentHistorikk(revurdering.get().getId());
        assertThat(historikkinnslag).isNotEmpty();
        List<HistorikkinnslagFelt> historikkinnslagFelter = historikkinnslag.get(0).getHistorikkinnslagDeler().get(0).getHistorikkinnslagFelt();
        assertThat(historikkinnslagFelter.stream().filter(historikkinnslagFelt -> HistorikkinnslagFeltType.BEGRUNNELSE
            .equals(historikkinnslagFelt.getFeltType())).findFirst().get().getTilVerdi()).isEqualTo(RE_ENDRING_BEREGNINGSGRUNNLAG.getKode());
    }
}
