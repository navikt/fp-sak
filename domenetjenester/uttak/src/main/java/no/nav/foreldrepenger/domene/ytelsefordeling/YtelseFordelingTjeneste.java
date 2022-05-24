package no.nav.foreldrepenger.domene.ytelsefordeling;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAleneOmsorgEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderMorStønadEØSEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class YtelseFordelingTjeneste {

    private YtelsesFordelingRepository ytelsesFordelingRepository;

    YtelseFordelingTjeneste() {
        //CDI
    }

    @Inject
    public YtelseFordelingTjeneste(YtelsesFordelingRepository ytelsesFordelingRepository) {
        this.ytelsesFordelingRepository = ytelsesFordelingRepository;
    }

    public YtelseFordelingAggregat hentAggregat(Long behandlingId) {
        return ytelsesFordelingRepository.hentAggregat(behandlingId);
    }

    public Optional<YtelseFordelingAggregat> hentAggregatHvisEksisterer(Long behandlingId) {
        return ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);
    }

    public void aksjonspunktBekreftFaktaForOmsorg(Long behandlingId, boolean omsorg, List<DatoIntervallEntitet> ikkeOmsorgPerioder) {
        new BekreftFaktaForOmsorgAksjonspunkt(ytelsesFordelingRepository).oppdater(behandlingId, omsorg, ikkeOmsorgPerioder);
    }

    public void aksjonspunktBekreftFaktaForAleneomsorg(Long behandlingId, boolean aleneomsorg) {
        var ytelseFordelingBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medPerioderAleneOmsorg(new PerioderAleneOmsorgEntitet(aleneomsorg));
        if (aleneomsorg) {
            ytelseFordelingBuilder.medPerioderAnnenforelderHarRett(new PerioderAnnenforelderHarRettEntitet(false));
        }
        ytelsesFordelingRepository.lagre(behandlingId, ytelseFordelingBuilder.build());
    }

    public void overstyrSøknadsperioder(Long behandlingId,
                                        List<OppgittPeriodeEntitet> overstyrteSøknadsperioder,
                                        List<PeriodeUttakDokumentasjonEntitet> dokumentasjonsperioder) {
        validerOverlapp(overstyrteSøknadsperioder);
        var erAnnenForelderInformert = ytelsesFordelingRepository.hentAggregat(behandlingId).getOppgittFordeling().getErAnnenForelderInformert();
        var overstyrtFordeling = new OppgittFordelingEntitet(overstyrteSøknadsperioder, erAnnenForelderInformert);

        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOverstyrtFordeling(overstyrtFordeling)
            .medPerioderUttakDokumentasjon(map(dokumentasjonsperioder));
        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());
    }

    private PerioderUttakDokumentasjonEntitet map(List<PeriodeUttakDokumentasjonEntitet> dokumentasjonsperioder) {
        if (dokumentasjonsperioder.isEmpty()) {
            return null;
        }
        var perioderUttakDokumentasjon = new PerioderUttakDokumentasjonEntitet();
        dokumentasjonsperioder.forEach(perioderUttakDokumentasjon::leggTil);
        return perioderUttakDokumentasjon;
    }

    private void validerOverlapp(List<OppgittPeriodeEntitet> perioder) {
        for (var i = 0; i < perioder.size(); i++) {
            for (var j = i + 1; j < perioder.size(); j++) {
                var per1 = new LocalDateInterval(perioder.get(i).getFom(), perioder.get(i).getTom());
                var per2 = new LocalDateInterval(perioder.get(j).getFom(), perioder.get(j).getTom());
                if (per1.overlaps(per2)) {
                    throw new IllegalArgumentException(per1 + " overlapper med " + per2);
                }
            }
        }
    }

    public EndringsresultatSnapshot finnAktivAggregatId(Long behandlingId) {
        var funnetId = ytelsesFordelingRepository.hentIdPåAktivYtelsesFordeling(behandlingId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(YtelseFordelingAggregat.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(YtelseFordelingAggregat.class));
    }

    public void aksjonspunktAvklarStartdatoForPerioden(Long behandlingId, BekreftStartdatoForPerioden adapter) {
        new BekreftStartdatoForPeriodenAksjonspunkt(ytelsesFordelingRepository).oppdater(behandlingId, adapter);
    }

    public void bekreftAnnenforelderHarRett(Long behandlingId, Boolean annenforelderHarRett, Boolean annenforelderMottarStønadEØS) {
        var perioderAnnenforelderHarRettEntitet = new PerioderAnnenforelderHarRettEntitet(annenforelderHarRett);

        var ytelseFordelingAggregatBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medPerioderAnnenforelderHarRett(perioderAnnenforelderHarRettEntitet);
        if (annenforelderMottarStønadEØS != null) {
            var perioderMorStønadEØSEntitet = new PerioderMorStønadEØSEntitet(annenforelderMottarStønadEØS);
            ytelseFordelingAggregatBuilder.medPerioderMorStønadEØS(perioderMorStønadEØSEntitet);
        }
        ytelsesFordelingRepository.lagre(behandlingId, ytelseFordelingAggregatBuilder.build());
    }

    public Optional<YtelseFordelingGrunnlagEntitet> hentGrunnlagPåId(Long grunnlagId) {
        return ytelsesFordelingRepository.hentGrunnlagPåId(grunnlagId);
    }
}
