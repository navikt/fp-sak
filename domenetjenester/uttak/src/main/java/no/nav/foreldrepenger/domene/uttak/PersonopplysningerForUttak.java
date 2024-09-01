package no.nav.foreldrepenger.domene.uttak;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;

public interface PersonopplysningerForUttak {

    Optional<LocalDate> søkersDødsdato(BehandlingReferanse ref);

    boolean harOppgittAnnenpartMedNorskID(BehandlingReferanse ref);

    boolean ektefelleHarSammeBosted(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt);

    boolean annenpartHarSammeBosted(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt);

    boolean barnHarSammeBosted(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt);

    boolean oppgittAnnenpartUtenNorskID(BehandlingReferanse referanse);
}
