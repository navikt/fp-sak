package no.nav.foreldrepenger.domene.ytelsefordeling;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderAnnenforelderHarRettEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
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

    public void aksjonspunktBekreftFaktaForOmsorg(Long behandlingId, BekreftFaktaForOmsorgVurderingAksjonspunktDto adapter) {
        new BekreftFaktaForOmsorgAksjonspunkt(ytelsesFordelingRepository).oppdater(behandlingId, adapter);
    }

    public void overstyrSøknadsperioder(Long behandlingId,
                                        List<OppgittPeriodeEntitet> overstyrteSøknadsperioder,
                                        List<PeriodeUttakDokumentasjonEntitet> dokumentasjonsperioder) {
        validerOverlapp(overstyrteSøknadsperioder);
        var erAnnenForelderInformert = ytelsesFordelingRepository.hentAggregat(behandlingId).getOppgittFordeling().getErAnnenForelderInformert();
        var oppgittFordelingEntitet = new OppgittFordelingEntitet(overstyrteSøknadsperioder, erAnnenForelderInformert);
        if (dokumentasjonsperioder.isEmpty()) {
            ytelsesFordelingRepository.lagreOverstyrtFordeling(behandlingId, oppgittFordelingEntitet, null);
        } else {
            PerioderUttakDokumentasjonEntitet perioderUttakDokumentasjon = new PerioderUttakDokumentasjonEntitet();
            dokumentasjonsperioder.forEach(perioderUttakDokumentasjon::leggTil);
            ytelsesFordelingRepository.lagreOverstyrtFordeling(behandlingId, oppgittFordelingEntitet, perioderUttakDokumentasjon);
        }
    }

    private void validerOverlapp(List<OppgittPeriodeEntitet> perioder) {
        for (int i = 0; i < perioder.size(); i++) {
            for (int j = i + 1; j < perioder.size(); j++) {
                var per1 = new LocalDateInterval(perioder.get(i).getFom(), perioder.get(i).getTom());
                var per2 = new LocalDateInterval(perioder.get(j).getFom(), perioder.get(j).getTom());
                if (per1.overlaps(per2)) {
                    throw new IllegalArgumentException(per1 + " overlapper med " + per2);
                }
            }
        }
    }

    public EndringsresultatSnapshot finnAktivAggregatId(Long behandlingId) {
        Optional<Long> funnetId = ytelsesFordelingRepository.hentIdPåAktivYtelsesFordeling(behandlingId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(YtelseFordelingAggregat.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(YtelseFordelingAggregat.class));
    }

    public DiffResult diffResultat(EndringsresultatDiff idDiff, boolean kunSporedeEndringer) {
        Objects.requireNonNull(idDiff.getGrunnlagId1(), "kan ikke diffe når id1 ikke er oppgitt");
        Objects.requireNonNull(idDiff.getGrunnlagId2(), "kan ikke diffe når id2 ikke er oppgitt");

        return ytelsesFordelingRepository.diffResultat((Long)idDiff.getGrunnlagId1(), (Long)idDiff.getGrunnlagId2(), kunSporedeEndringer);
    }

    public void aksjonspunktAvklarStartdatoForPerioden(Long behandlingId, BekreftStartdatoForPerioden adapter) {
        new BekreftStartdatoForPeriodenAksjonspunkt(ytelsesFordelingRepository).oppdater(behandlingId, adapter);
    }

    public void bekreftAnnenforelderHarRett(Long behandlingId, Boolean annenforelderHarRett) {
        PerioderAnnenforelderHarRettEntitet perioderAnnenforelderHarRettEntitet = new PerioderAnnenforelderHarRettEntitet();
        if(Boolean.FALSE.equals(annenforelderHarRett)) {
            ytelsesFordelingRepository.lagre(behandlingId, perioderAnnenforelderHarRettEntitet);
        } else {
            // Legger inn en dummy periode for å indikere saksbehandlers valg. Inntil vi faktisk har perioder her
            perioderAnnenforelderHarRettEntitet.leggTil(new PeriodeAnnenforelderHarRettEntitet(LocalDate.now(), LocalDate.now()));
            ytelsesFordelingRepository.lagre(behandlingId, new PerioderAnnenforelderHarRettEntitet(perioderAnnenforelderHarRettEntitet));
        }
    }
}
