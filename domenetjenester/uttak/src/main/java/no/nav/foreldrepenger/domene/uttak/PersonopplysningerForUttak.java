package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;

public interface PersonopplysningerForUttak {

    Optional<LocalDate> søkersDødsdato(BehandlingReferanse ref);

    boolean harOppgittAnnenpartMedNorskID(BehandlingReferanse ref);

    boolean ektefelleHarSammeBosted(BehandlingReferanse ref);

    boolean annenpartHarSammeBosted(BehandlingReferanse ref);

    boolean barnHarSammeBosted(BehandlingReferanse ref);

    boolean oppgittAnnenpartUtenNorskID(BehandlingReferanse referanse);
}
