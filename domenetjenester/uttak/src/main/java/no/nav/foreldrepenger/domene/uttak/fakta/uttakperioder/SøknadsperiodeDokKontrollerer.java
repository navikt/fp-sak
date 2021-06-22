package no.nav.foreldrepenger.domene.uttak.fakta.uttakperioder;

import static java.util.stream.Collectors.toList;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FEDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType.PERIODE_IKKE_VURDERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType.PERIODE_OK;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.uttak.fakta.KontrollerFaktaUttakFeil;

final class SøknadsperiodeDokKontrollerer {

    private final List<PeriodeUttakDokumentasjonEntitet> dokumentasjonPerioder;
    private final LocalDate fødselsDatoTilTidligOppstart;
    private final UtsettelseDokKontrollerer utsettelseDokKontrollerer;

    SøknadsperiodeDokKontrollerer(List<PeriodeUttakDokumentasjonEntitet> dokumentasjonPerioder,
                                  LocalDate fødselsDatoTilTidligOppstart,
                                  UtsettelseDokKontrollerer utsettelseDokKontrollerer) {
        this.dokumentasjonPerioder = dokumentasjonPerioder;
        this.fødselsDatoTilTidligOppstart = fødselsDatoTilTidligOppstart;
        this.utsettelseDokKontrollerer = utsettelseDokKontrollerer;
    }

    static KontrollerFaktaData kontrollerPerioder(YtelseFordelingAggregat ytelseFordeling,
                                                  LocalDate fødselsDatoTilTidligOppstart,
                                                  UtsettelseDokKontrollerer utsettelseDokKontrollerer) {
        var dokumentasjonPerioder = hentDokumentasjonPerioder(ytelseFordeling);

        var kontrollerer = new SøknadsperiodeDokKontrollerer(dokumentasjonPerioder,
            fødselsDatoTilTidligOppstart, utsettelseDokKontrollerer);
        return kontrollerer.kontrollerSøknadsperioder(
            ytelseFordeling.getGjeldendeSøknadsperioder().getOppgittePerioder());
    }

    private static List<PeriodeUttakDokumentasjonEntitet> hentDokumentasjonPerioder(YtelseFordelingAggregat ytelseFordeling) {
        return ytelseFordeling.getPerioderUttakDokumentasjon()
            .map(PerioderUttakDokumentasjonEntitet::getPerioder)
            .orElse(Collections.emptyList());
    }

    private KontrollerFaktaData kontrollerSøknadsperioder(List<OppgittPeriodeEntitet> søknadsperioder) {
        var perioder = søknadsperioder.stream().map(this::kontrollerSøknadsperiode).collect(toList());
        return new KontrollerFaktaData(perioder);
    }

    KontrollerFaktaPeriode kontrollerSøknadsperiode(OppgittPeriodeEntitet søknadsperiode) {
        var eksisterendeDokumentasjon = finnDokumentasjon(søknadsperiode.getFom(), søknadsperiode.getTom());

        if (erPeriodenAvklartAvSaksbehandler(søknadsperiode)) {
            return kontrollerAvklartPeriode(søknadsperiode, eksisterendeDokumentasjon);
        }

        return kontrollerUavklartPeriode(søknadsperiode, eksisterendeDokumentasjon);
    }

    private KontrollerFaktaPeriode kontrollerAvklartPeriode(OppgittPeriodeEntitet søknadsperiode,
                                                            List<PeriodeUttakDokumentasjonEntitet> eksisterendeDokumentasjon) {
        return KontrollerFaktaPeriode.manueltAvklart(søknadsperiode, eksisterendeDokumentasjon);
    }

    private KontrollerFaktaPeriode kontrollerUavklartPeriode(OppgittPeriodeEntitet søknadsperiode,
                                                             List<PeriodeUttakDokumentasjonEntitet> eksisterendeDokumentasjon) {
        if (!eksisterendeDokumentasjon.isEmpty()) {
            throw KontrollerFaktaUttakFeil.dokumentertUtenBegrunnelse();
        }

        if (søknadsperiode.isUtsettelse()) {
            return kontrollerUtsettelse(søknadsperiode);
        }
        if (søknadsperiode.isOverføring()) {
            return kontrollerOverføring(søknadsperiode);
        }
        if (erGyldigGrunnForTidligOppstart(søknadsperiode)) {
            return KontrollerFaktaPeriode.ubekreftetTidligOppstart(søknadsperiode);
        }
        if (søknadsperiode.isGradert()) {
            validerSøknadsperiodeGraderingOrdinærtArbeidsforhold(søknadsperiode);
        }
        return KontrollerFaktaPeriode.automatiskBekreftet(søknadsperiode, PERIODE_OK);
    }

    private boolean erPeriodenAvklartAvSaksbehandler(OppgittPeriodeEntitet søknadsperiode) {
        return !PERIODE_IKKE_VURDERT.equals(søknadsperiode.getPeriodeVurderingType());
    }

    private List<PeriodeUttakDokumentasjonEntitet> finnDokumentasjon(LocalDate fom, LocalDate tom) {
        var søknadPeriode = new SimpleLocalDateInterval(fom, tom);
        List<PeriodeUttakDokumentasjonEntitet> resultat = new ArrayList<>();

        for (var dokumentasjon : dokumentasjonPerioder) {
            var dokumentasjonPeriode = dokumentasjon.getPeriode();
            if (søknadPeriode.overlapper(dokumentasjonPeriode)) {
                resultat.add(dokumentasjon);
            }
        }
        return resultat;
    }

    private void validerSøknadsperiodeGraderingOrdinærtArbeidsforhold(OppgittPeriodeEntitet søknadsperiode) {
        if (søknadsperiode.getArbeidsgiver() == null && søknadsperiode.isArbeidstaker()) {
            throw KontrollerFaktaUttakFeil.søktGraderingUtenArbeidsgiver(søknadsperiode.getPeriodeType().getKode(),
                søknadsperiode.getFom(), søknadsperiode.getTom());
        }
    }

    private KontrollerFaktaPeriode kontrollerOverføring(OppgittPeriodeEntitet søknadsperiode) {
        return KontrollerFaktaPeriode.ubekreftet(søknadsperiode);
    }

    private boolean erGyldigGrunnForTidligOppstart(OppgittPeriodeEntitet søknadsperiode) {
        // Søker far/medmor uttak av Fellesperiode eller fedrekvote eller foreldrepenger før uke 7 ved fødsel?
        //unntak uttak av flerbarnsdager
        if (fødselsDatoTilTidligOppstart != null && !søknadsperiode.isFlerbarnsdager() && søknadsperiode.getFom()
            .isBefore(fødselsDatoTilTidligOppstart.plusWeeks(6L))) {
            return FELLESPERIODE.equals(søknadsperiode.getPeriodeType()) || FEDREKVOTE.equals(
                søknadsperiode.getPeriodeType()) || FORELDREPENGER.equals(søknadsperiode.getPeriodeType());
        }
        return false;
    }

    private KontrollerFaktaPeriode kontrollerUtsettelse(OppgittPeriodeEntitet søknadsperiode) {
        return utsettelseDokKontrollerer.måSaksbehandlerManueltBekrefte(søknadsperiode) ?
            KontrollerFaktaPeriode.ubekreftet(søknadsperiode) : KontrollerFaktaPeriode.automatiskBekreftet(søknadsperiode, PERIODE_OK);
    }
}
