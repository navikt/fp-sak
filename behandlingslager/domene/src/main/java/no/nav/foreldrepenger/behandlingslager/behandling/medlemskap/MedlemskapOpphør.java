package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;

public record MedlemskapOpphør(LocalDate fom, Avslagsårsak årsak) {

    public MedlemskapOpphør {
        Objects.requireNonNull(fom);
        Objects.requireNonNull(årsak);
        if (årsak == Avslagsårsak.UDEFINERT) {
            throw new IllegalArgumentException("Må ha årsak");
        }
    }
}
