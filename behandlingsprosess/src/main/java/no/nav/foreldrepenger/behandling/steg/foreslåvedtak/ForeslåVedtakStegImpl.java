package no.nav.foreldrepenger.behandling.steg.foreslåvedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;

@BehandlingStegRef(BehandlingStegType.FORESLÅ_VEDTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class ForeslåVedtakStegImpl implements ForeslåVedtakSteg {

    private BehandlingRepository behandlingRepository;
    private ForeslåVedtakTjeneste foreslåVedtakTjeneste;

    ForeslåVedtakStegImpl() {
        // for CDI proxy
    }

    @Inject
    ForeslåVedtakStegImpl(BehandlingRepository behandlingRepository, ForeslåVedtakTjeneste foreslåVedtakTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.foreslåVedtakTjeneste = foreslåVedtakTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        return foreslåVedtakTjeneste.foreslåVedtak(behandling);
    }
}
