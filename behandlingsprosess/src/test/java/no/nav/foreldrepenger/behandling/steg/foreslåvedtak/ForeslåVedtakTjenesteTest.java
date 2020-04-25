package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class ForeslåVedtakTjenesteTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private KlageRepository klageRepository;

    @Inject
    private AnkeRepository ankeRepository;

    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    @Mock
    private OppgaveTjeneste oppgaveTjeneste;
    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private HistorikkRepository historikkRepository = spy(repositoryProvider.getHistorikkRepository());
    @Mock
    private Behandling behandling;
    private BehandlingskontrollKontekst kontekst;

    private ForeslåVedtakTjeneste tjeneste;

    private AksjonspunktTestSupport aksjonspunktRepository = new AksjonspunktTestSupport();


    @Before
    public void setUp() {
        behandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagre(repositoryProvider);
        entityManager.persist(behandling.getBehandlingsresultat());
        kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);

        when(oppgaveTjeneste.harÅpneOppgaverAvType(any(AktørId.class), any())).thenReturn(Boolean.FALSE);
        when(dokumentBehandlingTjeneste.erDokumentBestilt(anyLong(), any())).thenReturn(true);

        SjekkMotEksisterendeOppgaverTjeneste sjekkMotEksisterendeOppgaverTjeneste = new SjekkMotEksisterendeOppgaverTjeneste(historikkRepository, oppgaveTjeneste);
        tjeneste = new ForeslåVedtakTjeneste(fagsakRepository, ankeRepository, klageRepository, behandlingskontrollTjeneste, sjekkMotEksisterendeOppgaverTjeneste);
    }

    @Test
    public void oppretterAksjonspunktVedTotrinnskontrollOgSetterStegPåVent() {
        // Arrange
        leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT, true);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
    }


    @Test
    public void setterTotrinnskontrollPaBehandlingHvisIkkeSattFraFør() {
        // Arrange
        leggTilAksjonspunkt(AksjonspunktDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET, false);

        // Act
        tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(behandling.isToTrinnsBehandling()).isTrue();
    }


    @Test
    public void setterPåVentHvisÅpentAksjonspunktVedtakUtenTotrinnskontroll() {
        // Arrange
        aksjonspunktRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void nullstillerBehandlingHvisDetEksistererVedtakUtenTotrinnAksjonspunkt() {
        // Arrange
        aksjonspunktRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL);

        // Act
        tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(behandling.isToTrinnsBehandling()).isFalse();
    }

    @Test
    public void setterStegTilUtførtUtenAksjonspunktDersomIkkeTotorinnskontroll() {
        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void setterIkkeTotrinnskontrollPaBehandlingHvisDetIkkeErTotrinnskontroll() {
        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandling.isToTrinnsBehandling()).isFalse();
    }

    @Test
    public void lagerRiktigAksjonspunkterNårDetErOppgaveriGsak() {
        // Arrange
        when(oppgaveTjeneste.harÅpneOppgaverAvType(any(AktørId.class), any())).thenReturn(Boolean.TRUE);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        verify(historikkRepository, times(2)).lagre(any());
        assertThat(stegResultat.getAksjonspunktListe().contains(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK)).isTrue();
        assertThat(stegResultat.getAksjonspunktListe().contains(AksjonspunktDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK)).isTrue();
    }

    @Test
    public void lagerIkkeNyeAksjonspunkterNårAksjonspunkterAlleredeFinnes() {
        // Arrange
        leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK, false);
        when(oppgaveTjeneste.harÅpneOppgaverAvType(any(AktørId.class), any())).thenReturn(Boolean.TRUE);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        verify(historikkRepository, times(0)).lagre(any());
    }

    @Test
    public void utførerUtenAksjonspunktHvisRevurderingIkkeOpprettetManueltOgIkkeTotrinnskontroll() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerEngangsstønad.forFødsel().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void utførerMedAksjonspunktForeslåVedtakManueltHvisRevurderingOpprettetManueltOgIkkeTotrinnskontroll() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagre(repositoryProvider);
        Behandling revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING).medManueltOpprettet(true))
            .build();
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(revurdering, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
    }

    @Test
    public void utførerUtenAksjonspunktHvisRevurderingIkkeManueltOpprettetOgIkkeTotrinnskontrollBehandling2TrinnIkkeReset() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerEngangsstønad.forFødsel().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);
        behandling.setToTrinnsBehandling();

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void utførerMedAksjonspunktForeslåVedtakManueltHvisRevurderingOpprettetManueltOgIkkeTotrinnskontrollBehandling2TrinnIkkeReset() {
        // Arrange
        Behandling behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .lagre(repositoryProvider);
        Behandling revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING).medManueltOpprettet(true))
            .build();
        revurdering.setToTrinnsBehandling();
        BehandlingLås lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(revurdering, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
    }

    @Test
    public void oppretterAksjonspunktVedTotrinnskontrollForRevurdering() {
        // Arrange
        behandling = ScenarioMorSøkerEngangsstønad.forFødsel().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);
        leggTilAksjonspunkt(AksjonspunktDefinisjon.OVERSTYRING_AV_ADOPSJONSVILKÅRET, true);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
    }

    @Test
    public void skalAvbryteForeslåOgFatteVedtakAksjonspunkterNårDeFinnesPåBehandlingUtenTotrinnskontroll() {
        // Arrange
        leggTilAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK, false);
        leggTilAksjonspunkt(AksjonspunktDefinisjon.FATTER_VEDTAK, false);

        // Act
        tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(behandling.isToTrinnsBehandling()).isFalse();
        assertThat(behandling.getAksjonspunkter()).hasSize(2);
        assertThat(behandling.getAksjonspunktFor(AksjonspunktDefinisjon.FORESLÅ_VEDTAK).getStatus()).isEqualTo(AksjonspunktStatus.AVBRUTT);
        assertThat(behandling.getAksjonspunktFor(AksjonspunktDefinisjon.FATTER_VEDTAK).getStatus()).isEqualTo(AksjonspunktStatus.AVBRUTT);
    }

    @Test
    public void skalUtføreUtenAksjonspunkterHvisKlageHarResultatHjemsendt() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        behandling = ScenarioKlageEngangsstønad.forHjemsendtNK(scenario).lagre(repositoryProvider, klageRepository);
        leggTilAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK, true);

        // Act
        BehandleStegResultat stegResultat = tjeneste.foreslåVedtak(behandling, kontekst);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    private void leggTilAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon, boolean totrinnsbehandling) {
        Aksjonspunkt aksjonspunkt = aksjonspunktRepository.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
        Whitebox.setInternalState(aksjonspunkt, "status", AksjonspunktStatus.UTFØRT);
        Whitebox.setInternalState(aksjonspunkt, "toTrinnsBehandling", totrinnsbehandling);
    }

}
