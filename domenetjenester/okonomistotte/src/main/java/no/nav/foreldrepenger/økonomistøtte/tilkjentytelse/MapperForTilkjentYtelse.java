package no.nav.foreldrepenger.økonomistøtte.tilkjentytelse;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseAndelV1;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelsePeriodeV1;
import no.nav.foreldrepenger.kontrakter.tilkjentytelse.v1.TilkjentYtelseV1;

class MapperForTilkjentYtelse {

    private MapperForTilkjentYtelse() {
        //hindrer instansiering, som gjør sonarqube glad
    }

    static List<TilkjentYtelsePeriodeV1> mapTilkjentYtelse(BeregningsresultatEntitet beregningsresultat) {
        return beregningsresultat.getBeregningsresultatPerioder()
            .stream()
            .map(MapperForTilkjentYtelse::mapPeriode)
            .collect(Collectors.toList());
    }

    private static TilkjentYtelsePeriodeV1 mapPeriode(BeregningsresultatPeriode periode) {
        var andeler = periode.getBeregningsresultatAndelList()
            .stream()
            .map(MapperForTilkjentYtelse::mapAndel)
            .collect(Collectors.toList());
        return new TilkjentYtelsePeriodeV1(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom(), andeler);
    }

    private static TilkjentYtelseAndelV1 mapAndel(BeregningsresultatAndel andel) {
        var resultat = mapAndelUtenFeriepenger(andel);
        resultat.setUtbetalingsgrad(andel.getUtbetalingsgrad());
        for (var feriepengerPrÅr : andel.getBeregningsresultatFeriepengerPrÅrListe()) {
            var år = Year.of(feriepengerPrÅr.getOpptjeningsår().getYear());
            var beløp = feriepengerPrÅr.getÅrsbeløp().getVerdi().longValue();
            resultat.leggTilFeriepenger(år, beløp);
        }
        return resultat;
    }

    private static TilkjentYtelseAndelV1 mapAndelUtenFeriepenger(BeregningsresultatAndel andel) {
        var inntektskategori = MapperForInntektskategori.mapInntektskategori(andel.getInntektskategori());
        var dagsats = andel.getDagsats();
        var satsType = TilkjentYtelseV1.SatsType.DAGSATS;


        var andelV1 = andel.erBrukerMottaker()
            ? TilkjentYtelseAndelV1.tilBruker(inntektskategori, dagsats, satsType)
            : TilkjentYtelseAndelV1.refusjon(inntektskategori, dagsats, satsType);

        var arbeidsgiverOpt = andel.getArbeidsgiver();
        if (arbeidsgiverOpt.isPresent()) {
            if (andel.erArbeidsgiverPrivatperson()) {
                andelV1.medArbeidsgiverAktørId(arbeidsgiverOpt.get().getIdentifikator());
            } else {
                andelV1.medArbeidsgiverOrgNr(arbeidsgiverOpt.get().getIdentifikator());
            }
        }
        return andelV1;
    }


}
