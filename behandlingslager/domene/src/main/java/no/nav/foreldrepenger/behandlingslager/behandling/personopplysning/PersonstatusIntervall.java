package no.nav.foreldrepenger.behandlingslager.behandling.personopplysning;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.fpsak.tidsserie.LocalDateInterval;

public record PersonstatusIntervall(LocalDateInterval intervall, PersonstatusType personstatus) {

    public PersonstatusIntervall(LocalDate fom, LocalDate tom, PersonstatusType personstatus) {
        this(new LocalDateInterval(fom, tom), personstatus);
    }

}
