package no.nav.foreldrepenger.domene.migrering;

import static no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus.FRILANSER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagRegelType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.kodeverk.Hjemmel;

@ExtendWith(MockitoExtension.class)
class RegelsporingMigreringTjenesteTest {

    @Mock
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;

    private RegelsporingMigreringTjeneste regelsporingMigreringTjeneste;
    private Behandling behandling;

    @BeforeEach
    void setup() {
        behandling = ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked();
        regelsporingMigreringTjeneste = new RegelsporingMigreringTjeneste(beregningsgrunnlagRepository);
    }

    @Test
    void skal_returnere_grunnlag_om_dette_er_fra_før_feilen_oppstod() {
        var stp = LocalDate.now();
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.valueOf(100_000))
            .medSkjæringstidspunkt(stp)
            .medRegelSporing("input", "evaluering", BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT, "1.0.0")
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(FRILANSER).medHjemmel(Hjemmel.F_14_7_8_38))
            .build();
        var periode1 = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(stp, stp.plusMonths(2).minusDays(1))
            .medBruttoPrÅr(BigDecimal.valueOf(30_000))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medAktivitetStatus(FRILANSER)
            .medBeregnetPrÅr(BigDecimal.valueOf(30_000))
            .medFastsattAvSaksbehandler(Boolean.TRUE)
            .medMottarYtelse(Boolean.TRUE, FRILANSER)
            .build(periode1);
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(1L, BeregningsgrunnlagTilstand.OPPRETTET);

        var sporinger = regelsporingMigreringTjeneste.finnRegelsporingGrunnlag(grunnlag,
            BehandlingReferanse.fra(behandling));

        assertThat(sporinger).containsKey(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT);
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT).getRegelInput()).isEqualTo("input");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT).getRegelEvaluering()).isEqualTo("evaluering");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT).getRegelVersjon()).isEqualTo("1.0.0");
    }


    @Test
    void skal_returnere_grunnlag_om_dette_inneholder_alle_sporinger() {
        var stp = LocalDate.now();
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.valueOf(100_000))
            .medSkjæringstidspunkt(stp)
            .medRegelSporing("input", "evaluering", BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT, "1.0.0")
            .medRegelSporing("input", "evaluering", BeregningsgrunnlagRegelType.BRUKERS_STATUS, "1.0.0")
            .medRegelSporing("input", "evaluering", BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE, "1.0.0")
            .medRegelSporing("input", "evaluering", BeregningsgrunnlagRegelType.PERIODISERING_GRADERING, "1.0.0")
            .medRegelSporing("input", "evaluering", BeregningsgrunnlagRegelType.PERIODISERING_REFUSJON, "1.0.0")
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(FRILANSER).medHjemmel(Hjemmel.F_14_7_8_38))
            .build();
        var periode1 = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(stp, stp.plusMonths(2).minusDays(1))
            .medBruttoPrÅr(BigDecimal.valueOf(30_000))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medAktivitetStatus(FRILANSER)
            .medBeregnetPrÅr(BigDecimal.valueOf(30_000))
            .medFastsattAvSaksbehandler(Boolean.TRUE)
            .medMottarYtelse(Boolean.TRUE, FRILANSER)
            .build(periode1);
        var grunnlag = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(1L, BeregningsgrunnlagTilstand.FASTSATT);

        var sporinger = regelsporingMigreringTjeneste.finnRegelsporingGrunnlag(grunnlag,
            BehandlingReferanse.fra(behandling));

        assertThat(sporinger).containsKey(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT);
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT).getRegelInput()).isEqualTo("input");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT).getRegelEvaluering()).isEqualTo("evaluering");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT).getRegelVersjon()).isEqualTo("1.0.0");

        assertThat(sporinger).containsKey(BeregningsgrunnlagRegelType.BRUKERS_STATUS);
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.BRUKERS_STATUS).getRegelInput()).isEqualTo("input");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.BRUKERS_STATUS).getRegelEvaluering()).isEqualTo("evaluering");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.BRUKERS_STATUS).getRegelVersjon()).isEqualTo("1.0.0");

        assertThat(sporinger).containsKey(BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE);
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE).getRegelInput()).isEqualTo("input");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE).getRegelEvaluering()).isEqualTo("evaluering");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE).getRegelVersjon()).isEqualTo("1.0.0");

        assertThat(sporinger).containsKey(BeregningsgrunnlagRegelType.PERIODISERING_REFUSJON);
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_REFUSJON).getRegelInput()).isEqualTo("input");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_REFUSJON).getRegelEvaluering()).isEqualTo("evaluering");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_REFUSJON).getRegelVersjon()).isEqualTo("1.0.0");

        assertThat(sporinger).containsKey(BeregningsgrunnlagRegelType.PERIODISERING_GRADERING);
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_GRADERING).getRegelInput()).isEqualTo("input");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_GRADERING).getRegelEvaluering()).isEqualTo("evaluering");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_GRADERING).getRegelVersjon()).isEqualTo("1.0.0");
    }

    @Test
    void skal_finne_gamle_grunnlag_når_sporing_mangler() {
        var stp = LocalDate.now();
        var beregningsgrunnlag = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.valueOf(100_000))
            .medSkjæringstidspunkt(stp)
            .medRegelSporing("forsvunnetInput", "forsvunnetEvaluering", BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT, "1.0.0")
            .medRegelSporing("forsvunnetInput", "forsvunnetEvaluering", BeregningsgrunnlagRegelType.BRUKERS_STATUS, "1.0.0")
            .medRegelSporing("forsvunnetInput", "forsvunnetEvaluering", BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE, "1.0.0")
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(FRILANSER).medHjemmel(Hjemmel.F_14_7_8_38))
            .build();
        var periode1 = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(stp, stp.plusMonths(2).minusDays(1))
            .medBruttoPrÅr(BigDecimal.valueOf(30_000))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medAktivitetStatus(FRILANSER)
            .medBeregnetPrÅr(BigDecimal.valueOf(30_000))
            .medFastsattAvSaksbehandler(Boolean.TRUE)
            .medMottarYtelse(Boolean.TRUE, FRILANSER)
            .build(periode1);
        var gammelGrunnlagMedSporinger = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag).build(1L, BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);

        var beregningsgrunnlag2 = BeregningsgrunnlagEntitet.ny()
            .medGrunnbeløp(BigDecimal.valueOf(100_000))
            .medSkjæringstidspunkt(stp)
            .medRegelSporing("input", "evaluering", BeregningsgrunnlagRegelType.PERIODISERING_REFUSJON, "1.0.0")
            .medRegelSporing("input", "evaluering", BeregningsgrunnlagRegelType.PERIODISERING_GRADERING, "1.0.0")
            .leggTilAktivitetStatus(BeregningsgrunnlagAktivitetStatus.builder().medAktivitetStatus(FRILANSER).medHjemmel(Hjemmel.F_14_7_8_38))
            .build();
        var periode2 = BeregningsgrunnlagPeriode.ny()
            .medBeregningsgrunnlagPeriode(stp, stp.plusMonths(2).minusDays(1))
            .medBruttoPrÅr(BigDecimal.valueOf(30_000))
            .build(beregningsgrunnlag2);
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAndelsnr(1L)
            .medAktivitetStatus(FRILANSER)
            .medBeregnetPrÅr(BigDecimal.valueOf(30_000))
            .medFastsattAvSaksbehandler(Boolean.TRUE)
            .medMottarYtelse(Boolean.TRUE, FRILANSER)
            .build(periode2);
        var nyttGrunnlagUtenSporinger = BeregningsgrunnlagGrunnlagBuilder.nytt().medBeregningsgrunnlag(beregningsgrunnlag2).build(1L, BeregningsgrunnlagTilstand.KOFAKBER_UT);

        when(beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(any(), any())).thenReturn(Optional.of(gammelGrunnlagMedSporinger));

        var sporinger = regelsporingMigreringTjeneste.finnRegelsporingGrunnlag(nyttGrunnlagUtenSporinger,
            BehandlingReferanse.fra(behandling));

        assertThat(sporinger).containsKey(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT);
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT).getRegelInput()).isEqualTo("forsvunnetInput");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT).getRegelEvaluering()).isEqualTo("forsvunnetEvaluering");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.SKJÆRINGSTIDSPUNKT).getRegelVersjon()).isEqualTo("1.0.0");

        assertThat(sporinger).containsKey(BeregningsgrunnlagRegelType.BRUKERS_STATUS);
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.BRUKERS_STATUS).getRegelInput()).isEqualTo("forsvunnetInput");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.BRUKERS_STATUS).getRegelEvaluering()).isEqualTo("forsvunnetEvaluering");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.BRUKERS_STATUS).getRegelVersjon()).isEqualTo("1.0.0");

        assertThat(sporinger).containsKey(BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE);
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE).getRegelInput()).isEqualTo("forsvunnetInput");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE).getRegelEvaluering()).isEqualTo("forsvunnetEvaluering");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_NATURALYTELSE).getRegelVersjon()).isEqualTo("1.0.0");

        assertThat(sporinger).containsKey(BeregningsgrunnlagRegelType.PERIODISERING_REFUSJON);
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_REFUSJON).getRegelInput()).isEqualTo("input");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_REFUSJON).getRegelEvaluering()).isEqualTo("evaluering");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_REFUSJON).getRegelVersjon()).isEqualTo("1.0.0");

        assertThat(sporinger).containsKey(BeregningsgrunnlagRegelType.PERIODISERING_GRADERING);
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_GRADERING).getRegelInput()).isEqualTo("input");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_GRADERING).getRegelEvaluering()).isEqualTo("evaluering");
        assertThat(sporinger.get(BeregningsgrunnlagRegelType.PERIODISERING_GRADERING).getRegelVersjon()).isEqualTo("1.0.0");
    }
}
