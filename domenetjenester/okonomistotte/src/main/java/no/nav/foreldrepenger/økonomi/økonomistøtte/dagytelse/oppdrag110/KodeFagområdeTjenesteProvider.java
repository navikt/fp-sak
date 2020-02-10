package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.oppdrag110;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomiKodeFagområde;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;

public class KodeFagområdeTjenesteProvider {

    private KodeFagområdeTjenesteProvider() {
        // skjul default constructor
    }

    public static KodeFagområdeTjeneste getKodeFagområdeTjeneste(OppdragInput behandlingInfo) {
        if (behandlingInfo.getTilkjentYtelse().isPresent()) {
            return behandlingInfo.gjelderForeldrepenger()
                ? KodeFagområdeTjeneste.forForeldrepenger()
                : KodeFagområdeTjeneste.forSvangerskapspenger();
        }
        return fraTidligereOppdrag(behandlingInfo.getAlleTidligereOppdrag110());
    }

    private static KodeFagområdeTjeneste fraTidligereOppdrag(List<Oppdrag110> tidligereOppdrag110) {
        boolean gjelderFP = tidligereOppdrag110.stream()
            .anyMatch(o110 -> ØkonomiKodeFagområde.gjelderForeldrepenger(o110.getKodeFagomrade()));

        return gjelderFP
            ? KodeFagområdeTjeneste.forForeldrepenger()
            : KodeFagområdeTjeneste.forSvangerskapspenger();
    }
}
