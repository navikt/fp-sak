package no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.FinnMottakerInfoITilkjentYtelse;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.KlassekodeUtleder;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.TidligereOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.VurderFeriepengerBeregning;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjentYtelse;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjenteFeriepenger;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.TilkjenteFeriepengerPrÅr;

class OpprettOppdragslinje150FeriepengerTjeneste {

    private OpprettOppdragslinje150FeriepengerTjeneste() {
        // skjul default constructor
    }

    static List<Oppdragslinje150> lagOppdragslinje150ForFeriepenger(OppdragInput behandlingInfo,
                                                                    Oppdrag110 oppdrag110, Oppdragsmottaker mottaker,
                                                                    List<Oppdragslinje150> tidligereOppdr150Liste,
                                                                    long sisteSattDelYtelseId) {
        List<Oppdragslinje150> tidligereOpp150FeriepengerListe = TidligereOppdragTjeneste.hentAlleTidligereOppdr150ForFeriepenger(behandlingInfo,
            tidligereOppdr150Liste);
        return opprettOppdragslinje150ForFeriepenger(behandlingInfo, tidligereOpp150FeriepengerListe, oppdrag110, mottaker, sisteSattDelYtelseId);
    }

    private static List<Oppdragslinje150> opprettOppdragslinje150ForFeriepenger(OppdragInput behandlingInfo, List<Oppdragslinje150> tidligereOpp150FeriepengerListe,
                                                                                Oppdrag110 oppdrag110, Oppdragsmottaker mottaker, long sisteSattDelYtelseId) {
        List<Oppdragslinje150> opp150FeriepengerList = new ArrayList<>();
        Optional<TilkjenteFeriepenger> oppdragFeriepengerOpt = behandlingInfo.getTilkjentYtelse()
            .flatMap(TilkjentYtelse::getTilkjenteFeriepenger);
        if (oppdragFeriepengerOpt.isPresent()) {
            TilkjenteFeriepenger tilkjenteFeriepenger = oppdragFeriepengerOpt.get();
            List<TilkjenteFeriepengerPrÅr> feriepengerPrÅrForMottakeren = Oppdragslinje150FeriepengerUtil.opprettOpp150FeriepengerListe(mottaker, tilkjenteFeriepenger);
            Set<LocalDate> opptjeningsDatoList = Oppdragslinje150FeriepengerUtil.getOpptjeningsdato(feriepengerPrÅrForMottakeren);

            List<Oppdragslinje150> endretOpp150FeriepengerForMottakeren = VurderFeriepengerBeregning.finnOppdr150MedEndringIFeriepengerBeregning(tidligereOpp150FeriepengerListe, feriepengerPrÅrForMottakeren);
            long nesteDelytelseId = sisteSattDelYtelseId + 1;
            for (LocalDate opptjeningsDato : opptjeningsDatoList) {
                int opptjeningsår = opptjeningsDato.getYear();
                if (VurderFeriepengerBeregning.skalOppdragslinje150ForGittÅretOpprettes(tidligereOpp150FeriepengerListe, endretOpp150FeriepengerForMottakeren, opptjeningsår)) {
                    Oppdragslinje150.Builder oppdragslinje150Builder = opprettOppdr150FeriepengerBuilder(behandlingInfo, oppdrag110, mottaker, feriepengerPrÅrForMottakeren, opptjeningsDato);
                    oppdragslinje150Builder.medDelytelseId(nesteDelytelseId);
                    if (!tidligereOpp150FeriepengerListe.isEmpty()) {
                        settRefIdFelterHvisTidligereFeriepengerFinnes(tidligereOpp150FeriepengerListe, endretOpp150FeriepengerForMottakeren, oppdragslinje150Builder, opptjeningsår);
                    }
                    nesteDelytelseId++;
                    opp150FeriepengerList.add(oppdragslinje150Builder.build());
                }
            }
            return opp150FeriepengerList;
        }
        return opp150FeriepengerList;
    }

    private static Oppdragslinje150.Builder opprettOppdr150FeriepengerBuilder(OppdragInput behandlingInfo, Oppdrag110 oppdrag110, Oppdragsmottaker mottaker,
                                                                              List<TilkjenteFeriepengerPrÅr> opdragFeriepengerPrÅrList, LocalDate opptjeningsDato) {
        int opptjeningsår = opptjeningsDato.getYear();
        LocalDate vedtakFom = LocalDate.of(opptjeningsår + 1, 5, 1);
        LocalDate vedtakTom = LocalDate.of(opptjeningsår + 1, 5, 31);
        String kodeKlassifik = KlassekodeUtleder.utledForFeriepenger(mottaker, behandlingInfo.getFamilieYtelseType());
        long sats = VurderFeriepengerBeregning.beregnFeriepengerMedGittOpptjeningsår(opdragFeriepengerPrÅrList, opptjeningsDato);

        Oppdragslinje150.Builder oppdragslinje150Builder = Oppdragslinje150.builder();
        OpprettOppdragslinje150Tjeneste.settFellesFelterIOppdr150(behandlingInfo, oppdragslinje150Builder, false, true);
        oppdragslinje150Builder.medKodeKlassifik(kodeKlassifik)
            .medOppdrag110(oppdrag110)
            .medVedtakFomOgTom(vedtakFom, vedtakTom)
            .medSats(sats);
        if (mottaker.erBruker()) {
            oppdragslinje150Builder.medUtbetalesTilId(Oppdragslinje150Util.endreTilElleveSiffer(mottaker.getId()));
        }
        return oppdragslinje150Builder;
    }

    private static void settRefIdFelterHvisTidligereFeriepengerFinnes(List<Oppdragslinje150> tidligereOpp150FeriepengerListe,
                                                                      List<Oppdragslinje150> endretOpp150FeriepengerListe,
                                                                      Oppdragslinje150.Builder oppdragslinje150Builder,
                                                                      int opptjeningsår) {
        long fagsystemId = tidligereOpp150FeriepengerListe.get(0).getOppdrag110().getFagsystemId();
        int feriepengeår = opptjeningsår + 1;
        endretOpp150FeriepengerListe.stream().filter(opp150 -> opp150.getDatoVedtakFom().getYear() == feriepengeår)
            .findFirst().ifPresent(oppdragslinje150 -> {
            if (!oppdragslinje150.gjelderOpphør()) {
                oppdragslinje150Builder.medRefDelytelseId(oppdragslinje150.getDelytelseId());
                oppdragslinje150Builder.medRefFagsystemId(fagsystemId);
            }
        });
    }

    static void kobleAndreMeldingselementerTilOpp150NyFeriepenger(OppdragInput behandlingInfo,
                                                                  List<Oppdragslinje150> opp150FeriepengerList,
                                                                  Oppdragsmottaker mottaker) {
        for (Oppdragslinje150 opp150 : opp150FeriepengerList) {
            if (!mottaker.erBruker()) {
                LocalDate maksDato = FinnMottakerInfoITilkjentYtelse.finnSisteDagMedUtbetalingTilMottaker(behandlingInfo, mottaker);
                OpprettOppdragsmeldingerRelatertTil150.opprettRefusjonsinfo156(behandlingInfo, opp150, mottaker, maksDato);
            }
        }
    }
}
