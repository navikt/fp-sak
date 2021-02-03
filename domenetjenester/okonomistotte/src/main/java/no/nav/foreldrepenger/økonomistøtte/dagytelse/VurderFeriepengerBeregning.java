package no.nav.foreldrepenger.økonomistøtte.dagytelse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150.Oppdragslinje150FeriepengerUtil;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150.Oppdragslinje150Util;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjentYtelse;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjenteFeriepenger;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjenteFeriepengerPrÅr;

public class VurderFeriepengerBeregning {

    private VurderFeriepengerBeregning() {
    }

    public static List<Oppdragslinje150> finnOppdr150MedEndringIFeriepengerBeregning(List<Oppdragslinje150> tidligereOpp150FeriepengerListe,
                                                                                     List<TilkjenteFeriepengerPrÅr> tilkjenteFeriepengerPrÅrList) {
        List<Oppdragslinje150> opp150FeriepengerListe = new ArrayList<>();
        if (tidligereOpp150FeriepengerListe.isEmpty()) {
            return opp150FeriepengerListe;
        }
        Set<LocalDate> opptjeningsårListe = Oppdragslinje150FeriepengerUtil.getOpptjeningsdato(tilkjenteFeriepengerPrÅrList);
        for (LocalDate opptjeningsår : opptjeningsårListe) {
            int feriepengeår = opptjeningsår.getYear() + 1;
            long sumFraRevurderingBeh = beregnFeriepengerMedGittOpptjeningsår(tilkjenteFeriepengerPrÅrList, opptjeningsår);
            List<Oppdragslinje150> opp150MedMaxDelytelseIdListe = Oppdragslinje150FeriepengerUtil.finnOpp150FeriepengerMedMaxDelytelseIdForGittÅret(tidligereOpp150FeriepengerListe, feriepengeår);
            boolean erSisteOpp150Opphør = finnesOpphørIOppdragslinje150Liste(opp150MedMaxDelytelseIdListe);
            if (erSisteOpp150Opphør) {
                opp150MedMaxDelytelseIdListe.stream()
                    .filter(Oppdragslinje150::gjelderOpphør).findFirst()
                    .ifPresent(opp150FeriepengerListe::add);
                continue;
            }
            opp150MedMaxDelytelseIdListe.forEach(opp150 -> {
                if (opp150.getSats() != sumFraRevurderingBeh) {
                    opp150FeriepengerListe.add(opp150);
                }
            });
        }
        return opp150FeriepengerListe;
    }

    public static boolean erFeriepengerEndret(OppdragInput behandlingInfo, List<Oppdragslinje150> tidligereOppdr150Liste,
                                              Oppdragsmottaker mottaker) {
        List<Oppdragslinje150> tidligereOpp150FeriepengerListe = TidligereOppdragTjeneste.hentAlleTidligereOppdr150ForFeriepenger(behandlingInfo,
            tidligereOppdr150Liste);
        Optional<TilkjenteFeriepenger> oppdragFeriepengerOpt = behandlingInfo.getTilkjentYtelse()
            .flatMap(TilkjentYtelse::getTilkjenteFeriepenger);
        if (oppdragFeriepengerOpt.isPresent()) {
            if (!tidligereOpp150FeriepengerListe.isEmpty()) {
                List<TilkjenteFeriepengerPrÅr> tilkjenteFeriepengerPrÅrList = Oppdragslinje150FeriepengerUtil.opprettOpp150FeriepengerListe(mottaker, oppdragFeriepengerOpt.get());
                List<Oppdragslinje150> opp150FeriepengerListe = VurderFeriepengerBeregning.finnOppdr150MedEndringIFeriepengerBeregning(tidligereOpp150FeriepengerListe, tilkjenteFeriepengerPrÅrList);
                boolean erDetNyFeriepengeår = Oppdragslinje150FeriepengerUtil.finnesNyFeriepengeårIBehandling(tidligereOpp150FeriepengerListe, tilkjenteFeriepengerPrÅrList);
                return !opp150FeriepengerListe.isEmpty() || erDetNyFeriepengeår;
            } else {
                List<TilkjenteFeriepengerPrÅr> tilkjenteFeriepengerPrÅrRevurdListe = Oppdragslinje150FeriepengerUtil.opprettOpp150FeriepengerListe(mottaker, oppdragFeriepengerOpt.get());
                return !tilkjenteFeriepengerPrÅrRevurdListe.isEmpty();
            }
        }
        return false;
    }

    private static boolean finnesOpphørIOppdragslinje150Liste(List<Oppdragslinje150> opp150MedMaxDelytelseIdListe) {
        return opp150MedMaxDelytelseIdListe.stream()
            .anyMatch(Oppdragslinje150::gjelderOpphør);
    }

    public static boolean erFeriepengerBeregningNullForGittÅret(OppdragInput behandlingInfo, Oppdragslinje150 opp150Feriepenger,
                                                                boolean erBrukerMottaker) {
        Optional<TilkjenteFeriepenger> oppdragFeriepengerOpt = behandlingInfo.getTilkjentYtelse()
            .flatMap(TilkjentYtelse::getTilkjenteFeriepenger);
        if (oppdragFeriepengerOpt.isPresent()) {
            TilkjenteFeriepenger tilkjenteFeriepenger = oppdragFeriepengerOpt.get();
            LocalDate opptjeningsdato = opp150Feriepenger.getDatoVedtakFom().minusYears(1);
            List<TilkjenteFeriepengerPrÅr> tilkjenteFeriepengerPrÅrListe = getOppdragFeriepengerPrÅrForGittÅret(tilkjenteFeriepenger, opptjeningsdato);
            if (erBrukerMottaker) {
                return sjekkOmFeriepengerFinnesForBruker(tilkjenteFeriepengerPrÅrListe);
            }
            String orgnr = opp150Feriepenger.getRefusjonsinfo156().getRefunderesId();
            return sjekkOmFeriepengerFinnesForArbeidsgiveren(tilkjenteFeriepengerPrÅrListe, orgnr);
        }
        return true;
    }

    private static boolean sjekkOmFeriepengerFinnesForArbeidsgiveren(List<TilkjenteFeriepengerPrÅr> tilkjenteFeriepengerPrÅrList, String orgnr) {
        return tilkjenteFeriepengerPrÅrList.stream()
            .noneMatch(feriepengerPrÅr -> !feriepengerPrÅr.skalTilBrukerEllerPrivatperson()
                && orgnr.equals(Oppdragslinje150Util.endreTilElleveSiffer(feriepengerPrÅr.getArbeidsforholdOrgnr())));
    }

    private static boolean sjekkOmFeriepengerFinnesForBruker(List<TilkjenteFeriepengerPrÅr> tilkjenteFeriepengerPrÅrListe) {
        return tilkjenteFeriepengerPrÅrListe.stream()
            .noneMatch(TilkjenteFeriepengerPrÅr::skalTilBrukerEllerPrivatperson);
    }

    private static List<TilkjenteFeriepengerPrÅr> getOppdragFeriepengerPrÅrForGittÅret(TilkjenteFeriepenger tilkjenteFeriepenger, LocalDate opptjeningsdato) {
        return tilkjenteFeriepenger.getTilkjenteFeriepengerPrÅrList()
            .stream()
            .filter(feriepengerPrÅr -> feriepengerPrÅr.getOpptjeningÅr().getYear() == opptjeningsdato.getYear())
            .collect(Collectors.toList());
    }

    public static boolean skalOppdragslinje150ForGittÅretOpprettes(List<Oppdragslinje150> tidligereOpp150FeriepengerListe,
                                                            List<Oppdragslinje150> endretOpp150FeriepengerListe, int opptjeningsår) {
        int feriepengeår = opptjeningsår + 1;
        if (tidligereOpp150FeriepengerListe.isEmpty()) {
            return true;
        }
        boolean erBeregningForGittÅretEndret = endretOpp150FeriepengerListe.stream()
            .anyMatch(oppdr150 -> oppdr150.getDatoVedtakFom().getYear() == feriepengeår);
        boolean erBeregningForGittÅretNytt = tidligereOpp150FeriepengerListe.stream()
            .noneMatch(oppdr150 -> oppdr150.getDatoVedtakFom().getYear() == feriepengeår);

        return erBeregningForGittÅretEndret || erBeregningForGittÅretNytt;
    }

    public static long beregnFeriepengerMedGittOpptjeningsår(List<TilkjenteFeriepengerPrÅr> tilkjenteFeriepengerPrÅrList, LocalDate opptjeningsDato) {
        return tilkjenteFeriepengerPrÅrList.stream()
            .filter(feriepengerPrÅr -> feriepengerPrÅr.getOpptjeningÅr().equals(opptjeningsDato))
            .mapToLong(b -> b.getÅrsbeløp().getVerdi().longValue())
            .sum();
    }
}
