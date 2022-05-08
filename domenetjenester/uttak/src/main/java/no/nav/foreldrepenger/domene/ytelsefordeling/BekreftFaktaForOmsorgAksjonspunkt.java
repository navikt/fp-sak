package no.nav.foreldrepenger.domene.ytelsefordeling;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

class BekreftFaktaForOmsorgAksjonspunkt {

    private final YtelsesFordelingRepository ytelsesFordelingRepository;

    public BekreftFaktaForOmsorgAksjonspunkt(YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    void oppdater(Long behandlingId, boolean omsorg, List<DatoIntervallEntitet> ikkeOmsorgPerioder) {
        var perioderUtenOmsorg = new PerioderUtenOmsorgEntitet();
        if (!omsorg) {
            mapPeriodeUtenOmsorgperioder(ikkeOmsorgPerioder).forEach(perioderUtenOmsorg::leggTil);
        }
        var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medPerioderUtenOmsorg(perioderUtenOmsorg)
            .build();
        ytelsesFordelingRepository.lagre(behandlingId, ytelseFordelingAggregat);
    }

    private static List<PeriodeUtenOmsorgEntitet> mapPeriodeUtenOmsorgperioder(List<DatoIntervallEntitet> ikkeOmsorgPeriodes) {
       return ikkeOmsorgPeriodes.stream().map(BekreftFaktaForOmsorgAksjonspunkt::mapPeriodeUtenOmsorg).collect(Collectors.toList());
    }

    private static PeriodeUtenOmsorgEntitet mapPeriodeUtenOmsorg(DatoIntervallEntitet ikkeOmsorgPeriode) {
        return new PeriodeUtenOmsorgEntitet(ikkeOmsorgPeriode.getFomDato(), ikkeOmsorgPeriode.getTomDato());
    }
}
