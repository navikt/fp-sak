package no.nav.foreldrepenger.mottak.hendelser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Denne klassen er nødvendig for at ikke testene i denne modulen skal trigge lagring til DVH grensesnitt,
 * noe som feiler hardt på inkonsistente data.
 */
@ApplicationScoped
@Alternative
public class DummyDatavarehustjeneste implements DatavarehusTjeneste {


    @Override
    public void lagreNedBehandling(Long behandlingId) {

    }

    @Override
    public void lagreNedBehandling(Behandling behandling, BehandlingVedtak vedtak) {

    }

    @Override
    public void opprettOgLagreVedtakXml(Long behandlingId) {

    }

    @Override
    public void oppdaterVedtakXml(Long behandlingId) {

    }

    @Override
    public List<Long> hentVedtakBehandlinger(LocalDateTime fom, LocalDateTime tom) {
        return new ArrayList<>();
    }

    @Override
    public List<Long> hentVedtakBehandlinger(Long behandlingid) {
        return new ArrayList<>();
    }

    @Override
    public void oppdaterHvisKlageEllerAnke(Long behandlingId, Collection<Aksjonspunkt> aksjonspunkter) {

    }

}
