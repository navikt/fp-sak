package no.nav.foreldrepenger.økonomistøtte.dagytelse.oppdrag110;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;

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

    public String finn(boolean erBrukerMottaker) {
        if (gjelderForeldrepenger) {
            return erBrukerMottaker
                ? ØkonomiKodeFagområde.FP.name()
                : ØkonomiKodeFagområde.FPREF.name();
        }
        return erBrukerMottaker
            ? ØkonomiKodeFagområde.SVP.name()
            : ØkonomiKodeFagområde.SVPREF.name();
    }

    public boolean gjelderBruker(Oppdrag110 oppdrag110) {
        ØkonomiKodeFagområde.valider(oppdrag110.getKodeFagomrade());
        if (gjelderForeldrepenger) {
            return ØkonomiKodeFagområde.FP.name().equals(oppdrag110.getKodeFagomrade());
        }
        return ØkonomiKodeFagområde.SVP.name().equals(oppdrag110.getKodeFagomrade());
    }
}
