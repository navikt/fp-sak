package no.nav.foreldrepenger.domene.ytelsefordeling;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUtenOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

class BekreftFaktaForOmsorgAksjonspunkt {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

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
        PerioderUtenOmsorgEntitet perioderUtenOmsorg = new PerioderUtenOmsorgEntitet();
        if(Boolean.FALSE.equals(adapter.getOmsorg())) {
            mapPeriodeUtenOmsorgperioder(adapter.getIkkeOmsorgPerioder())
                .forEach(perioderUtenOmsorg::leggTil);
            ytelsesFordelingRepository.lagre(behandlingId, new PerioderUtenOmsorgEntitet(perioderUtenOmsorg));
        } else {
            ytelsesFordelingRepository.lagre(behandlingId, perioderUtenOmsorg);
        }
    }

    private void bekreftFaktaAleneomsorg(Long behandlingId, BekreftFaktaForOmsorgVurderingAksjonspunktDto adapter) {
        PerioderAleneOmsorgEntitet perioderAleneOmsorgEntitet = new PerioderAleneOmsorgEntitet();
        PerioderAnnenforelderHarRettEntitet perioderAnnenforelderHarRettEntitet = new PerioderAnnenforelderHarRettEntitet();
        if(Boolean.FALSE.equals(adapter.getAleneomsorg())) {
            ytelsesFordelingRepository.lagre(behandlingId, perioderAleneOmsorgEntitet);
            //Lagre annen forelder har rett
            // Legger inn en dummy periode for å indikere saksbehandlers valg. Inntil vi faktisk har perioder her
            perioderAnnenforelderHarRettEntitet.leggTil(new PeriodeAnnenforelderHarRettEntitet(LocalDate.now(), LocalDate.now()));
        } else {
            // Legger inn en dummy periode for å indikere saksbehandlers valg. Inntil vi faktisk har perioder her
            perioderAleneOmsorgEntitet.leggTil(new PeriodeAleneOmsorgEntitet(LocalDate.now(), LocalDate.now()));
            ytelsesFordelingRepository.lagre(behandlingId, new PerioderAleneOmsorgEntitet(perioderAleneOmsorgEntitet));
        }
        ytelsesFordelingRepository.lagre(behandlingId, perioderAnnenforelderHarRettEntitet);
    }

    private static List<PeriodeUtenOmsorgEntitet> mapPeriodeUtenOmsorgperioder(List<DatoIntervallEntitet> ikkeOmsorgPeriodes) {
       return ikkeOmsorgPeriodes.stream().map(BekreftFaktaForOmsorgAksjonspunkt::mapPeriodeUtenOmsorg).collect(Collectors.toList());
    }

    private static PeriodeUtenOmsorgEntitet mapPeriodeUtenOmsorg(DatoIntervallEntitet ikkeOmsorgPeriode) {
        return new PeriodeUtenOmsorgEntitet(ikkeOmsorgPeriode.getFomDato(), ikkeOmsorgPeriode.getTomDato());
    }
}
