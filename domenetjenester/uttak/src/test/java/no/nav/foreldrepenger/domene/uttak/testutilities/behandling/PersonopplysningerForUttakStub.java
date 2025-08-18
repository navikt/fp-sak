package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.domene.uttak.PersonopplysningerForUttak;

public class PersonopplysningerForUttakStub implements PersonopplysningerForUttak {

    @Override
    public Optional<LocalDate> søkersDødsdato(BehandlingReferanse ref) {
        return Optional.empty();
    }

    @Override
    public boolean harOppgittAnnenpartMedNorskID(BehandlingReferanse ref) {
        return true;
    }

    @Override
    public boolean ektefelleHarSammeBosted(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt) {
        return true;
    }

    @Override
    public boolean annenpartHarSammeBosted(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt) {
        return true;
    }

    @Override
    public boolean barnHarSammeBosted(BehandlingReferanse ref, Skjæringstidspunkt skjæringstidspunkt) {
        return true;
    }
}
