package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;

public interface DatavarehusTjeneste {

    void lagreNedBehandling(Long behandlingId);

    void lagreNedVedtak(BehandlingVedtak vedtak, Behandling behandling);

    void opprettOgLagreVedtakXml(Long behandlingId);

    void oppdaterVedtakXml(Long behandlingId);

    List<Long> hentVedtakBehandlinger(LocalDateTime fom, LocalDateTime tom);

    List<Long> hentVedtakBehandlinger(Long behandlingid);

    void oppdaterHvisKlageEllerAnke(Long behandlingId, Collection<Aksjonspunkt> aksjonspunkter);
}
