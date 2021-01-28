package no.nav.foreldrepenger.domene.ytelsefordeling;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;

class BekreftStartdatoForPeriodenAksjonspunkt {

    private final YtelsesFordelingRepository ytelsesFordelingRepository;

    BekreftStartdatoForPeriodenAksjonspunkt(YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    public void oppdater(Long behandlingId, BekreftStartdatoForPerioden adapter) {
        var aggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        var avklarteDatoer = aggregat.getAvklarteDatoer();

        var avklarteUttakDatoerEntitet = new AvklarteUttakDatoerEntitet.Builder(avklarteDatoer)
            .medFørsteUttaksdato(adapter.getStartdatoForPerioden())
            .build();

        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medAvklarteDatoer(avklarteUttakDatoerEntitet);
        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());
    }
}
