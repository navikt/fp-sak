package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.FagsakRelasjonEventPubliserer;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDel;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class VurderDekningsgradOppdatererTest {

    private static final String BEGRUNNELSE = "begrunnelse";

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider.getFagsakRelasjonRepository(), FagsakRelasjonEventPubliserer.NULL_EVENT_PUB);


    @Inject
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    private VurderDekningsgradOppdaterer vurderDekningsgradOppdaterer;
    private HistorikkRepository historikkRepository;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private Behandling behandling;
    private Optional<Aksjonspunkt> aksjonspunkt;

    @Before
    public void oppsett(){
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        this.behandling = scenario.lagre(repositoryProvider);
        this.vurderDekningsgradOppdaterer = new VurderDekningsgradOppdaterer(historikkTjenesteAdapter, repositoryProvider,fagsakRelasjonTjeneste);
        this.historikkRepository = repositoryProvider.getHistorikkRepository();
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        AksjonspunktTestSupport aksjonspunktRepository = new AksjonspunktTestSupport();
        BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
        Aksjonspunkt ap = aksjonspunktRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_DEKNINGSGRAD);
        aksjonspunktRepository.setTilUtført(ap, BEGRUNNELSE);

        this.aksjonspunkt = Optional.of(ap);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    @Test
    public void skal_ikke_lage_historikkinnslag_hvis_ingen_endring(){
        // Arrange
        Dekningsgrad lagretDekningsgrad = Dekningsgrad._80;
        int dekningsgradFraDto = 80;
        fagsakRelasjonRepository.opprettRelasjon(behandling.getFagsak(), lagretDekningsgrad);
        VurderDekningsgradDto dto = new VurderDekningsgradDto(BEGRUNNELSE, dekningsgradFraDto);
        // Act
        vurderDekningsgradOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        // Assert
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.FAKTA_ENDRET);
        List<Historikkinnslag> historikk = historikkRepository.hentHistorikk(behandling.getId());
        assertThat(historikk).isEmpty();
    }

    @Test
    public void skal_lage_historikkinnslag_hvis_endring_i_begrunnelse(){
        // Arrange
        Dekningsgrad lagretDekningsgrad = Dekningsgrad._80;
        int dekningsgradFraDto = 80;
        fagsakRelasjonRepository.opprettRelasjon(behandling.getFagsak(), lagretDekningsgrad);
        VurderDekningsgradDto dto = new VurderDekningsgradDto("en endret begrunnelse", dekningsgradFraDto);
        // Act
        vurderDekningsgradOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        // Assert
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.FAKTA_ENDRET);
        List<Historikkinnslag> historikk = historikkRepository.hentHistorikk(behandling.getId());
        assertThat(historikk).hasSize(1);
        List<HistorikkinnslagDel> deler = historikk.get(0).getHistorikkinnslagDeler();
        assertThat(deler).hasSize(1);
        assertThat(deler.get(0).getBegrunnelse()).hasValueSatisfying(begrunnelse -> assertThat(begrunnelse).isEqualTo("en endret begrunnelse"));
        assertThat(deler.get(0).getSkjermlenke()).hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.BEREGNING_FORELDREPENGER.getKode()));
        assertThat(deler.get(0).getEndretFelt(HistorikkEndretFeltType.DEKNINGSGRAD)).hasValueSatisfying(felt ->
            assertThat(felt.getFraVerdi()).isEqualTo("80%"));
        assertThat(deler.get(0).getEndretFelt(HistorikkEndretFeltType.DEKNINGSGRAD)).hasValueSatisfying(felt ->
            assertThat(felt.getTilVerdi()).isEqualTo("80%"));
    }

    @Test
    public void skal_lage_historikkinnslag_hvis_endring_i_dekningsgrad(){
        // Arrange
        Dekningsgrad lagretDekningsgrad = Dekningsgrad._80;
        int dekningsgradFraDto = 100;
        fagsakRelasjonRepository.opprettRelasjon(behandling.getFagsak(), lagretDekningsgrad);
        VurderDekningsgradDto dto = new VurderDekningsgradDto(BEGRUNNELSE, dekningsgradFraDto);
        // Act
        vurderDekningsgradOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        // Assert
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.FAKTA_ENDRET);
        List<Historikkinnslag> historikk = historikkRepository.hentHistorikk(behandling.getId());
        assertThat(historikk).hasSize(1);
        List<HistorikkinnslagDel> deler = historikk.get(0).getHistorikkinnslagDeler();
        assertThat(deler).hasSize(1);
        assertThat(deler.get(0).getBegrunnelse()).hasValueSatisfying(begrunnelse -> assertThat(begrunnelse).isEqualTo(BEGRUNNELSE));
        assertThat(deler.get(0).getSkjermlenke()).hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.BEREGNING_FORELDREPENGER.getKode()));
        assertThat(deler.get(0).getEndretFelt(HistorikkEndretFeltType.DEKNINGSGRAD)).hasValueSatisfying(felt ->
            assertThat(felt.getFraVerdi()).isEqualTo("80%"));
        assertThat(deler.get(0).getEndretFelt(HistorikkEndretFeltType.DEKNINGSGRAD)).hasValueSatisfying(felt ->
            assertThat(felt.getTilVerdi()).isEqualTo("100%"));
    }

    @Test
    public void skal_lage_historikkinnslag_hvis_endring_i_dekningsgrad_og_begrunnelse(){
        // Arrange
        Dekningsgrad lagretDekningsgrad = Dekningsgrad._80;
        int dekningsgradFraDto = 100;
        fagsakRelasjonRepository.opprettRelasjon(behandling.getFagsak(), lagretDekningsgrad);
        VurderDekningsgradDto dto = new VurderDekningsgradDto("en endret begrunnelse", dekningsgradFraDto);
        // Act
        vurderDekningsgradOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        // Assert
        historikkTjenesteAdapter.opprettHistorikkInnslag(behandling.getId(), HistorikkinnslagType.FAKTA_ENDRET);
        List<Historikkinnslag> historikk = historikkRepository.hentHistorikk(behandling.getId());
        assertThat(historikk).hasSize(1);
        List<HistorikkinnslagDel> deler = historikk.get(0).getHistorikkinnslagDeler();
        assertThat(deler).hasSize(1);
        assertThat(deler.get(0).getBegrunnelse()).hasValueSatisfying(begrunnelse -> assertThat(begrunnelse).isEqualTo("en endret begrunnelse"));
        assertThat(deler.get(0).getSkjermlenke()).hasValueSatisfying(skjermlenke -> assertThat(skjermlenke).isEqualTo(SkjermlenkeType.BEREGNING_FORELDREPENGER.getKode()));
        assertThat(deler.get(0).getEndretFelt(HistorikkEndretFeltType.DEKNINGSGRAD)).hasValueSatisfying(felt ->
            assertThat(felt.getFraVerdi()).isEqualTo("80%"));
        assertThat(deler.get(0).getEndretFelt(HistorikkEndretFeltType.DEKNINGSGRAD)).hasValueSatisfying(felt ->
            assertThat(felt.getTilVerdi()).isEqualTo("100%"));
    }

    @Test
    public void skal_lagre_hvis_dekningsgrad_er_endret(){
        // Arrange
        Dekningsgrad lagretDekningsgrad = Dekningsgrad._80;
        int dekningsgradFraDto = 100;
        fagsakRelasjonRepository.opprettRelasjon(behandling.getFagsak(), lagretDekningsgrad);
        VurderDekningsgradDto dto = new VurderDekningsgradDto("en endret begrunnelse", dekningsgradFraDto);
        // Act
        vurderDekningsgradOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        // Assert
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        assertThat(fagsakRelasjonRepository.finnRelasjonFor(behandling.getFagsak()).getGjeldendeDekningsgrad().getVerdi()).isEqualTo(100);
        assertThat(fagsakRelasjonRepository.finnRelasjonFor(behandling.getFagsak()).getDekningsgrad().getVerdi()).isNotEqualTo(100);
        assertThat(behandlingsresultat).hasValueSatisfying( r -> assertThat(r.isEndretDekningsgrad()).isTrue());
    }

    @Test
    public void skal_ikke_endre_lagret_verdier_hvis_dekningsgrad_ikke_er_endret(){
        // Arrange
        Dekningsgrad lagretDekningsgrad = Dekningsgrad._100;
        int dekningsgradFraDto = 100;
        fagsakRelasjonRepository.opprettRelasjon(behandling.getFagsak(), lagretDekningsgrad);
        VurderDekningsgradDto dto = new VurderDekningsgradDto("en endret begrunnelse", dekningsgradFraDto);
        // Act
        vurderDekningsgradOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));
        // Assert
        Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        assertThat(fagsakRelasjonRepository.finnRelasjonFor(behandling.getFagsak()).getGjeldendeDekningsgrad().getVerdi()).isEqualTo(100);
        assertThat(fagsakRelasjonRepository.finnRelasjonFor(behandling.getFagsak()).getDekningsgrad().getVerdi()).isEqualTo(100);
        assertThat(behandlingsresultat).hasValueSatisfying( r -> assertThat(r.isEndretDekningsgrad()).isFalse());
    }

}
