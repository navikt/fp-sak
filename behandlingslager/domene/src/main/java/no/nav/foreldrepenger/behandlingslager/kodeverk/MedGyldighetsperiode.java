package no.nav.foreldrepenger.behandlingslager.kodeverk;

import java.time.LocalDate;

/** Kodeverk som er portet til java. */
public interface MedGyldighetsperiode {

    LocalDate getGyldigFraOgMed();
    LocalDate getGyldigTilOgMed();

}
