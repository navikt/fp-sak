package no.nav.foreldrepenger.økonomistøtte.dagytelse.wrapper;

import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.økonomistøtte.Oppdragsmottaker;

public class OppdragsmottakerInfo {

    private final Oppdragsmottaker mottaker;
    private final TilkjentYtelseAndel tilkjentYtelseAndel;
    private final List<Oppdrag110> tidligereOpp110MottakerListe;
    private final List<Oppdragslinje150> tidligereOppdr150MottakerListe;

    public OppdragsmottakerInfo(Oppdragsmottaker mottaker, TilkjentYtelseAndel tilkjentYtelseAndel, List<Oppdrag110> tidligereOpp110MottakerListe,
                                List<Oppdragslinje150> tidligereOppdr150MottakerListe) {
        this.mottaker = mottaker;
        this.tilkjentYtelseAndel = tilkjentYtelseAndel;
        this.tidligereOpp110MottakerListe = Collections.unmodifiableList(tidligereOpp110MottakerListe);
        this.tidligereOppdr150MottakerListe = Collections.unmodifiableList(tidligereOppdr150MottakerListe);
    }

    public Oppdragsmottaker getMottaker() {
        return mottaker;
    }

    public TilkjentYtelseAndel getTilkjentYtelseAndel() {
        return tilkjentYtelseAndel;
    }

    public List<Oppdrag110> getTidligereOpp110MottakerListe() {
        return tidligereOpp110MottakerListe;
    }

    public List<Oppdragslinje150> getTidligereOppdr150MottakerListe() {
        return tidligereOppdr150MottakerListe;
    }
}
