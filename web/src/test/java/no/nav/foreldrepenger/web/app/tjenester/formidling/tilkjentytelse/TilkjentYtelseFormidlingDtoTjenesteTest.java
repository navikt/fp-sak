package no.nav.foreldrepenger.web.app.tjenester.formidling.tilkjentytelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.kontrakter.fpsak.tilkjentytelse.TilkjentYtelseDagytelseDto;

class TilkjentYtelseFormidlingDtoTjenesteTest {

    @Test
    void skal_teste_mapping_av_tilkjent_ytelse_dagytelse() {
        // Arrange
        var res = BeregningsresultatEntitet.builder().medRegelInput("").medRegelSporing("").build();
        var periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 1))
            .build(res);
        var andel = BeregningsresultatAndel.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medArbeidsgiver(Arbeidsgiver.virksomhet("999999999"))
            .medDagsats(560)
            .medDagsatsFraBg(560)
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medBrukerErMottaker(true)
            .medArbeidsforholdRef(InternArbeidsforholdRef.nyRef())
            .medStillingsprosent(BigDecimal.valueOf(90))
            .build(periode);

        // Act
        var mappetResultat = TilkjentYtelseFormidlingDtoTjeneste.mapDagytelse(res);

        // Assert
        assertThat(mappetResultat).isNotNull();
        assertThat(mappetResultat.perioder()).hasSize(1);

        var mappetPeriode = mappetResultat.perioder().get(0);
        assertThat(mappetPeriode.fom()).isEqualTo(periode.getBeregningsresultatPeriodeFom());
        assertThat(mappetPeriode.tom()).isEqualTo(periode.getBeregningsresultatPeriodeTom());
        assertThat(mappetPeriode.dagsats()).isEqualTo(periode.getDagsats());
        assertThat(mappetPeriode.andeler()).hasSameSizeAs(periode.getBeregningsresultatAndelList());

        var mappetAndel = mappetPeriode.andeler().get(0);
        assertThat(mappetAndel.aktivitetstatus()).isEqualTo(TilkjentYtelseDagytelseDto.Aktivitetstatus.ARBEIDSTAKER);
        assertThat(mappetAndel.tilSoker()).isEqualTo(andel.getDagsats());
        assertThat(mappetAndel.refusjon()).isZero();
        assertThat(mappetAndel.stillingsprosent()).isEqualByComparingTo(andel.getStillingsprosent());
        assertThat(mappetAndel.arbeidsgiverReferanse()).isEqualTo(andel.getArbeidsgiver().get().getIdentifikator());
        assertThat(mappetAndel.arbeidsforholdId()).isEqualTo(andel.getArbeidsforholdRef().getReferanse());
    }

    @Test
    void skal_teste_engangsstønad_mapping() {
        // Arrange
        var resultat = new EngangsstønadBeregning(1L, 93000, 1, 93000, LocalDateTime.now());

        // Act
        var mappetResultat = TilkjentYtelseFormidlingDtoTjeneste.mapEngangsstønad(resultat);

        // Assert
        assertThat(mappetResultat).isNotNull();
        assertThat(mappetResultat.beregnetTilkjentYtelse()).isEqualTo(resultat.getBeregnetTilkjentYtelse());
    }
}
