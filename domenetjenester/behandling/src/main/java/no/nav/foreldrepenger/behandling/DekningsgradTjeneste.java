package no.nav.foreldrepenger.behandling;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
public class DekningsgradTjeneste {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    DekningsgradTjeneste() {
        // CDI
    }

    @Inject
    public DekningsgradTjeneste(YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    public Optional<Dekningsgrad> finnGjeldendeDekningsgradHvisEksisterer(BehandlingReferanse ref) {
        //Bare foreldrepenger dekningsgrad kan være noe annet enn 100
        if (ref.fagsakYtelseType() != FagsakYtelseType.FORELDREPENGER) {
            return Optional.of(Dekningsgrad._100);
        }
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(ref.behandlingId())
            .map(YtelseFordelingAggregat::getGjeldendeDekningsgrad);
    }

    public Dekningsgrad finnGjeldendeDekningsgrad(BehandlingReferanse ref) {
        return finnGjeldendeDekningsgradHvisEksisterer(ref).orElseThrow();
    }

    public Optional<Dekningsgrad> finnOppgittDekningsgrad(BehandlingReferanse ref) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(ref.behandlingId()).map(YtelseFordelingAggregat::getOppgittDekningsgrad);
    }

    public boolean behandlingHarEndretDekningsgrad(BehandlingReferanse ref) {
        var yfaOpt = ytelsesFordelingRepository.hentAggregatHvisEksisterer(ref.behandlingId());
        if (yfaOpt.isEmpty()) {
            return false;
        }
        var ytelseFordelingAggregat = yfaOpt.orElseThrow();
        return ref.getOriginalBehandlingId().map(originalBehandling -> {
            var originalDekningsgrad = ytelsesFordelingRepository.hentAggregat(originalBehandling).getGjeldendeDekningsgrad();
            var behandlingDekningsgad = ytelseFordelingAggregat.getGjeldendeDekningsgrad();
            return !Objects.equals(originalDekningsgrad, behandlingDekningsgad);
        }).orElseGet(() -> !Objects.equals(ytelseFordelingAggregat.getGjeldendeDekningsgrad(), ytelseFordelingAggregat.getOppgittDekningsgrad()));

    }

}
