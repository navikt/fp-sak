package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.util.Collection;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;

public interface DatavarehusTjeneste {

    void lagreNedBehandling(Long behandlingId);

    void lagreNedBehandling(Behandling behandling, BehandlingVedtak vedtak);

    void opprettOgLagreVedtakXml(Long behandlingId);

    void oppdaterHvisKlageEllerAnke(Long behandlingId, Collection<Aksjonspunkt> aksjonspunkter);
}
