package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
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
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.AktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagPrStatusOgAndel;

@ExtendWith(MockitoExtension.class)
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
        lenient().when(behandlingsresultatRepository.hent(revurdering.getId())).thenReturn(behandlingsresultat);

        kontekstRevurdering = mock(BehandlingskontrollKontekst.class);
        BehandlingLås behandlingLås = mock(BehandlingLås.class);
        lenient().when(kontekstRevurdering.getBehandlingId()).thenReturn(revurdering.getId());
        lenient().when(kontekstRevurdering.getSkriveLås()).thenReturn(behandlingLås);
        lenient().when(behandlingRepository.hentBehandling(kontekstRevurdering.getBehandlingId())).thenReturn(revurdering);

        foreslåVedtakRevurderingStegForeldrepenger =
            new ForeslåVedtakRevurderingStegImpl(foreslåVedtakTjeneste, beregningsgrunnlagTjeneste, repositoryProvider);
        lenient().when(foreslåVedtakTjeneste.foreslåVedtak(revurdering, kontekstRevurdering)).thenReturn(behandleStegResultat);
        lenient().when(behandleStegResultat.getAksjonspunktResultater()).thenReturn(Collections.emptyList());
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_når_samme_beregningsgrunnlag() {
        // Arrange
        orginalBehandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(orginalBehandling);
        when(behandlingsresultatRepository.hent(orginalBehandling.getId())).thenReturn(orginalBehandlingsresultat);
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(orginalBehandling.getId())).thenReturn(Optional.of(buildBeregningsgrunnlag(1000L)));
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(revurdering.getId())).thenReturn(Optional.of(buildBeregningsgrunnlag(1000L)));
        when(behandlingRepository.hentBehandling(orginalBehandling.getId())).thenReturn(orginalBehandling);

        // Act
        BehandleStegResultat behandleStegResultat = foreslåVedtakRevurderingStegForeldrepenger.utførSteg(kontekstRevurdering);

        // Assert
        assertThat(behandleStegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_når_revurdering_har_mindre_beregningsgrunnlag() {
        // Arrange
        orginalBehandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(orginalBehandling);
        when(behandlingsresultatRepository.hent(orginalBehandling.getId())).thenReturn(orginalBehandlingsresultat);
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(orginalBehandling.getId())).thenReturn(Optional.of(buildBeregningsgrunnlag(1000L)));
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(revurdering.getId())).thenReturn(Optional.of(buildBeregningsgrunnlag(900L)));
        when(behandlingRepository.hentBehandling(orginalBehandling.getId())).thenReturn(orginalBehandling);

        // Act
        BehandleStegResultat behandleStegResultat = foreslåVedtakRevurderingStegForeldrepenger.utførSteg(kontekstRevurdering);

        // Assert
        assertThat(behandleStegResultat.getAksjonspunktListe().get(0)).isEqualTo(AksjonspunktDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_når_original_behandling_har_resultat_avslått() {
        // Arrange
        orginalBehandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT).buildFor(orginalBehandling);
        when(behandlingsresultatRepository.hent(orginalBehandling.getId())).thenReturn(orginalBehandlingsresultat);
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(revurdering.getId())).thenReturn(Optional.of(buildBeregningsgrunnlag(900L)));
        when(behandlingRepository.hentBehandling(orginalBehandling.getId())).thenReturn(orginalBehandling);

        // Act
        BehandleStegResultat behandleStegResultat = foreslåVedtakRevurderingStegForeldrepenger.utførSteg(kontekstRevurdering);

        // Assert
        assertThat(behandleStegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_når_original_behandling_har_resultat_opphør() {
        // Arrange
        orginalBehandlingsresultat = Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.OPPHØR).buildFor(orginalBehandling);
        when(behandlingsresultatRepository.hent(orginalBehandling.getId())).thenReturn(orginalBehandlingsresultat);
        when(beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(revurdering.getId())).thenReturn(Optional.of(buildBeregningsgrunnlag(900L)));
        when(behandlingRepository.hentBehandling(orginalBehandling.getId())).thenReturn(orginalBehandling);

        // Act
        BehandleStegResultat behandleStegResultat = foreslåVedtakRevurderingStegForeldrepenger.utførSteg(kontekstRevurdering);

        // Assert
        assertThat(behandleStegResultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    public void test_tilbakehopp() {
        // Arrange

        // Act
        foreslåVedtakRevurderingStegForeldrepenger.vedHoppOverBakover(kontekstRevurdering, null, null, null);

        // Assert
        revurdering = behandlingRepository.hentBehandling(revurdering.getId());
        assertThat(behandlingsresultat.getKonsekvenserForYtelsen()).isEmpty();
    }

    private BeregningsgrunnlagEntitet buildBeregningsgrunnlag(Long bruttoPerÅr) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medSkjæringstidspunkt(LocalDate.now())
            .medGrunnbeløp(BigDecimal.valueOf(91425))
            .build();
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1))
            .medBruttoPrÅr(BigDecimal.valueOf(bruttoPerÅr))
            .leggTilBeregningsgrunnlagPrStatusOgAndel(BeregningsgrunnlagPrStatusOgAndel.builder()
                .medAktivitetStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
                .medRedusertRefusjonPrÅr(BigDecimal.valueOf(bruttoPerÅr)))
            .build(beregningsgrunnlag);
        return beregningsgrunnlag;
    }

}
