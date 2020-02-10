package no.nav.foreldrepenger.behandling.steg.vedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.økonomi.simulering.tjeneste.SimulerInntrekkSjekkeTjeneste;

@BehandlingStegRef(kode = "FVEDSTEG")
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
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        simulerInntrekkSjekkeTjeneste.sjekkIntrekk(behandling);
        return fatteVedtakTjeneste.fattVedtak(kontekst, behandling);
    }
}
