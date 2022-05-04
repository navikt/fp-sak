package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;

@ApplicationScoped
class RyddFaktaUttakTjeneste {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    @Inject
    public RyddFaktaUttakTjeneste(YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    RyddFaktaUttakTjeneste() {
        // CDI
    }

    void ryddVedHoppOverBakover(BehandlingskontrollKontekst kontekst) {
        var opprinnelig = ytelsesFordelingRepository.hentAggregatHvisEksisterer(kontekst.getBehandlingId());
        if (opprinnelig.isEmpty()) {
            return;
        }
        var builder = YtelseFordelingAggregat.Builder.oppdatere(opprinnelig)
            .medPerioderUttakDokumentasjon(null)
            .medOverstyrtFordeling(null)
            .medAvklarteDatoer(new AvklarteUttakDatoerEntitet.Builder(opprinnelig.get().getAvklarteDatoer())
                .medJustertEndringsdato(null)
                .build());
        ytelsesFordelingRepository.lagre(kontekst.getBehandlingId(), builder.build());
    }

}
