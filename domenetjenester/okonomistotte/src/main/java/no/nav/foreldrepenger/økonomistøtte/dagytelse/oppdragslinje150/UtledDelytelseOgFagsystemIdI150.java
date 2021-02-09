package no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdragslinje150;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.økonomistøtte.OpprettOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.KlassekodeUtleder;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.TidligereOppdragTjeneste;
import no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper.OppdragsmottakerInfo;

class UtledDelytelseOgFagsystemIdI150 {

    private static final int INITIAL_TELLER = 100;
    private static final int TELLER_I_ANDRE_ITER = 101;

    private UtledDelytelseOgFagsystemIdI150() {
    }

    static void settRefDelytelseOgFagsystemId(Oppdrag110 oppdrag110, List<Long> delYtelseIdListe, int count,
                                              int teller, Oppdragslinje150.Builder oppdragslinje150Builder) {
        long fagsystemId = oppdrag110.getFagsystemId();
        long delytelseId = OpprettOppdragTjeneste.concatenateValues(fagsystemId, teller);
        delYtelseIdListe.add(delytelseId);
        oppdragslinje150Builder.medDelytelseId(delytelseId);
        if (teller > INITIAL_TELLER + count) {
            int ix = teller - (TELLER_I_ANDRE_ITER + count);
            oppdragslinje150Builder.medRefFagsystemId(fagsystemId);
            oppdragslinje150Builder.medRefDelytelseId(delYtelseIdListe.get(ix));
        }
    }

    static void settRefDelytelseOgFagsystemId(OppdragsmottakerInfo oppdragInfo, Oppdrag110 nyOppdrag110, Oppdragslinje150.Builder oppdragslinje150Builder,
                                              List<Long> delYtelseIdListe, int antallIter) {
        Oppdragslinje150 sisteOppdr150;
        boolean erDetNyKlassekodeINyOppdrag;
        List<Oppdragslinje150> tidligereOppdr150Liste = oppdragInfo.getTidligereOppdr150MottakerListe();
        if (oppdragInfo.getMottaker().erBruker()) {
            KodeKlassifik kodeklassifikINyeAndeler = KlassekodeUtleder.utled(oppdragInfo.getTilkjentYtelseAndel());
            Optional<Oppdragslinje150> sisteOppdr150Opt = finnOpp150MedGittKodeKlassifikOgMaksId(tidligereOppdr150Liste, kodeklassifikINyeAndeler);
            erDetNyKlassekodeINyOppdrag = !sisteOppdr150Opt.isPresent();
            sisteOppdr150 = sisteOppdr150Opt.orElseGet(() -> Oppdragslinje150Util.getOpp150MedMaxDelytelseId(tidligereOppdr150Liste));
        } else {
            sisteOppdr150 = Oppdragslinje150Util.getOpp150MedMaxDelytelseId(tidligereOppdr150Liste);
            erDetNyKlassekodeINyOppdrag = false;
        }

        settRefFagsystemId(nyOppdrag110, oppdragslinje150Builder, erDetNyKlassekodeINyOppdrag, antallIter);
        long delytelseId = finnDelytelseId(oppdragInfo, nyOppdrag110, sisteOppdr150, antallIter);
        settRefDelYtelseId(oppdragslinje150Builder, sisteOppdr150, delytelseId, antallIter, erDetNyKlassekodeINyOppdrag);
        delYtelseIdListe.add(delytelseId);
        if (antallIter > 0) {
            oppdragslinje150Builder.medRefDelytelseId(delYtelseIdListe.get(antallIter - 1));
        }
    }

    private static Optional<Oppdragslinje150> finnOpp150MedGittKodeKlassifikOgMaksId(List<Oppdragslinje150> tidligereOppdr150Liste, KodeKlassifik kodeklassifikINyeAndeler) {
        return tidligereOppdr150Liste.stream()
            .filter(oppdr150 -> oppdr150.getKodeKlassifik().equals(kodeklassifikINyeAndeler))
            .max(Comparator.comparing(Oppdragslinje150::getDelytelseId).thenComparing(Oppdragslinje150::getKodeStatusLinje, Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    private static void settRefFagsystemId(Oppdrag110 nyOppdrag110, Oppdragslinje150.Builder oppdragslinje150Builder, boolean erDetNyKlassekodeINyOppdrag, int antalIter) {
        if (antalIter == 0) {
            if (!erDetNyKlassekodeINyOppdrag) {
                oppdragslinje150Builder.medRefFagsystemId(nyOppdrag110.getFagsystemId());
            }
        } else {
            oppdragslinje150Builder.medRefFagsystemId(nyOppdrag110.getFagsystemId());
        }
    }

    private static void settRefDelYtelseId(Oppdragslinje150.Builder oppdragslinje150Builder, Oppdragslinje150 sisteOppdr150,
                                           long delytelseId, int antallIterasjoner, boolean erDetNyKlassekodeINyOppdrag) {
        oppdragslinje150Builder.medDelytelseId(delytelseId);
        if (antallIterasjoner == 0 && !erDetNyKlassekodeINyOppdrag) {
            oppdragslinje150Builder.medRefDelytelseId(sisteOppdr150.getDelytelseId());
        }
    }

    private static long finnDelytelseId(OppdragsmottakerInfo oppdragInfo, Oppdrag110 nyOppdrag110, Oppdragslinje150 sisteOppdr150, int antallIterasjoner) {
        long sisteDelytelseIdINyOpp10 = TidligereOppdragTjeneste.finnMaxDelytelseIdForEnOppdrag110(nyOppdrag110, sisteOppdr150);
        long sisteDelytelseIdITidligereOpp10 = TidligereOppdragTjeneste.finnMaxDelytelseIdITidligereOppdragForMottakeren(oppdragInfo);

        return sisteDelytelseIdINyOpp10 > sisteDelytelseIdITidligereOpp10 ? sisteDelytelseIdINyOpp10 + 1L
            : sisteDelytelseIdITidligereOpp10 + 1L + antallIterasjoner;
    }
}
