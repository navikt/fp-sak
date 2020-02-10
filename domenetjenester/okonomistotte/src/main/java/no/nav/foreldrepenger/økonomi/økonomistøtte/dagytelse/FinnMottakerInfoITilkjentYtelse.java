package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelseAndel;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelsePeriode;

public class FinnMottakerInfoITilkjentYtelse {

    private FinnMottakerInfoITilkjentYtelse() {
    }

    static boolean erBrukerMottakerITilkjentYtelse(OppdragInput behandlingInfo) {
        TilkjentYtelse tilkjentYtelse = behandlingInfo.getTilkjentYtelse()
            .orElseThrow(() -> new IllegalStateException("Mangler beregningsresultat for behandling " + behandlingInfo.getBehandlingId()));
        return finnesUtbetalingTilBruker(tilkjentYtelse.getTilkjentYtelsePerioder());
    }

    public static boolean erBrukerMottakerIForrigeTilkjentYtelse(OppdragInput behandlingInfo) {
        return finnesUtbetalingTilBruker(behandlingInfo.getForrigeTilkjentYtelsePerioder());
    }

    private static boolean finnesUtbetalingTilBruker(List<TilkjentYtelsePeriode> tilkjentYtelsePerioder) {
        return tilkjentYtelsePerioder
            .stream()
            .anyMatch(FinnMottakerInfoITilkjentYtelse::finnesAndelForBruker);
    }

    static List<String> finnOrgnrForArbeidsgivereITilkjentYtelse(OppdragInput behandlingInfo) {

        TilkjentYtelse tilkjentYtelseFP = behandlingInfo.getTilkjentYtelse()
            .orElseThrow(() -> new IllegalStateException("Mangler beregningsresultat for behandling " + behandlingInfo.getBehandlingId()));

        return tilkjentYtelseFP.getTilkjentYtelsePerioder()
            .stream()
            .flatMap(oppdragPeriode -> oppdragPeriode.getTilkjentYtelseAndeler().stream())
            .filter(oppdragAndel -> !oppdragAndel.skalTilBrukerEllerPrivatperson())
            .filter(oppdragAndel -> oppdragAndel.getDagsats() > 0)
            .map(TilkjentYtelseAndel::getArbeidsforholdOrgnr)
            .distinct()
            .collect(Collectors.toList());
    }

    public static LocalDate finnSisteDagMedUtbetalingTilMottaker(OppdragInput behandlingInfo, Oppdragsmottaker mottaker) {
        List<TilkjentYtelsePeriode> tilkjentYtelsePeriodeListe = behandlingInfo.getTilkjentYtelse()
            .map(TilkjentYtelse::getTilkjentYtelsePerioder)
            .orElse(Collections.emptyList());
        return tilkjentYtelsePeriodeListe.stream()
            .flatMap(oppdragPeriode -> oppdragPeriode.getTilkjentYtelseAndeler().stream())
            .filter(andel -> !andel.skalTilBrukerEllerPrivatperson())
            .filter(andel -> andel.getArbeidsforholdOrgnr().equals(mottaker.getId()))
            .filter(andel -> andel.getDagsats() > 0)
            .map(andel -> andel.getTilkjentYtelsePeriode().getTom())
            .max(Comparator.comparing(Function.identity()))
            .orElseThrow(() -> new IllegalStateException("Det finnes ikke en uttaksdato"));
    }

    public static LocalDate førsteUttaksdatoBrukersForrigeBehandling(OppdragInput behandlingInfo) {
        return behandlingInfo.getForrigeTilkjentYtelsePerioder()
            .stream()
            .filter(FinnMottakerInfoITilkjentYtelse::finnesAndelForBruker)
            .map(TilkjentYtelsePeriode::getFom)
            .min(Comparator.comparing(Function.identity()))
            .orElseThrow(() -> new IllegalArgumentException("Fant ingen andel for bruker i forrige oppdrag"));
    }

    private static boolean finnesAndelForBruker(TilkjentYtelsePeriode periode) {
        return periode.getTilkjentYtelseAndeler()
            .stream()
            .anyMatch(andel -> andel.skalTilBrukerEllerPrivatperson() && andel.getDagsats() > 0);
    }
}
