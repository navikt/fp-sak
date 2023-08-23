package no.nav.foreldrepenger.domene.uttak;

import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.regler.uttak.UttakParametre;

import java.time.LocalDate;

public final class TidsperiodeForbeholdtMor {

    private TidsperiodeForbeholdtMor() {
    }

    public static AbstractLocalDateInterval tidsperiode(LocalDate familiehendelse) {
        return new SimpleLocalDateInterval(fraOgMed(familiehendelse), tilOgMed(familiehendelse));
    }

    public static LocalDate fraOgMed(LocalDate familiehendelse) {
        return familiehendelse.minusWeeks(UttakParametre.ukerFørTerminSenestUttak(familiehendelse));
    }

    public static LocalDate tilOgMed(LocalDate familiehendelse) {
        return familiehendelse.plusWeeks(UttakParametre.ukerReservertMorEtterFødsel(familiehendelse))
            .minusDays(1);
    }
}
