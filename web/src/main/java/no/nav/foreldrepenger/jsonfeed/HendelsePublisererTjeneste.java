package no.nav.foreldrepenger.jsonfeed;

import no.nav.foreldrepenger.behandling.FagsakStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;

public interface HendelsePublisererTjeneste {
    void lagreVedtak(BehandlingVedtak vedtak);

    void lagreFagsakAvsluttet(FagsakStatusEvent event);
}
