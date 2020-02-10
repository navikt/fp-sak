package no.nav.foreldrepenger.datavarehus.tjeneste;


import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakResultatEntitet;

class CommonDvhMapper {
    private CommonDvhMapper() {
        // hidden
    }

    static String finnEndretAvEllerOpprettetAv(BaseEntitet base) {
        return base.getEndretAv() == null ? base.getOpprettetAv() : base.getEndretAv();
    }

    static LocalDate foersteStoenadsdag(Optional<UttakResultatEntitet> uttakResultat, Optional<LocalDate> skjæringstidspunkt) {
        if (uttakResultat.isPresent()) {
            Optional<LocalDate> førsteUttaksdato = uttakResultat.get().getGjeldendePerioder().finnFørsteUttaksdato();
            return førsteUttaksdato.orElseThrow(() -> new IllegalStateException("Utviklerfeil: Skal ikke skje at man har uttak uten perioder"));
        } else if (skjæringstidspunkt.isPresent()) {
            return skjæringstidspunkt.get();
        }
        return null;
    }
}
