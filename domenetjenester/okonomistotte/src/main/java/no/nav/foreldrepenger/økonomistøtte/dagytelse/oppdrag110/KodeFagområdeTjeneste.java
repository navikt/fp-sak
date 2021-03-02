package no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdrag110;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeFagområde;

public class KodeFagområdeTjeneste {

    private boolean gjelderForeldrepenger;

    private KodeFagområdeTjeneste(boolean gjelderForeldrepenger) {
        this.gjelderForeldrepenger = gjelderForeldrepenger;
    }

    public static KodeFagområdeTjeneste forForeldrepenger() {
        return new KodeFagområdeTjeneste(true);
    }

    public static KodeFagområdeTjeneste forSvangerskapspenger() {
        return new KodeFagområdeTjeneste(false);
    }

    public KodeFagområde finn(boolean erBrukerMottaker) {
        if (gjelderForeldrepenger) {
            return erBrukerMottaker
                ? KodeFagområde.FORELDREPENGER_BRUKER
                : KodeFagområde.FORELDREPENGER_AG;
        }
        return erBrukerMottaker
            ? KodeFagområde.SVANGERSKAPSPENGER_BRUKER
            : KodeFagområde.SVANGERSKAPSPENGER_AG;
    }

    public boolean gjelderBruker(Oppdrag110 oppdrag110) {
        if (gjelderForeldrepenger) {
            return KodeFagområde.FORELDREPENGER_BRUKER.equals(oppdrag110.getKodeFagomrade());
        }
        return KodeFagområde.SVANGERSKAPSPENGER_BRUKER.equals(oppdrag110.getKodeFagomrade());
    }
}
