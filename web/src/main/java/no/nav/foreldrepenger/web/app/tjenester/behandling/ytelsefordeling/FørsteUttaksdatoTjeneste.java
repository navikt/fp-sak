package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

/**
 * Tiltenkt bruk i avklare fakta uttak
 */
public interface FørsteUttaksdatoTjeneste {
    Optional<LocalDate> finnFørsteUttaksdato(Behandling behandling);
}
