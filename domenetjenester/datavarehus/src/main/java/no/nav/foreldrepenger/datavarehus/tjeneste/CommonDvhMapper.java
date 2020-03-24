package no.nav.foreldrepenger.datavarehus.tjeneste;


import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;

class CommonDvhMapper {
    private CommonDvhMapper() {
        // hidden
    }

    static String finnEndretAvEllerOpprettetAv(BaseEntitet base) {
        return base.getEndretAv() == null ? base.getOpprettetAv() : base.getEndretAv();
    }

    static LocalDate foersteStoenadsdag(Optional<ForeldrepengerUttak> uttakResultat, Optional<LocalDate> skjæringstidspunkt) {
        if (uttakResultat.isPresent()) {
            return uttakResultat.get().finnFørsteUttaksdato();
        } else if (skjæringstidspunkt.isPresent()) {
            return skjæringstidspunkt.get();
        }
        return null;
    }
}
