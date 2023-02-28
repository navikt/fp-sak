package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;

import no.nav.foreldrepenger.regler.uttak.UttakParametre;

public final class TidsperiodeForbeholdtMor {

    public static LocalDate tilOgMed(LocalDate familiehendelse) {
        return familiehendelse.plusWeeks(UttakParametre.ukerReservertMorEtterFødsel(familiehendelse))
            .minusDays(1);
    }
}
