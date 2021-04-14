package no.nav.foreldrepenger.ytelse.beregning.regler.feriepenger;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import no.nav.foreldrepenger.ytelse.beregning.regelmodell.BeregningsresultatPeriode;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr;
import no.nav.fpsak.tidsserie.LocalDateInterval;

class BeregnFeriepengerForPeriode {
    private static final BigDecimal FERIEPENGER_SATS_PROSENT = BigDecimal.valueOf(0.102);

    private BeregnFeriepengerForPeriode() {
    }

    static void beregn(Map<String, Object> resultater, List<BeregningsresultatPeriode> beregningsresultatPerioder, LocalDateInterval feriepengerPeriode) {
        beregningsresultatPerioder.stream()
            .filter(periode -> periode.getPeriode().overlaps(feriepengerPeriode))
            .forEach(periode -> {
                var overlapp = periode.getPeriode().overlap(feriepengerPeriode).get();//NOSONAR
                var antallFeriepengerDager = beregnAntallUkedagerMellom(overlapp.getFomDato(), overlapp.getTomDato());
                var opptjeningÅr = overlapp.getFomDato().withMonth(12).withDayOfMonth(31);

                //Regelsporing
                var periodeNavn = "perioden " + overlapp;
                resultater.put("Antall feriepengedager i " + periodeNavn, antallFeriepengerDager);
                resultater.put("Opptjeningsår i " + periodeNavn, opptjeningÅr);

                periode.getBeregningsresultatAndelList().stream()
                    .filter(andel -> andel.getInntektskategori().erArbeidstakerEllerSjømann())
                    .forEach(andel -> {
                        var feriepengerGrunnlag = andel.getDagsats() * antallFeriepengerDager;
                        var feriepengerAndelPrÅr = BigDecimal.valueOf(feriepengerGrunnlag).multiply(FERIEPENGER_SATS_PROSENT);
                        if (feriepengerAndelPrÅr.compareTo(BigDecimal.ZERO) == 0) {
                            return;
                        }
                        BeregningsresultatFeriepengerPrÅr.builder()
                            .medOpptjeningÅr(opptjeningÅr)
                            .medÅrsbeløp(feriepengerAndelPrÅr)
                            .build(andel);

                        //Regelsporing
                        var mottaker = andel.erBrukerMottaker() ? "Bruker." : "Arbeidsgiver.";
                        var andelId = andel.getArbeidsforhold() != null ? andel.getArbeidsgiverId() : andel.getAktivitetStatus().name();
                        resultater.put("Feriepenger." + mottaker + andelId + " i " + periodeNavn, feriepengerAndelPrÅr);
                    });
            });
    }

    private static int beregnAntallUkedagerMellom(LocalDate fom, LocalDate tom) {
        var antallUkedager = 0;
        for (var d = fom; !d.isAfter(tom); d = d.plusDays(1)) {
            var dag = d.getDayOfWeek().getValue();
            if (dag <= DayOfWeek.FRIDAY.getValue()) {
                antallUkedager++;
            }
        }
        return antallUkedager;
    }
}
