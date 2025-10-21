package no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;

/**
 * Barn som ikke har en kjent id, men som kun identifiseres ved fødselsdato og et løpenummer i en sak.
 * (eks. før fødsel, eller før registrert i Folkeregisteret/AktørId).
 */
public interface UidentifisertBarn {

    Comparator<UidentifisertBarn> FØDSEL_COMPARATOR = Comparator.comparing(UidentifisertBarn::getFødselsdato, Comparator.naturalOrder())
            .thenComparing(UidentifisertBarn::getDødsdatoNullable, Comparator.nullsFirst(Comparator.naturalOrder()));

    LocalDate getFødselsdato();

    Optional<LocalDate> getDødsdato();

    default LocalDate getDødsdatoNullable() {
        return getDødsdato().orElse(null);
    }

    Integer getBarnNummer();

}
