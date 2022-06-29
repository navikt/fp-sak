package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ForeslåVedtakRevurderingStegImplTest {

    @Mock
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;
    @Mock
    private BehandleStegResultat behandleStegResultat;
    @Mock
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    @Mock
    private Behandlingsresultat behandlingsresultat;
    @Mock
    private Behandlingsresultat orginalBehandlingsresultat;

    private final BehandlingRepositoryProvider repositoryProvider = mock(BehandlingRepositoryProvider.class);
    private ForeslåVedtakRevurderingStegImpl foreslåVedtakRevurderingStegForeldrepenger;

    private Behandling orginalBehandling;
    private Behandling revurdering;
    private BehandlingskontrollKontekst kontekstRevurdering;

    @BeforeEach
    public void before() {
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getBehandlingRevurderingRepository()).thenReturn(behandlingRevurderingRepository);
        when(repositoryProvider.getBehandlingsresultatRepository()).thenReturn(behandlingsresultatRepository);

        orginalBehandling = ScenarioMorSøkerEngangsstønad.forFødsel().lagMocked();
        orginalBehandling.avsluttBehandling();
        revurdering = ScenarioMorSøkerForeldrepenger.forFødsel()
                .medBehandlingType(BehandlingType.REVURDERING)
                .medOriginalBehandling(orginalBehandling, BehandlingÅrsakType.BERØRT_BEHANDLING)
                .lagMocked();

        behandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT).buildFor(revurdering);
        when(behandlingsresultatRepository.hent(revurdering.getId())).thenReturn(behandlingsresultat);

        kontekstRevurdering = mock(BehandlingskontrollKontekst.class);
        var behandlingLås = mock(BehandlingLås.class);
        when(kontekstRevurdering.getBehandlingId()).thenReturn(revurdering.getId());
        when(kontekstRevurdering.getSkriveLås()).thenReturn(behandlingLås);
        when(behandlingRepository.hentBehandling(kontekstRevurdering.getBehandlingId())).thenReturn(revurdering);

        foreslåVedtakRevurderingStegForeldrepenger = new ForeslåVedtakRevurderingStegImpl(foreslåVedtakTjeneste, beregningsgrunnlagTjeneste,
                repositoryProvider);
        when(foreslåVedtakTjeneste.foreslåVedtak(revurdering)).thenReturn(behandleStegResultat);
        when(behandleStegResultat.getAksjonspunktResultater()).thenReturn(Collections.emptyList());
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_når_samme_beregningsgrunnlag() {
        orginalBehandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                .buildFor(orginalBehandling);
        when(behandlingsresultatRepository.hent(orginalBehandling.getId())).thenReturn(orginalBehandlingsresultat);
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(orginalBehandling.getId()))
                .thenReturn(Optional.of(buildBeregningsgrunnlag(1000L)));
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(revurdering.getId()))
                .thenReturn(Optional.of(buildBeregningsgrunnlag(1000L)));
        when(behandlingRepository.hentBehandling(orginalBehandling.getId())).thenReturn(orginalBehandling);

        assertThat(foreslåVedtakRevurderingStegForeldrepenger.utførSteg(kontekstRevurdering).getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_når_revurdering_har_mindre_beregningsgrunnlag() {
        orginalBehandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                .buildFor(orginalBehandling);
        when(behandlingsresultatRepository.hent(orginalBehandling.getId())).thenReturn(orginalBehandlingsresultat);
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(orginalBehandling.getId()))
                .thenReturn(Optional.of(buildBeregningsgrunnlag(1000L)));
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(revurdering.getId()))
                .thenReturn(Optional.of(buildBeregningsgrunnlag(900L)));
        when(behandlingRepository.hentBehandling(orginalBehandling.getId())).thenReturn(orginalBehandling);

        assertThat(foreslåVedtakRevurderingStegForeldrepenger.utførSteg(kontekstRevurdering).getAksjonspunktListe().get(0))
                .isEqualTo(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_når_original_behandling_har_resultat_avslått() {
        orginalBehandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT)
                .buildFor(orginalBehandling);
        when(behandlingsresultatRepository.hent(orginalBehandling.getId())).thenReturn(orginalBehandlingsresultat);
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(revurdering.getId()))
                .thenReturn(Optional.of(buildBeregningsgrunnlag(900L)));
        when(behandlingRepository.hentBehandling(orginalBehandling.getId())).thenReturn(orginalBehandling);
        assertThat(foreslåVedtakRevurderingStegForeldrepenger.utførSteg(kontekstRevurdering).getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_når_original_behandling_har_resultat_opphør() {
        orginalBehandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.OPPHØR)
                .buildFor(orginalBehandling);
        when(behandlingsresultatRepository.hent(orginalBehandling.getId())).thenReturn(orginalBehandlingsresultat);
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(revurdering.getId()))
                .thenReturn(Optional.of(buildBeregningsgrunnlag(900L)));
        when(behandlingRepository.hentBehandling(orginalBehandling.getId())).thenReturn(orginalBehandling);
        assertThat(foreslåVedtakRevurderingStegForeldrepenger.utførSteg(kontekstRevurdering).getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void test_tilbakehopp() {
        foreslåVedtakRevurderingStegForeldrepenger.vedHoppOverBakover(kontekstRevurdering, null, null, null);
        revurdering = behandlingRepository.hentBehandling(revurdering.getId());
        assertThat(behandlingsresultat.getKonsekvenserForYtelsen()).isEmpty();
    }

    private BeregningsgrunnlagEntitet buildBeregningsgrunnlag(Long bruttoPerÅr) {
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
                .medSkjæringstidspunkt(LocalDate.now())
                .medGrunnbeløp(BigDecimal.valueOf(91425))
                .build();
        BeregningsgrunnlagPeriode.ny()
                .medBeregningsgrunnlagPeriode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))
                .medBruttoPrÅr(BigDecimal.valueOf(bruttoPerÅr))
                .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                        .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
                        .medRedusertRefusjonPrÅr(BigDecimal.valueOf(bruttoPerÅr)))
                .build(beregningsgrunnlag);
        return beregningsgrunnlag;
    }

}
