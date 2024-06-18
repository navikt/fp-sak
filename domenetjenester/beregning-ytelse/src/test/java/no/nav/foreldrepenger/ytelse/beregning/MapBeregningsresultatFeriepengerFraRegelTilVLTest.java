package no.nav.foreldrepenger.ytelse.beregning;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatFeriepengerResultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.FastsattFeriepengeresultat;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.AktivitetStatus;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.beregningsgrunnlag.Arbeidsforhold;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class MapBeregningsresultatFeriepengerFraRegelTilVLTest {

    private static final LocalDate STP = LocalDate.now();
    public static final String ORGNR = KUNSTIG_ORG;
    private static final Arbeidsforhold ARBEIDSFORHOLD = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(ORGNR, null);

    @Test
    void skal_ikkje_lage_feriepengeresultat_om_årsbeløp_avrundes_til_0() {
        // Arrange
        var feriepengerPrÅrListe = List.of(lagPeriodeMedAndel(BigDecimal.valueOf(0.1)));
        var regelresultat = new BeregningsresultatFeriepengerResultat(feriepengerPrÅrListe, new LocalDateInterval(STP, STP.plusMonths(10)));
        var resultat = new FastsattFeriepengeresultat(regelresultat, null, "input", "sporing", null);

        // Act
        var feriepenger = MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(resultat);

        // Assert
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe()).isEmpty();
    }

    @Test
    void skal_lage_feriepengeresultat_om_årsbeløp_ikkje_avrundes_til_0() {
        // Arrange
        var feriepengerPrÅrListe = List.of(lagPeriodeMedAndel(BigDecimal.valueOf(1.5)));
        var regelresultat = new BeregningsresultatFeriepengerResultat(feriepengerPrÅrListe, new LocalDateInterval(STP, STP.plusMonths(10)));
        var resultat = new FastsattFeriepengeresultat(regelresultat, null, "input", "sporing", null);

        // Act
        var feriepenger = MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(resultat);


        // Assert
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(1);
        assertBeregningsresultatFeriepenger(feriepenger, STP, BigDecimal.valueOf(1.5).setScale(0, RoundingMode.HALF_UP), ORGNR, true,
            no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.ARBEIDSTAKER);
    }

    private BeregningsresultatFeriepengerPrÅr lagPeriodeMedAndel(BigDecimal årsbeløp) {
        return lagAndel(AktivitetStatus.ATFL, ARBEIDSFORHOLD, true, årsbeløp, STP);
    }

    @Test
    void skal_lage_feriepengeresultat_med_flere_perioder() {
        // Arrange
        var baselinedato = LocalDate.of(2023, Month.DECEMBER, 1);
        var baselinedatoP6W = baselinedato.plusWeeks(6);
        List<BeregningsresultatFeriepengerPrÅr> feriepengerPrÅrListe = new ArrayList<>();
        feriepengerPrÅrListe.addAll(lagPeriodeMedMangeAndeler(baselinedato));
        feriepengerPrÅrListe.addAll(lagPeriodeMedMangeAndeler(baselinedatoP6W));

        var regelresultat = new BeregningsresultatFeriepengerResultat(feriepengerPrÅrListe,
            new LocalDateInterval(baselinedato, baselinedato.plusWeeks(8).minusDays(1)));
        var resultat = new FastsattFeriepengeresultat(regelresultat, null, "input", "sporing", null);

        // Act
        var feriepenger = MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(resultat);


        // Assert
        assertThat(feriepenger.getBeregningsresultatFeriepengerPrÅrListe()).hasSize(8);
        assertBeregningsresultatFeriepenger(feriepenger, baselinedato, BigDecimal.valueOf(25), null, true,
            no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertBeregningsresultatFeriepenger(feriepenger, baselinedato, BigDecimal.valueOf(2), null, true,
            no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.FRILANSER);
        assertBeregningsresultatFeriepenger(feriepenger, baselinedato, BigDecimal.ONE, ORGNR, true,
            no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.ARBEIDSTAKER);
        assertBeregningsresultatFeriepenger(feriepenger, baselinedato, BigDecimal.TEN, ORGNR, false,
            no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.ARBEIDSTAKER);
        assertBeregningsresultatFeriepenger(feriepenger, baselinedatoP6W, BigDecimal.valueOf(25), null, true,
            no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
        assertBeregningsresultatFeriepenger(feriepenger, baselinedatoP6W, BigDecimal.valueOf(2), null, true,
            no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.FRILANSER);
        assertBeregningsresultatFeriepenger(feriepenger, baselinedatoP6W, BigDecimal.ONE, ORGNR, true,
            no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.ARBEIDSTAKER);
        assertBeregningsresultatFeriepenger(feriepenger, baselinedatoP6W, BigDecimal.TEN, ORGNR, false,
            no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus.ARBEIDSTAKER);
    }

    private void assertBeregningsresultatFeriepenger(BeregningsresultatFeriepenger feriepenger, LocalDate år, BigDecimal beløp,
                                                     String arbeidsgiver, boolean brukerErMottaker,
                                                     no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus aktivitetStatus) {
        var ferieandeler = feriepenger.getBeregningsresultatFeriepengerPrÅrListe().stream()
            .filter(f -> f.getOpptjeningsår().isEqual(år.with(MonthDay.of(Month.DECEMBER, 31))) && f.erBrukerMottaker() == brukerErMottaker)
            .filter(f -> aktivitetStatus.equals(f.getAktivitetStatus()))
            .filter(f -> arbeidsgiver == null || f.getArbeidsgiver().filter(a -> a.getIdentifikator().equals(arbeidsgiver)).isPresent())
            .toList();
        assertThat(ferieandeler).satisfiesOnlyOnce(a -> assertThat(a.getÅrsbeløp().getVerdi().longValue()).isEqualTo(beløp.longValue()));
    }


    private List<BeregningsresultatFeriepengerPrÅr> lagPeriodeMedMangeAndeler(LocalDate opptjeningsår) {
        List<BeregningsresultatFeriepengerPrÅr> feriepengerPrÅrListe = new ArrayList<>();
        feriepengerPrÅrListe.add(lagAndel(AktivitetStatus.ATFL, ARBEIDSFORHOLD, true, BigDecimal.ONE, opptjeningsår));
        feriepengerPrÅrListe.add(lagAndel(AktivitetStatus.ATFL, ARBEIDSFORHOLD, false, BigDecimal.TEN, opptjeningsår));
        feriepengerPrÅrListe.add(lagAndel(AktivitetStatus.ATFL, Arbeidsforhold.frilansArbeidsforhold(), true, BigDecimal.valueOf(2), opptjeningsår));
        feriepengerPrÅrListe.add(lagAndel(AktivitetStatus.SN, null, true, BigDecimal.valueOf(25L), opptjeningsår));
        return feriepengerPrÅrListe;
    }

    private BeregningsresultatFeriepengerPrÅr lagAndel(AktivitetStatus aktivitetStatus, Arbeidsforhold arbeidsforhold, boolean brukerErMottaker,
                                                       BigDecimal årsbeløp, LocalDate opptjeningsår) {
        return BeregningsresultatFeriepengerPrÅr.builder().medÅrsbeløp(årsbeløp)
            .medOpptjeningÅr(opptjeningsår.with(MonthDay.of(Month.DECEMBER, 31)))
            .medBrukerErMottaker(brukerErMottaker)
            .medArbeidsforhold(arbeidsforhold)
            .medAktivitetStatus(aktivitetStatus)
            .build();
    }
}
