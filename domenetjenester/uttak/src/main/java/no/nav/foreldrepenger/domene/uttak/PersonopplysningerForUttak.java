package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;

public interface PersonopplysningerForUttak {

    Optional<LocalDate> søkersDødsdato(BehandlingReferanse ref);

    Optional<LocalDate> søkersDødsdatoGjeldendePåDato(BehandlingReferanse ref, LocalDate dato);

    boolean harOppgittAnnenpart(BehandlingReferanse ref);

    boolean ektefelleHarSammeBosted(BehandlingReferanse ref);

    boolean annenpartHarSammeBosted(BehandlingReferanse ref);

    boolean barnHarSammeBosted(BehandlingReferanse ref);

    boolean oppgittAnnenpartUtenNorskID(BehandlingReferanse referanse);
}
