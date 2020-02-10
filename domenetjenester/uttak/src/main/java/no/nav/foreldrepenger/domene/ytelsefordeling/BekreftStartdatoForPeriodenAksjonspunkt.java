package no.nav.foreldrepenger.domene.ytelsefordeling;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;

class BekreftStartdatoForPeriodenAksjonspunkt {

    private final YtelsesFordelingRepository repository;

    BekreftStartdatoForPeriodenAksjonspunkt(YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.repository = ytelsesFordelingRepository;
    }

    public void oppdater(Long behandlingId, BekreftStartdatoForPerioden adapter) {
        final YtelseFordelingAggregat aggregat = repository.hentAggregat(behandlingId);
        final Optional<AvklarteUttakDatoerEntitet> avklarteDatoer = aggregat.getAvklarteDatoer();

        AvklarteUttakDatoerEntitet.Builder builder = new AvklarteUttakDatoerEntitet.Builder(avklarteDatoer)
            .medFørsteUttaksdato(adapter.getStartdatoForPerioden());

        repository.lagre(behandlingId, builder.build());
    }

}
