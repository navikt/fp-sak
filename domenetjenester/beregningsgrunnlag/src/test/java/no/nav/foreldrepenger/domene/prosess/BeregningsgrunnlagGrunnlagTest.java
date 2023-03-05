package no.nav.foreldrepenger.domene.prosess;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetAggregatDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetOverstyringDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningAktivitetOverstyringerDto;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagDtoBuilder;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningAktivitetHandlingType;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
class BeregningsgrunnlagGrunnlagTest {

    private static final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();
    private static final Intervall PERIODE = Intervall.fraOgMedTilOgMed(LocalDate.of(2019, Month.MARCH, 1), TIDENES_ENDE);

    @Test
    void skal_returnere_register() {
        var beregningAktivitetSN = BeregningAktivitetDto.builder()
            .medPeriode(PERIODE)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING)
            .build();

        var beregningAktivitetAAP = BeregningAktivitetDto.builder()
            .medPeriode(PERIODE)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEIDSAVKLARING)
            .build();

        var registerAktiviteter = BeregningAktivitetAggregatDto.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
            .leggTilAktivitet(beregningAktivitetSN)
            .leggTilAktivitet(beregningAktivitetAAP)
            .build();

        var bgg = BeregningsgrunnlagGrunnlagDtoBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(registerAktiviteter)
            .build(BeregningsgrunnlagTilstand.OPPRETTET);

        // Act
        var resultat = bgg.getGjeldendeAktiviteter();

        // Assert
        assertThat(resultat.getSkjæringstidspunktOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(resultat.getBeregningAktiviteter()).hasSize(2);
        assertThat(resultat.getBeregningAktiviteter().get(0)).isEqualTo(beregningAktivitetSN);
        assertThat(resultat.getBeregningAktiviteter().get(1)).isEqualTo(beregningAktivitetAAP);
    }

    @Test
    void skal_returnere_overstyringer() {
        var beregningAktivitetSN = BeregningAktivitetDto.builder()
            .medPeriode(PERIODE)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING)
            .build();

        var beregningAktivitetAAP = BeregningAktivitetDto.builder()
            .medPeriode(PERIODE)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEIDSAVKLARING)
            .build();

        var registerAktiviteter = BeregningAktivitetAggregatDto.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
            .leggTilAktivitet(beregningAktivitetSN)
            .leggTilAktivitet(beregningAktivitetAAP)
            .build();

        var overstyring = lagOverstyringForBA(beregningAktivitetAAP);
        var overstyringerEntitet = BeregningAktivitetOverstyringerDto.builder()
            .leggTilOverstyring(overstyring)
            .build();
        var bgg = BeregningsgrunnlagGrunnlagDtoBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(registerAktiviteter)
            .medOverstyring(overstyringerEntitet)
            .build(BeregningsgrunnlagTilstand.OPPRETTET);

        // Act
        var resultat = bgg.getGjeldendeAktiviteter();

        // Assert
        assertThat(resultat.getSkjæringstidspunktOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(resultat.getBeregningAktiviteter()).hasSize(1);
        assertThat(resultat.getBeregningAktiviteter().get(0)).isEqualTo(beregningAktivitetSN);
    }

    @Test
    void skal_returnere_overstyringer_når_saksbehandlet_finnes() {
        var beregningAktivitetSN = BeregningAktivitetDto.builder()
            .medPeriode(PERIODE)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.NÆRING)
            .build();

        var beregningAktivitetAAP = BeregningAktivitetDto.builder()
            .medPeriode(PERIODE)
            .medOpptjeningAktivitetType(OpptjeningAktivitetType.ARBEIDSAVKLARING)
            .build();

        var registerAktiviteter = BeregningAktivitetAggregatDto.builder()
            .medSkjæringstidspunktOpptjening(SKJÆRINGSTIDSPUNKT)
            .leggTilAktivitet(beregningAktivitetSN)
            .leggTilAktivitet(beregningAktivitetAAP)
            .build();

        var overstyring = lagOverstyringForBA(beregningAktivitetAAP);
        var overstyringerEntitet = BeregningAktivitetOverstyringerDto.builder()
            .leggTilOverstyring(overstyring)
            .build();
        var bgg = BeregningsgrunnlagGrunnlagDtoBuilder.oppdatere(Optional.empty())
            .medRegisterAktiviteter(registerAktiviteter)
            .medOverstyring(overstyringerEntitet)
            .medSaksbehandletAktiviteter(registerAktiviteter)
            .build(BeregningsgrunnlagTilstand.OPPRETTET);

        // Act
        var resultat = bgg.getGjeldendeAktiviteter();

        // Assert
        assertThat(resultat.getSkjæringstidspunktOpptjening()).isEqualTo(SKJÆRINGSTIDSPUNKT);
        assertThat(resultat.getBeregningAktiviteter()).hasSize(1);
        assertThat(resultat.getBeregningAktiviteter().get(0)).isEqualTo(beregningAktivitetSN);
    }

    private BeregningAktivitetOverstyringDto lagOverstyringForBA(BeregningAktivitetDto beregningAktivitet) {
        return BeregningAktivitetOverstyringDto.builder()
            .medOpptjeningAktivitetType(beregningAktivitet.getOpptjeningAktivitetType())
            .medPeriode(beregningAktivitet.getPeriode())
            .medArbeidsgiver(beregningAktivitet.getArbeidsgiver())
            .medArbeidsforholdRef(beregningAktivitet.getArbeidsforholdRef())
            .medHandling(BeregningAktivitetHandlingType.IKKE_BENYTT)
            .build();
    }
}
