package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioKlageEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.vedtak.impl.KlageAnkeVedtakTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;

@CdiDbAwareTest
class ForeslåVedtakTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    private KlageRepository klageRepository;

    @Inject
    private AnkeRepository ankeRepository;

    @Mock
    private OppgaveTjeneste oppgaveTjeneste;
    @Mock
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;
    private HistorikkRepository historikkRepository;
    @Mock
    private Behandling behandling;

    private ForeslåVedtakTjeneste tjeneste;

    @BeforeEach
    public void setUp(EntityManager em) {
        historikkRepository = spy(repositoryProvider.getHistorikkRepository());
        behandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagre(repositoryProvider);

        lenient().when(oppgaveTjeneste.harÅpneVurderKonsekvensOppgaver(any(AktørId.class))).thenReturn(Boolean.FALSE);
        lenient().when(oppgaveTjeneste.harÅpneVurderDokumentOppgaver(any(AktørId.class))).thenReturn(Boolean.FALSE);
        lenient().when(dokumentBehandlingTjeneste.erDokumentBestilt(anyLong(), any())).thenReturn(true);

        var sjekkMotEksisterendeOppgaverTjeneste = new SjekkMotEksisterendeOppgaverTjeneste(historikkRepository, oppgaveTjeneste);
        var klageAnke = new KlageAnkeVedtakTjeneste(klageRepository, ankeRepository);
        tjeneste = new ForeslåVedtakTjeneste(fagsakRepository, behandlingRepository, behandlingsresultatRepository,
            klageAnke, sjekkMotEksisterendeOppgaverTjeneste, dokumentBehandlingTjeneste, mock(FagsakEgenskapRepository.class));
    }

    @Test
    void oppretterAksjonspunktVedTotrinnskontrollOgSetterStegPåVent() {
        // Arrange
        leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT, true, false);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
    }

    @Test
    void setterTotrinnskontrollPaBehandlingHvisIkkeSattFraFør() {
        // Arrange
        leggTilAksjonspunkt(AksjonspunktDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET, false, false);

        // Act
        tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(behandling.isToTrinnsBehandling()).isTrue();
    }

    @Test
    void foreslåVedtakManueltDersomAksjonspunktLøstAvSaksbehandler() {
        // Arrange
        var ap = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE);
        ap.setEndretAv("saksbehandler");
        AksjonspunktTestSupport.setTilUtført(ap, "begrunnelse");

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).contains(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
    }

    @Test
    void foreslåVedtakManueltDersomAvslag() {
        // Arrange
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT))
            .lagre(repositoryProvider);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).contains(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
    }

    @Test
    void foreslåVedtakManueltDersomOpphørInngangIkkeOppfylt() {
        // Arrange
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT)
            .medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.OPPHØR))
            .lagre(repositoryProvider);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).contains(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
    }

    @Test
    void ingenAksjonspunktDersomOpphørEtterInngangsvilkår() {
        // Arrange
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel()
            .medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.OPPHØR))
            .lagre(repositoryProvider);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    void setterStegTilUtførtUtenAksjonspunktDersomIkkeTotrinnskontroll() {
        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    void setterIkkeTotrinnskontrollPaBehandlingHvisDetIkkeErTotrinnskontroll() {
        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(behandling.isToTrinnsBehandling()).isFalse();
    }

    @Test
    void nullstillerFritekstfeltetDersomIkkeTotrinnskontroll() {
        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        verify(dokumentBehandlingTjeneste, times(1)).nullstillVedtakFritekstHvisFinnes(anyLong());
    }

    @Test
    void nullstillerFritekstfeltetDersomIkkeLengerRelevant() {
        behandling = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD).lagre(repositoryProvider);
        leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT, true, false);
        leggTilAksjonspunkt(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS, true, true);
        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        verify(dokumentBehandlingTjeneste, times(1)).nullstillVedtakFritekstHvisFinnes(anyLong());
    }

    @Test
    void nullstillerIkkeFritekstfeltetDersomTotrinnskontroll() {
        // Arrange
        leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT, true, false);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        verify(dokumentBehandlingTjeneste, times(0)).nullstillVedtakFritekstHvisFinnes(anyLong());
    }

    @Test
    void lagerRiktigAksjonspunkterNårDetErOppgaveriGsak() {
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
    void lagerIkkeNyeAksjonspunkterNårAksjonspunkterAlleredeFinnes() {
        // Arrange
        leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK, false, false);
        lenient().when(oppgaveTjeneste.harÅpneVurderKonsekvensOppgaver(any(AktørId.class))).thenReturn(Boolean.TRUE);
        lenient().when(oppgaveTjeneste.harÅpneVurderDokumentOppgaver(any(AktørId.class))).thenReturn(Boolean.TRUE);


        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        verify(historikkRepository, times(0)).lagre(any());
    }

    @Test
    void utførerUtenAksjonspunktHvisRevurderingIkkeOpprettetManueltOgIkkeTotrinnskontroll() {
        // Arrange
        var behandling = ScenarioMorSøkerEngangsstønad.forFødsel().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    void utførerMedAksjonspunktForeslåVedtakManueltHvisRevurderingOpprettetManueltOgIkkeTotrinnskontroll() {
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
    void utførerUtenAksjonspunktHvisRevurderingIkkeManueltOpprettetOgIkkeTotrinnskontrollBehandling2TrinnIkkeReset() {
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
    void utførerMedAksjonspunktForeslåVedtakManueltHvisRevurderingOpprettetManueltOgIkkeTotrinnskontrollBehandling2TrinnIkkeReset() {
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
    void oppretterAksjonspunktVedTotrinnskontrollForRevurdering() {
        // Arrange
        behandling = ScenarioMorSøkerEngangsstønad.forFødsel().medBehandlingType(BehandlingType.REVURDERING).lagre(repositoryProvider);
        leggTilAksjonspunkt(AksjonspunktDefinisjon.OVERSTYRING_AV_ADOPSJONSVILKÅRET, true, false);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).hasSize(1);
        assertThat(stegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
    }

    @Test
    void skalUtføreUtenAksjonspunkterHvisKlageHarResultatHjemsendt() {
        // Arrange
        AbstractTestScenario<?> scenario = ScenarioMorSøkerEngangsstønad.forFødsel();
        behandling = ScenarioKlageEngangsstønad.forHjemsendtNK(scenario).lagre(repositoryProvider, klageRepository);
        leggTilAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK, true, false);

        // Act
        var stegResultat = tjeneste.foreslåVedtak(behandling);

        // Assert
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
        assertThat(stegResultat.getAksjonspunktListe()).isEmpty();
    }

    private void leggTilAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon, boolean totrinnsbehandling, boolean settTilAvbrutt) {
        var aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
        if (totrinnsbehandling) {
            AksjonspunktTestSupport.setToTrinnsBehandlingKreves(aksjonspunkt);
        }
        if (settTilAvbrutt) {
            AksjonspunktTestSupport.setTilAvbrutt(aksjonspunkt);
        } else {
            AksjonspunktTestSupport.setTilUtført(aksjonspunkt, "");
        }
    }

}
