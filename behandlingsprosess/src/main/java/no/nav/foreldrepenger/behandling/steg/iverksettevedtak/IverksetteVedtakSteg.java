package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;

public interface IverksetteVedtakSteg extends BehandlingSteg {

    // Midlertidig løsning for å flytte berørtBehanlding og vurderOpphørAvYytelser til mottak. Bør håndteres ved fx BehStatusEventObserver som trigger ved IVED.
    default List<String> getInitielleTasks() {
        return Collections.emptyList();
    }

    default Optional<Venteårsak> kanBegynneIverksetting(Behandling behandling) {
        return Optional.empty();
    }

    default void etterInngangFørIverksetting(Behandling behandling, BehandlingVedtak behandlingVedtak) {
        return;
    }
}
