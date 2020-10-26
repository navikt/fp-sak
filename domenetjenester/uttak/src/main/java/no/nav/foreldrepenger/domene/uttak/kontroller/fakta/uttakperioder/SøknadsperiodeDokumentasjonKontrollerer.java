package no.nav.foreldrepenger.domene.uttak.kontroller.fakta.uttakperioder;

import static java.util.stream.Collectors.toList;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FEDREKVOTE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FELLESPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType.FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType.PERIODE_IKKE_VURDERT;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType.PERIODE_OK;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.ARBEID;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.FERIE;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.Årsak;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.IntervalUtils;
import no.nav.foreldrepenger.domene.uttak.kontroller.fakta.KontrollerFaktaUttakFeil;
import no.nav.vedtak.feil.FeilFactory;

final class SøknadsperiodeDokumentasjonKontrollerer {

    private final List<PeriodeUttakDokumentasjonEntitet> dokumentasjonPerioder;
    private final LocalDate fødselsDatoTilTidligOppstart;

    SøknadsperiodeDokumentasjonKontrollerer(List<PeriodeUttakDokumentasjonEntitet> dokumentasjonPerioder,
                                            LocalDate fødselsDatoTilTidligOppstart) {
        this.dokumentasjonPerioder = dokumentasjonPerioder;
        this.fødselsDatoTilTidligOppstart = fødselsDatoTilTidligOppstart;
    }

    static KontrollerFaktaData kontrollerPerioder(YtelseFordelingAggregat ytelseFordeling,
                                                  LocalDate fødselsDatoTilTidligOppstart) {
        List<PeriodeUttakDokumentasjonEntitet> dokumentasjonPerioder = hentDokumentasjonPerioder(ytelseFordeling);

        SøknadsperiodeDokumentasjonKontrollerer kontrollerer = new SøknadsperiodeDokumentasjonKontrollerer(dokumentasjonPerioder,
            fødselsDatoTilTidligOppstart);
        return kontrollerer.kontrollerSøknadsperioder(ytelseFordeling.getGjeldendeSøknadsperioder().getOppgittePerioder());
    }

    private static List<PeriodeUttakDokumentasjonEntitet> hentDokumentasjonPerioder(YtelseFordelingAggregat ytelseFordeling) {
        return ytelseFordeling.getPerioderUttakDokumentasjon()
            .map(PerioderUttakDokumentasjonEntitet::getPerioder).orElse(Collections.emptyList());
    }

    private KontrollerFaktaData kontrollerSøknadsperioder(List<OppgittPeriodeEntitet> søknadsperioder) {
        List<KontrollerFaktaPeriode> perioder = søknadsperioder.stream()
            .map(this::kontrollerSøknadsperiode)
            .collect(toList());

        return new KontrollerFaktaData(perioder);
    }

    KontrollerFaktaPeriode kontrollerSøknadsperiode(OppgittPeriodeEntitet søknadsperiode) {
        List<PeriodeUttakDokumentasjonEntitet> eksisterendeDokumentasjon = finnDokumentasjon(søknadsperiode.getFom(), søknadsperiode.getTom());

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
            throw KontrollerFaktaUttakFeil.FACTORY.dokumentertUtenBegrunnelse().toException();
        }

        if (søknadsperiode.erUtsettelse()) {
            return kontrollerUtsettelse(søknadsperiode);
        }
        if (søknadsperiode.erOverføring()) {
            return kontrollerOverføring(søknadsperiode);
        }
        if (erGyldigGrunnForTidligOppstart(søknadsperiode)) {
            return KontrollerFaktaPeriode.ubekreftetTidligOppstart(søknadsperiode);
        }
        if (søknadsperiode.erGradert()) {
            validerSøknadsperiodeGraderingOrdinærtArbeidsforhold(søknadsperiode);
        }
        return KontrollerFaktaPeriode.automatiskBekreftet(søknadsperiode, PERIODE_OK);
    }

    private boolean erPeriodenAvklartAvSaksbehandler(OppgittPeriodeEntitet søknadsperiode) {
        return !PERIODE_IKKE_VURDERT.equals(søknadsperiode.getPeriodeVurderingType());
    }

    private List<PeriodeUttakDokumentasjonEntitet> finnDokumentasjon(LocalDate fom, LocalDate tom) {
        IntervalUtils søknadPeriode = new IntervalUtils(fom, tom);
        List<PeriodeUttakDokumentasjonEntitet> resultat = new ArrayList<>();

        for (PeriodeUttakDokumentasjonEntitet dokumentasjon : dokumentasjonPerioder) {
            DatoIntervallEntitet dokumentasjonPeriode = dokumentasjon.getPeriode();
            if (søknadPeriode.overlapper(dokumentasjonPeriode)) {
                resultat.add(dokumentasjon);
            }
        }
        return resultat;
    }

    private void validerSøknadsperiodeGraderingOrdinærtArbeidsforhold(OppgittPeriodeEntitet søknadsperiode) {
        if (søknadsperiode.getArbeidsgiver() == null && søknadsperiode.getErArbeidstaker()) {
            throw FeilFactory.create(KontrollerFaktaUttakFeil.class).søktGraderingUtenArbeidsgiver(søknadsperiode.getPeriodeType().getKode(),
                søknadsperiode.getFom(), søknadsperiode.getTom()).toException();
        }
    }

    private KontrollerFaktaPeriode kontrollerOverføring(OppgittPeriodeEntitet søknadsperiode) {
        return KontrollerFaktaPeriode.ubekreftet(søknadsperiode);
    }

    private boolean erGyldigGrunnForTidligOppstart(OppgittPeriodeEntitet søknadsperiode) {
        // Søker far/medmor uttak av Fellesperiode eller fedrekvote eller foreldrepenger før uke 7 ved fødsel?
        //unntak uttak av flerbarnsdager
        if (fødselsDatoTilTidligOppstart != null
            && !søknadsperiode.isFlerbarnsdager()
            && søknadsperiode.getFom().isBefore(fødselsDatoTilTidligOppstart.plusWeeks(6L))) {
            return FELLESPERIODE.equals(søknadsperiode.getPeriodeType()) || FEDREKVOTE.equals(søknadsperiode.getPeriodeType()) || FORELDREPENGER.equals(søknadsperiode.getPeriodeType());
        }
        return false;
    }

    private KontrollerFaktaPeriode kontrollerUtsettelse(OppgittPeriodeEntitet søknadsperiode) {
        Årsak utsettelseÅrsak = søknadsperiode.getÅrsak();
        if (ARBEID.equals(utsettelseÅrsak)) {
            return kontrollerUtsettelseArbeid(søknadsperiode);
        }
        if (FERIE.equals(utsettelseÅrsak)) {
            return kontrollerUtsettelseFerie(søknadsperiode);
        }
        return KontrollerFaktaPeriode.ubekreftet(søknadsperiode);
    }

    private KontrollerFaktaPeriode kontrollerUtsettelseFerie(OppgittPeriodeEntitet søknadsperiode) {
        return KontrollerFaktaPeriode.automatiskBekreftet(søknadsperiode, PERIODE_OK);
    }

    private KontrollerFaktaPeriode kontrollerUtsettelseArbeid(OppgittPeriodeEntitet søknadsperiode) {
        return KontrollerFaktaPeriode.automatiskBekreftet(søknadsperiode, PERIODE_OK);
    }
}
