package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.vedtak.impl.KlageAnkeVedtakTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

@CdiDbAwareTest
public class ForeslåVedtakTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

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
    private HistorikkRepository historikkRepository;
    @Mock
    private Behandling behandling;
    private BehandlingskontrollKontekst kontekst;

    private ForeslåVedtakTjeneste tjeneste;

    @BeforeEach
    public void setUp(EntityManager em) {
        historikkRepository = spy(repositoryProvider.getHistorikkRepository());
        behandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagre(repositoryProvider);
        em.persist(behandling.getBehandlingsresultat());
        kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling);

        lenient().when(oppgaveTjeneste.harÅpneVurderKonsekvensOppgaver(any(AktørId.class))).thenReturn(Boolean.FALSE);
        lenient().when(oppgaveTjeneste.harÅpneVurderDokumentOppgaver(any(AktørId.class))).thenReturn(Boolean.FALSE);
        lenient().when(dokumentBehandlingTjeneste.erDokumentBestilt(anyLong(), any())).thenReturn(true);

        var sjekkMotEksisterendeOppgaverTjeneste = new SjekkMotEksisterendeOppgaverTjeneste(historikkRepository,
                oppgaveTjeneste);
        var klageAnke = new KlageAnkeVedtakTjeneste(klageRepository, ankeRepository);
        tjeneste = new ForeslåVedtakTjeneste(fagsakRepository, klageAnke, sjekkMotEksisterendeOppgaverTjeneste, dokumentBehandlingTjeneste);
    }

    @Test
    public void oppretterAksjonspunktVedTotrinnskontrollOgSetterStegPåVent() {
        // Arrange
        leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT, true);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

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
        tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(behandling.isToTrinnsBehandling()).isTrue();
    }

    @Test
    public void setterPåVentHvisÅpentAksjonspunktVedtakUtenTotrinnskontroll() {
        // Arrange
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void nullstillerBehandlingHvisDetEksistererVedtakUtenTotrinnAksjonspunkt() {
        // Arrange
        AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL);

        // Act
        tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(behandling.isToTrinnsBehandling()).isFalse();
    }

    @Test
    public void setterStegTilUtførtUtenAksjonspunktDersomIkkeTotrinnskontroll() {
        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void setterIkkeTotrinnskontrollPaBehandlingHvisDetIkkeErTotrinnskontroll() {
        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandling.isToTrinnsBehandling()).isFalse();
    }

    @Test
    public void nullstillerFritekstfeltetDersomIkkeTotrinnskontroll() {
        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        verify(dokumentBehandlingTjeneste, times(1)).nullstillVedtakFritekstHvisFinnes(anyLong());
    }

    @Test
    public void nullstillerIkkeFritekstfeltetDersomTotrinnskontroll() {
        // Arrange
        leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT, true);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        verify(dokumentBehandlingTjeneste, times(0)).nullstillVedtakFritekstHvisFinnes(anyLong());
    }

    @Test
    public void lagerRiktigAksjonspunkterNårDetErOppgaveriGsak() {
        // Arrange
        lenient().when(oppgaveTjeneste.harÅpneVurderKonsekvensOppgaver(any(AktørId.class))).thenReturn(Boolean.TRUE);
        lenient().when(oppgaveTjeneste.harÅpneVurderDokumentOppgaver(any(AktørId.class))).thenReturn(Boolean.TRUE);


        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

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
        lenient().when(oppgaveTjeneste.harÅpneVurderKonsekvensOppgaver(any(AktørId.class))).thenReturn(Boolean.TRUE);
        lenient().when(oppgaveTjeneste.harÅpneVurderDokumentOppgaver(any(AktørId.class))).thenReturn(Boolean.TRUE);


        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        verify(historikkRepository, times(0)).lagre(any());
    }

    @Test
    public void utførerUtenAksjonspunktHvisRevurderingIkkeOpprettetManueltOgIkkeTotrinnskontroll() {
        // Arrange
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void utførerMedAksjonspunktForeslåVedtakManueltHvisRevurderingOpprettetManueltOgIkkeTotrinnskontroll() {
        // Arrange
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .lagre(repositoryProvider);
        var revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING).medManueltOpprettet(true))
                .build();
        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(revurdering);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
    }

    @Test
    public void utførerUtenAksjonspunktHvisRevurderingIkkeManueltOpprettetOgIkkeTotrinnskontrollBehandling2TrinnIkkeReset() {
        // Arrange
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);
        behandling.setToTrinnsBehandling();

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void utførerMedAksjonspunktForeslåVedtakManueltHvisRevurderingOpprettetManueltOgIkkeTotrinnskontrollBehandling2TrinnIkkeReset() {
        // Arrange
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
                .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
                .lagre(repositoryProvider);
        var revurdering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING).medManueltOpprettet(true))
                .build();
        revurdering.setToTrinnsBehandling();
        var lås = behandlingRepository.taSkriveLås(revurdering);
        behandlingRepository.lagre(revurdering, lås);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(revurdering);

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
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
    }

    @Test
    public void skalUtføreUtenAksjonspunkterHvisKlageHarResultatHjemsendt() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        behandling = ScenarioKlageEngangsstønad.forHjemsendtNK(scenario).lagre(repositoryProvider, klageRepository);
        leggTilAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK, true);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    private void leggTilAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon, boolean totrinnsbehandling) {
        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
        if (totrinnsbehandling) {
            AksjonspunktTestSupport.setToTrinnsBehandlingKreves(aksjonspunkt);
        }
        AksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
    }

}
