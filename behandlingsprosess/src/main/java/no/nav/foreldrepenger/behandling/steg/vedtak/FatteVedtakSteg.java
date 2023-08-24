package no.nav.foreldrepenger.behandling.steg.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.økonomistøtte.simulering.tjeneste.SimulerInntrekkSjekkeTjeneste;

@BehandlingStegRef(BehandlingStegType.FATTE_VEDTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class FatteVedtakSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private FatteVedtakTjeneste fatteVedtakTjeneste;
    private SimulerInntrekkSjekkeTjeneste simulerInntrekkSjekkeTjeneste;

    FatteVedtakSteg() {
        // for CDI proxy
    }

    @Inject
    public FatteVedtakSteg(BehandlingRepositoryProvider repositoryProvider,
            FatteVedtakTjeneste fatteVedtakTjeneste,
            SimulerInntrekkSjekkeTjeneste simulerInntrekkSjekkeTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.fatteVedtakTjeneste = fatteVedtakTjeneste;
        this.simulerInntrekkSjekkeTjeneste = simulerInntrekkSjekkeTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        simulerInntrekkSjekkeTjeneste.sjekkIntrekk(behandling);
        return fatteVedtakTjeneste.fattVedtak(kontekst, behandling);
    }
}
