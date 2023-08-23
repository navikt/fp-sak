package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;

import java.util.Optional;

@ApplicationScoped
class RyddOmsorgRettTjeneste {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public RyddOmsorgRettTjeneste(YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    RyddOmsorgRettTjeneste() {
        // CDI
    }

    void ryddVedHoppOverBakover(BehandlingskontrollKontekst kontekst) {
        ytelsesFordelingRepository.hentAggregatHvisEksisterer(kontekst.getBehandlingId())
            .map(a -> YtelseFordelingAggregat.Builder.oppdatere(Optional.of(a)))
            .ifPresent(builder -> {
                builder.medOverstyrtRettighet(null);
                ytelsesFordelingRepository.lagre(kontekst.getBehandlingId(), builder.build());
            });
    }

}
