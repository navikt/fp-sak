package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;

@ApplicationScoped
class RyddKontrollerFaktaUttakTjeneste {

    private YtelsesFordelingRepository ytelsesfordelingRepository;

    @Inject
    public RyddKontrollerFaktaUttakTjeneste(YtelsesFordelingRepository ytelsesfordelingRepository) {
        this.ytelsesfordelingRepository = ytelsesfordelingRepository;
    }

    RyddKontrollerFaktaUttakTjeneste() {
        //CDI
    }

    void ryddVedHoppOverBakover(BehandlingskontrollKontekst kontekst) {
        Optional<YtelseFordelingAggregat> opprinnelig = ytelsesfordelingRepository.hentAggregatHvisEksisterer(kontekst.getBehandlingId());
        if (opprinnelig.isPresent()) {
            YtelseFordelingAggregat.Builder builder = YtelseFordelingAggregat.Builder.oppdatere(opprinnelig);
            YtelseFordelingAggregat ytelseFordeling = builder
                .medPerioderUttakDokumentasjon(null)
                .medOverstyrtFordeling(null)
                .medAvklarteDatoer(new AvklarteUttakDatoerEntitet.Builder(opprinnelig.get().getAvklarteDatoer())
                    .medJustertEndringsdato(null)
                    .build())
                .build();

            ytelsesfordelingRepository.lagre(kontekst.getBehandlingId(), ytelseFordeling);
        }
    }
}
