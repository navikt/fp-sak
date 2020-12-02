package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;

@ApplicationScoped
class RyddFaktaUttakTjenesteFørstegangsbehandling {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public RyddFaktaUttakTjenesteFørstegangsbehandling(YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    RyddFaktaUttakTjenesteFørstegangsbehandling() {
        // CDI
    }

    void ryddVedHoppOverBakover(BehandlingskontrollKontekst kontekst) {
        yfBuilder(kontekst.getBehandlingId(), ytelsesFordelingRepository).ifPresent(builder -> {
            // Om annen forelder har rett avklares bare i førstegangsbehandling
            builder.medPerioderAnnenforelderHarRett(null);
            lagre(kontekst, builder);
        });
    }

    private void lagre(BehandlingskontrollKontekst kontekst, YtelseFordelingAggregat.Builder builder) {
        ytelsesFordelingRepository.lagre(kontekst.getBehandlingId(), builder.build());
    }

    static Optional<YtelseFordelingAggregat.Builder> yfBuilder(Long behandlingId, YtelsesFordelingRepository ytelsesFordelingRepository) {
        var opprinnelig = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);
        if (opprinnelig.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(YtelseFordelingAggregat.Builder.oppdatere(opprinnelig)
                .medPerioderUttakDokumentasjon(null)
                .medOverstyrtFordeling(null)
                .medAvklarteDatoer(new AvklarteUttakDatoerEntitet.Builder(opprinnelig.get().getAvklarteDatoer())
                        .medJustertEndringsdato(null)
                        .build()));
    }

}
