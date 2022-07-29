package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;

import no.nav.foreldrepenger.regler.uttak.konfig.Konfigurasjon;
import no.nav.foreldrepenger.regler.uttak.konfig.Parametertype;

public final class TidsperiodeForbeholdtMor {

    public static LocalDate tilOgMed(LocalDate familiehendelse) {
        return familiehendelse.plusWeeks(
                Konfigurasjon.STANDARD.getParameter(Parametertype.UTTAK_MØDREKVOTE_ETTER_FØDSEL_UKER, familiehendelse))
            .minusDays(1);
    }
}
