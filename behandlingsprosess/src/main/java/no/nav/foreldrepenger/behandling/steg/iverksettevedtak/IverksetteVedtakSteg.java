package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;

public interface IverksetteVedtakSteg extends BehandlingSteg {

    // Midlertidig løsning for å flytte berørtBehanlding og vurderOpphørAvYytelser til mottak. Bør håndteres ved fx BehStatusEventObserver som trigger ved IVED.
    default List<String> getInitielleTasks() {
        return Collections.emptyList();
    }
}
