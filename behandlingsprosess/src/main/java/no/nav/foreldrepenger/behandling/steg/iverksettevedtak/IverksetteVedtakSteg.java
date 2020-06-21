package no.nav.foreldrepenger.behandling.steg.iverksettevedtak;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;

public interface IverksetteVedtakSteg extends BehandlingSteg {

    default Optional<Venteårsak> kanBegynneIverksetting(Behandling behandling) {
        return Optional.empty();
    }

    default void etterInngangFørIverksetting(Behandling behandling, BehandlingVedtak behandlingVedtak) {
        return;
    }
}
