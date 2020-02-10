package no.nav.foreldrepenger.økonomi.tilkjentytelse;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatFeriepengerPrÅr;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
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
        List<TilkjentYtelseAndelV1> andeler = periode.getBeregningsresultatAndelList()
            .stream()
            .map(MapperForTilkjentYtelse::mapAndel)
            .collect(Collectors.toList());
        return new TilkjentYtelsePeriodeV1(periode.getBeregningsresultatPeriodeFom(), periode.getBeregningsresultatPeriodeTom(), andeler);
    }

    private static TilkjentYtelseAndelV1 mapAndel(BeregningsresultatAndel andel) {
        TilkjentYtelseAndelV1 resultat = mapAndelUtenFeriepenger(andel);
        resultat.setUtbetalingsgrad(andel.getUtbetalingsgrad());
        for (BeregningsresultatFeriepengerPrÅr feriepengerPrÅr : andel.getBeregningsresultatFeriepengerPrÅrListe()) {
            Year år = Year.of(feriepengerPrÅr.getOpptjeningsår().getYear());
            long beløp = feriepengerPrÅr.getÅrsbeløp().getVerdi().longValue();
            resultat.leggTilFeriepenger(år, beløp);
        }
        return resultat;
    }

    private static TilkjentYtelseAndelV1 mapAndelUtenFeriepenger(BeregningsresultatAndel andel) {
        TilkjentYtelseV1.Inntektskategori inntektskategori = MapperForInntektskategori.mapInntektskategori(andel.getInntektskategori());
        int dagsats = andel.getDagsats();
        TilkjentYtelseV1.SatsType satsType = TilkjentYtelseV1.SatsType.DAGSATS;


        TilkjentYtelseAndelV1 andelV1 = andel.erBrukerMottaker()
            ? TilkjentYtelseAndelV1.tilBruker(inntektskategori, dagsats, satsType)
            : TilkjentYtelseAndelV1.refusjon(inntektskategori, dagsats, satsType);

        Optional<Arbeidsgiver> arbeidsgiverOpt = andel.getArbeidsgiver();
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
