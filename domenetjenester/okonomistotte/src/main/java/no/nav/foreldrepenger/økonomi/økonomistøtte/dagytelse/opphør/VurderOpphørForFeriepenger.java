package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør;

import java.time.LocalDate;
import java.util.Collections;
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

    static boolean vurder(OppdragInput behandlingInfo, Oppdrag110 forrigeOppdrag110, Optional<Oppdragsmottaker> mottakerOpt) {
        Optional<TilkjenteFeriepenger> oppdragFeriepengerOpt = behandlingInfo.getTilkjentYtelse()
            .flatMap(TilkjentYtelse::getTilkjenteFeriepenger);
        boolean erBrukerMottaker = !mottakerOpt.isPresent();
        List<Oppdragslinje150> gjeldendeFeriepengerFraFør = finnGjeldendeFeriepengerFraFør(behandlingInfo, forrigeOppdrag110);
        boolean erDetFeriepengerFraFør = !gjeldendeFeriepengerFraFør.isEmpty();
        if (oppdragFeriepengerOpt.isPresent()) {
            TilkjenteFeriepenger tilkjenteFeriepenger = oppdragFeriepengerOpt.get();
            List<LocalDate> feriepengeårListe = gjeldendeFeriepengerFraFør.stream()
                .map(Oppdragslinje150::getDatoVedtakFom)
                .collect(Collectors.toList());
            for (LocalDate feriepengeår : feriepengeårListe) {
                int opptjeningsår = feriepengeår.getYear() - 1;
                boolean finnesFeriepengeårIRevurdering = sjekkOmDetFinnesFeriepengeårIRevurdering(mottakerOpt, tilkjenteFeriepenger, opptjeningsår, erBrukerMottaker);
                if (finnesFeriepengeårIRevurdering) {
                    continue;
                }
                return true;
            }
            return false;
        }
        return erDetFeriepengerFraFør;
    }

    private static boolean sjekkOmDetFinnesFeriepengeårIRevurdering(Optional<Oppdragsmottaker> mottakerOpt, TilkjenteFeriepenger tilkjenteFeriepenger,
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

    private static List<Oppdragslinje150> finnGjeldendeFeriepengerFraFør(OppdragInput behandlingInfo, Oppdrag110 forrigeOppdrag110) {
        List<Oppdragslinje150> tidligereOpp150List = TidligereOppdragTjeneste.hentAlleTidligereOppdragslinje150(behandlingInfo,
            forrigeOppdrag110);
        List<Oppdragslinje150> oppdr150FeriepengerListe = Oppdragslinje150Util.getOpp150ForFeriepengerMedKlassekode(tidligereOpp150List);
        Map<Long, List<Oppdragslinje150>> opp150PerDelytelseId = oppdr150FeriepengerListe.stream()
            .collect(Collectors.groupingBy(Oppdragslinje150::getDelytelseId, TreeMap::new, Collectors.toList()));
        if (!opp150PerDelytelseId.isEmpty()) {
            opp150PerDelytelseId.entrySet().removeIf(entry -> entry.getValue().stream().anyMatch(Oppdragslinje150::gjelderOpphør));
            return opp150PerDelytelseId.values().stream().flatMap(List::stream).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
