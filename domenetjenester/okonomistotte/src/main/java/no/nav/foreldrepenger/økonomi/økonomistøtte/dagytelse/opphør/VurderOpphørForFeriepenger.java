package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.økonomi.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.TidligereOppdragTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdragslinje150.Oppdragslinje150Util;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjenteFeriepenger;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjenteFeriepengerPrÅr;

class VurderOpphørForFeriepenger {

    private VurderOpphørForFeriepenger() {
    }

    static boolean vurder(OppdragInput oppdragInput, Oppdrag110 forrigeOppdrag110, Optional<Oppdragsmottaker> mottakerOpt) {
        Optional<TilkjenteFeriepenger> oppdragFeriepengerOpt = oppdragInput.getTilkjentYtelse()
            .flatMap(TilkjentYtelse::getTilkjenteFeriepenger);
        boolean erBrukerMottaker = !mottakerOpt.isPresent();
        List<LocalDate> gjeldendeFeriepengerÅrFraFør = finnGjeldendeFeriepengerÅrFraFør(oppdragInput, forrigeOppdrag110);

        if (oppdragFeriepengerOpt.isPresent()) {
            TilkjenteFeriepenger tilkjenteFeriepenger = oppdragFeriepengerOpt.get();
            for (LocalDate feriepengeår : gjeldendeFeriepengerÅrFraFør) {
                int opptjeningsår = feriepengeår.getYear() - 1;
                boolean finnesFeriepengeårIRevurdering = sjekkOmDetFinnesFeriepengeårITilkjentYtelse(mottakerOpt, tilkjenteFeriepenger, opptjeningsår, erBrukerMottaker);
                if (finnesFeriepengeårIRevurdering) {
                    continue;
                }
                return true;
            }
            return false;
        }
        return !gjeldendeFeriepengerÅrFraFør.isEmpty();
    }

    private static boolean sjekkOmDetFinnesFeriepengeårITilkjentYtelse(Optional<Oppdragsmottaker> mottakerOpt, TilkjenteFeriepenger tilkjenteFeriepenger,
                                                                       int opptjeningsår, boolean erBrukerMottaker) {
        if (erBrukerMottaker) {
            return tilkjenteFeriepenger.getTilkjenteFeriepengerPrÅrList().stream()
                .filter(TilkjenteFeriepengerPrÅr::skalTilBrukerEllerPrivatperson)
                .anyMatch(feriepengerPrÅr -> feriepengerPrÅr.getOpptjeningÅr().getYear() == opptjeningsår);
        } else {
            Oppdragsmottaker mottakerArbeidsgiver = mottakerOpt
                .orElseThrow(() -> new IllegalStateException("Utvikler feil: Fant ikke arbeidsgiver som mottaker i feriepenger vurdering"));
            return tilkjenteFeriepenger.getTilkjenteFeriepengerPrÅrList().stream()
                .filter(feriepengerPrÅr -> !feriepengerPrÅr.skalTilBrukerEllerPrivatperson()
                    && feriepengerPrÅr.getArbeidsforholdOrgnr().equals(mottakerArbeidsgiver.getOrgnr()))
                .anyMatch(oppdragFeriepengerPrÅr -> oppdragFeriepengerPrÅr.getOpptjeningÅr().getYear() == opptjeningsår);
        }
    }

    private static List<LocalDate> finnGjeldendeFeriepengerÅrFraFør(OppdragInput oppdragInput, Oppdrag110 forrigeOppdrag110) {
        List<Oppdragslinje150> tidligereOpp150List = TidligereOppdragTjeneste.hentAlleTidligereOppdragslinje150(oppdragInput,
            forrigeOppdrag110);
        List<Oppdragslinje150> oppdr150FeriepengerListe = Oppdragslinje150Util.getOpp150ForFeriepengerMedKlassekode(tidligereOpp150List);

        Map<LocalDate, List<Oppdragslinje150>> opp150PerFom = oppdr150FeriepengerListe.stream()
            .sorted(Comparator.comparing(Oppdragslinje150::getDelytelseId)
                .thenComparing(Oppdragslinje150::getKodeStatusLinje, Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed())
            .collect(Collectors.groupingBy(Oppdragslinje150::getDatoVedtakFom, TreeMap::new, Collectors.toList()));

        List<LocalDate> gjeldendeOppdragslinjer = new ArrayList<>();

        if (!opp150PerFom.isEmpty()) {
            opp150PerFom.forEach((key, value) -> {
                for (var oppdragslinje150 : value) {
                    if (!oppdragslinje150.gjelderOpphør()) {
                        gjeldendeOppdragslinjer.add(oppdragslinje150.getDatoVedtakFom());
                    } else {
                        break;
                    }
                }
            });

            return gjeldendeOppdragslinjer.stream().distinct().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
