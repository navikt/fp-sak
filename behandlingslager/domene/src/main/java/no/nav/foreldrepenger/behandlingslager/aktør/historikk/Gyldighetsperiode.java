package no.nav.foreldrepenger.behandlingslager.aktør.historikk;

import static no.nav.vedtak.konfig.Tid.TIDENES_BEGYNNELSE;
import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;

import java.time.LocalDate;
import java.util.Optional;


public record Gyldighetsperiode(LocalDate fom, LocalDate tom) {
    public Gyldighetsperiode {
        fom = Optional.ofNullable(fom).orElseGet(() -> TIDENES_BEGYNNELSE);
        tom = Optional.ofNullable(tom).orElseGet(() -> TIDENES_ENDE);
    }

    public Gyldighetsperiode(LocalDate fom) {
        this(fom, null);
    }
}
