package no.nav.foreldrepenger.domene.ytelsefordeling;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittRettighetEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
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

    public void aksjonspunktBekreftFaktaForOmsorg(Long behandlingId, boolean omsorg) {
        var ytelseFordelingAggregat = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOverstyrtOmsorg(omsorg)
            .build();
        ytelsesFordelingRepository.lagre(behandlingId, ytelseFordelingAggregat);
    }

    public void overstyrSøknadsperioder(Long behandlingId,
                                        List<OppgittPeriodeEntitet> overstyrteSøknadsperioder) {
        validerOverlapp(overstyrteSøknadsperioder);
        var oppgittFordeling = ytelsesFordelingRepository.hentAggregat(behandlingId).getOppgittFordeling();
        var erAnnenForelderInformert = oppgittFordeling.getErAnnenForelderInformert();
        var ønskerJustertVedFødsel = oppgittFordeling.ønskerJustertVedFødsel();
        var overstyrtFordeling = new OppgittFordelingEntitet(overstyrteSøknadsperioder, erAnnenForelderInformert, ønskerJustertVedFødsel);

        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId)
            .medOverstyrtFordeling(overstyrtFordeling);
        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());
    }

    private static void validerOverlapp(List<OppgittPeriodeEntitet> perioder) {
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

    public void aksjonspunktAvklarStartdatoForPerioden(Long behandlingId, LocalDate startdatoForPerioden) {
        var aggregat = ytelsesFordelingRepository.hentAggregat(behandlingId);
        var avklarteDatoer = aggregat.getAvklarteDatoer();

        var avklarteUttakDatoer = new AvklarteUttakDatoerEntitet.Builder(avklarteDatoer)
            .medFørsteUttaksdato(startdatoForPerioden);

        var yfBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId).medAvklarteDatoer(avklarteUttakDatoer.build());
        ytelsesFordelingRepository.lagre(behandlingId, yfBuilder.build());
    }

    public void aksjonspunktBekreftFaktaForAleneomsorg(Long behandlingId, boolean aleneomsorg) {
        var avklartRettighet = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId)
            .flatMap(YtelseFordelingAggregat::getAvklartRettighet)
            .orElse(null);
        var ytelseFordelingBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId);
        if (aleneomsorg) {
            avklartRettighet = OppgittRettighetEntitet.kopiAleneomsorgIkkeRettAnnenForelder(avklartRettighet);
        } else {
            avklartRettighet = OppgittRettighetEntitet.kopiIkkeAleneomsorg(avklartRettighet);
        }
        ytelseFordelingBuilder.medAvklartRettighet(avklartRettighet);
        ytelsesFordelingRepository.lagre(behandlingId, ytelseFordelingBuilder.build());
    }

    public void bekreftAnnenforelderHarRett(Long behandlingId, boolean annenforelderHarRett, Boolean annenForelderHarRettEØS, Boolean annenforelderMottarUføretrygd) {
        var avklartRettighet = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId)
            .flatMap(YtelseFordelingAggregat::getAvklartRettighet)
            .orElse(null);
        avklartRettighet = OppgittRettighetEntitet.kopiAnnenForelderRett(avklartRettighet, annenforelderHarRett);
        var ytelseFordelingAggregatBuilder = ytelsesFordelingRepository.opprettBuilder(behandlingId);
        if (annenForelderHarRettEØS != null) {
            avklartRettighet = OppgittRettighetEntitet.kopiAnnenForelderRettEØS(avklartRettighet, annenForelderHarRettEØS);
        }
        if (annenforelderMottarUføretrygd != null) {
            avklartRettighet = OppgittRettighetEntitet.kopiMorUføretrygd(avklartRettighet, annenforelderMottarUføretrygd);
        }
        ytelseFordelingAggregatBuilder.medAvklartRettighet(avklartRettighet);
        ytelsesFordelingRepository.lagre(behandlingId, ytelseFordelingAggregatBuilder.build());
    }

    public void overstyrRettighet(Long behandlingId, Rettighetstype rettighetstype) {
        var eksisterendeYF = ytelsesFordelingRepository.hentAggregat(behandlingId);
        var yfa = YtelseFordelingAggregat.Builder.oppdatere(Optional.of(eksisterendeYF))
            .medOverstyrtRettighetstype(rettighetstype)
            .medAvklartRettighet(null)
            .build();
        ytelsesFordelingRepository.lagre(behandlingId, yfa);
    }

    public Optional<YtelseFordelingGrunnlagEntitet> hentGrunnlagPåId(Long grunnlagId) {
        return ytelsesFordelingRepository.hentGrunnlagPåId(grunnlagId);
    }

    public void lagreSakskompleksDekningsgrad(Long behandlingId, Dekningsgrad sakskompleksDekningsgrad) {
        var eksisterendeYtelsesFordeling = ytelsesFordelingRepository.hentAggregat(behandlingId);
        if (sakskompleksDekningsgrad.equals(eksisterendeYtelsesFordeling.getSakskompleksDekningsgrad())) {
            return;
        }

        var yfa = YtelseFordelingAggregat.Builder.oppdatere(Optional.of(eksisterendeYtelsesFordeling))
            .medSakskompleksDekningsgrad(sakskompleksDekningsgrad)
            .build();

        ytelsesFordelingRepository.lagre(behandlingId, yfa);
    }
}
