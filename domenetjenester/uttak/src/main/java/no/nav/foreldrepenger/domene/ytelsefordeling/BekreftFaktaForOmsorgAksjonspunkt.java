package no.nav.foreldrepenger.domene.ytelsefordeling;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

class BekreftFaktaForOmsorgAksjonspunkt {

    private final YtelsesFordelingRepository ytelsesFordelingRepository;

    public BekreftFaktaForOmsorgAksjonspunkt(YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    void oppdater(Long behandlingId, BekreftFaktaForOmsorgVurderingAksjonspunktDto adapter) {
        if(adapter.getAleneomsorg() != null) {
            bekreftFaktaAleneomsorg(behandlingId, adapter);
        }
        if(adapter.getOmsorg() != null) {
            bekreftFaktaOmsorg(behandlingId, adapter);
        }
    }

    private void bekreftFaktaOmsorg(Long behandlingId, BekreftFaktaForOmsorgVurderingAksjonspunktDto adapter) {
        var perioderUtenOmsorg = new PerioderUtenOmsorgEntitet();
        if (Boolean.FALSE.equals(adapter.getOmsorg())) {
            mapPeriodeUtenOmsorgperioder(adapter.getIkkeOmsorgPerioder()).forEach(perioderUtenOmsorg::leggTil);
        }
        var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medPerioderUtenOmsorg(perioderUtenOmsorg)
            .build();
        ytelsesFordelingRepository.lagre(behandlingId, ytelseFordelingAggregat);
    }

    private void bekreftFaktaAleneomsorg(Long behandlingId, BekreftFaktaForOmsorgVurderingAksjonspunktDto adapter) {
        var erAleneomsorg = !Boolean.FALSE.equals(adapter.getAleneomsorg());
        var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medPerioderAleneOmsorg(new PerioderAleneOmsorgEntitet(erAleneomsorg))
            .medPerioderAnnenforelderHarRett(new PerioderAnnenforelderHarRettEntitet(!erAleneomsorg))
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
