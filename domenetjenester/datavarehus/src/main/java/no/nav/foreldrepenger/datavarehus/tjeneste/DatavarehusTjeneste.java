package no.nav.foreldrepenger.datavarehus.tjeneste;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;

public interface DatavarehusTjeneste {

    void lagreNedBehandling(Long behandlingId);

    void lagreNedBehandling(Behandling behandling, BehandlingVedtak vedtak);
}
