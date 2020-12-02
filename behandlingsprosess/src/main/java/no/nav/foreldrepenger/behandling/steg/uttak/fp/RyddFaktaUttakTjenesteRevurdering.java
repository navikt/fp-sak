package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import static no.nav.foreldrepenger.behandling.steg.uttak.fp.RyddFaktaUttakTjenesteFørstegangsbehandling.yfBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;

@ApplicationScoped
class RyddFaktaUttakTjenesteRevurdering {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public RyddFaktaUttakTjenesteRevurdering(YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    RyddFaktaUttakTjenesteRevurdering() {
        // CDI
    }

    void ryddVedHoppOverBakover(BehandlingskontrollKontekst kontekst) {
        yfBuilder(kontekst.getBehandlingId(), ytelsesFordelingRepository).ifPresent(builder -> lagre(kontekst, builder));
    }

    private void lagre(BehandlingskontrollKontekst kontekst, YtelseFordelingAggregat.Builder builder) {
        ytelsesFordelingRepository.lagre(kontekst.getBehandlingId(), builder.build());
    }
}
