package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.*;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerEngangsstønad;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.modell.*;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.AndelKilde;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ForeslåVedtakRevurderingStegImplTest {

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
    private BeregningTjeneste beregningTjeneste;
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

        foreslåVedtakRevurderingStegForeldrepenger = new ForeslåVedtakRevurderingStegImpl(foreslåVedtakTjeneste, beregningTjeneste,
                repositoryProvider);
        when(foreslåVedtakTjeneste.foreslåVedtak(eq(revurdering), any())).thenReturn(behandleStegResultat);
        when(behandleStegResultat.getAksjonspunktResultater()).thenReturn(Collections.emptyList());
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_når_samme_beregningsgrunnlag() {
        orginalBehandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                .buildFor(orginalBehandling);
        when(behandlingsresultatRepository.hent(orginalBehandling.getId())).thenReturn(orginalBehandlingsresultat);
        when(beregningTjeneste.hent(orginalBehandling.getId()))
                .thenReturn(Optional.of(buildBeregningsgrunnlag(1000L)));
        when(beregningTjeneste.hent(revurdering.getId()))
                .thenReturn(Optional.of(buildBeregningsgrunnlag(1000L)));
        when(behandlingRepository.hentBehandling(orginalBehandling.getId())).thenReturn(orginalBehandling);

        assertThat(foreslåVedtakRevurderingStegForeldrepenger.utførSteg(kontekstRevurdering).getAksjonspunktListe()).isEmpty();
    }

    @Test
    void skal_opprette_aksjonspunkt_når_revurdering_har_mindre_beregningsgrunnlag() {
        orginalBehandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                .buildFor(orginalBehandling);
        when(behandlingsresultatRepository.hent(orginalBehandling.getId())).thenReturn(orginalBehandlingsresultat);
        when(beregningTjeneste.hent(orginalBehandling.getId()))
                .thenReturn(Optional.of(buildBeregningsgrunnlag(1000L)));
        when(beregningTjeneste.hent(revurdering.getId()))
                .thenReturn(Optional.of(buildBeregningsgrunnlag(900L)));
        when(behandlingRepository.hentBehandling(orginalBehandling.getId())).thenReturn(orginalBehandling);

        foreslåVedtakRevurderingStegForeldrepenger.utførSteg(kontekstRevurdering);
        verify(foreslåVedtakTjeneste).foreslåVedtak(revurdering, List.of(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST));
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_når_original_behandling_har_resultat_avslått() {
        orginalBehandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT)
                .buildFor(orginalBehandling);
        when(behandlingsresultatRepository.hent(orginalBehandling.getId())).thenReturn(orginalBehandlingsresultat);
        when(beregningTjeneste.hent(revurdering.getId()))
                .thenReturn(Optional.of(buildBeregningsgrunnlag(900L)));
        when(behandlingRepository.hentBehandling(orginalBehandling.getId())).thenReturn(orginalBehandling);
        assertThat(foreslåVedtakRevurderingStegForeldrepenger.utførSteg(kontekstRevurdering).getAksjonspunktListe()).isEmpty();
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_når_original_behandling_har_resultat_opphør() {
        orginalBehandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.OPPHØR)
                .buildFor(orginalBehandling);
        when(behandlingsresultatRepository.hent(orginalBehandling.getId())).thenReturn(orginalBehandlingsresultat);
        when(beregningTjeneste.hent(revurdering.getId()))
                .thenReturn(Optional.of(buildBeregningsgrunnlag(900L)));
        when(behandlingRepository.hentBehandling(orginalBehandling.getId())).thenReturn(orginalBehandling);
        assertThat(foreslåVedtakRevurderingStegForeldrepenger.utførSteg(kontekstRevurdering).getAksjonspunktListe()).isEmpty();
    }

    @Test
    void test_tilbakehopp() {
        foreslåVedtakRevurderingStegForeldrepenger.vedHoppOverBakover(kontekstRevurdering, null, null, null);
        revurdering = behandlingRepository.hentBehandling(revurdering.getId());
        assertThat(behandlingsresultat.getKonsekvenserForYtelsen()).isEmpty();
    }

    private BeregningsgrunnlagGrunnlag buildBeregningsgrunnlag(Long bruttoPerÅr) {
        var bgBuilder = Beregningsgrunnlag.builder()
                .medSkjæringstidspunkt(LocalDate.now())
                .medGrunnbeløp(BigDecimal.valueOf(91425));
        var periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))
            .medBruttoPrÅr(BigDecimal.valueOf(bruttoPerÅr))
            .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
                .medKilde(AndelKilde.PROSESS_START)
                .medRedusertRefusjonPrÅr(BigDecimal.valueOf(bruttoPerÅr))
                .medDagsatsArbeidsgiver(BigDecimal.valueOf(bruttoPerÅr).divide(BigDecimal.valueOf(260), 0, RoundingMode.HALF_UP).longValue())
                .build())
            .build();
        var beregningsgrunnlag = bgBuilder.leggTilBeregningsgrunnlagPeriode(periode).build();
        return BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(BeregningsgrunnlagTilstand.FASTSATT);
    }

}
