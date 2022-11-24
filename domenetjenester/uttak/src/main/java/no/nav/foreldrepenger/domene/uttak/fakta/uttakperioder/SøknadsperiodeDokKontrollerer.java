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
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerInnleggelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.GraderingAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeFarRundtFødsel;
import no.nav.foreldrepenger.domene.uttak.fakta.KontrollerFaktaUttakFeil;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.fpsak.tidsserie.LocalDateInterval;

final class SøknadsperiodeDokKontrollerer {

    private final List<PeriodeUttakDokumentasjonEntitet> dokumentasjonPerioder;
    private final LocalDate fødselsDatoTilTidligOppstart; // Kun satt dersom far/medmor og termin/fødsel
    private final UtsettelseDokKontrollerer utsettelseDokKontrollerer;
    private final List<PleiepengerInnleggelseEntitet> pleiepengerInnleggelser;
    private final Optional<LocalDateInterval> farUttakRundtFødsel;

    SøknadsperiodeDokKontrollerer(List<PeriodeUttakDokumentasjonEntitet> dokumentasjonPerioder,
                                  LocalDate fødselsDatoTilTidligOppstart,
                                  UtsettelseDokKontrollerer utsettelseDokKontrollerer,
                                  List<PleiepengerInnleggelseEntitet> pleiepengerInnleggelser,
                                  Optional<LocalDateInterval> farUttakRundtFødsel) {
        this.dokumentasjonPerioder = dokumentasjonPerioder;
        this.fødselsDatoTilTidligOppstart = fødselsDatoTilTidligOppstart;
        this.utsettelseDokKontrollerer = utsettelseDokKontrollerer;
        this.pleiepengerInnleggelser = pleiepengerInnleggelser;
        this.farUttakRundtFødsel = farUttakRundtFødsel;
    }

    SøknadsperiodeDokKontrollerer(List<PeriodeUttakDokumentasjonEntitet> dokumentasjonPerioder,
                                  LocalDate fødselsDatoTilTidligOppstart,
                                  UtsettelseDokKontrollerer utsettelseDokKontrollerer) {
        this(dokumentasjonPerioder, fødselsDatoTilTidligOppstart, utsettelseDokKontrollerer, List.of(), Optional.empty());
    }

    static KontrollerFaktaData kontrollerPerioder(YtelseFordelingAggregat ytelseFordeling,
                                                  LocalDate fødselsDatoTilTidligOppstart,
                                                  UttakInput uttakInput) {
        var dokumentasjonPerioder = hentDokumentasjonPerioder(ytelseFordeling);
        var farUttakRundtFødsel = TidsperiodeFarRundtFødsel.intervallFarRundtFødsel(uttakInput);
        var kontrollerer = new SøknadsperiodeDokKontrollerer(dokumentasjonPerioder,
            fødselsDatoTilTidligOppstart, utledUtsettelseKontrollerer(uttakInput),
            finnPerioderMedPleiepengerInnleggelse(uttakInput), farUttakRundtFødsel);
        return kontrollerer.kontrollerSøknadsperioder(
            ytelseFordeling.getGjeldendeFordeling().getPerioder());
    }

    private static List<PleiepengerInnleggelseEntitet> finnPerioderMedPleiepengerInnleggelse(UttakInput input) {
        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = input.getYtelsespesifiktGrunnlag();
        var pleiepengerGrunnlag = ytelsespesifiktGrunnlag.getPleiepengerGrunnlag();
        if (pleiepengerGrunnlag.isPresent()) {
            var perioderMedInnleggelse = pleiepengerGrunnlag.get().getPerioderMedInnleggelse();
            if (perioderMedInnleggelse.isPresent()) {
                return perioderMedInnleggelse.get().getInnleggelser();
            }
        }
        return List.of();
    }

    private static UtsettelseDokKontrollerer utledUtsettelseKontrollerer(UttakInput input) {
        var stp = input.getBehandlingReferanse().getSkjæringstidspunkt();
        return stp.kreverSammenhengendeUttak() ? new UtsettelseDokKontrollererSammenhengendeUttak()
            : new UtsettelseDokKontrollererFrittUttak(finnGjeldendeFamiliehendelse(input));
    }

    private static LocalDate finnGjeldendeFamiliehendelse(UttakInput input) {
        ForeldrepengerGrunnlag ytelsespesifiktGrunnlag = input.getYtelsespesifiktGrunnlag();
        var gjeldendeFamilieHendelse = ytelsespesifiktGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        return gjeldendeFamilieHendelse.getFamilieHendelseDato();
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

        if (erPeriodenAvklartAvSaksbehandler(søknadsperiode, eksisterendeDokumentasjon)) {
            return kontrollerAvklartPeriode(søknadsperiode, eksisterendeDokumentasjon);
        }
        if (erAvklartAvVedtakOmPleiepenger(søknadsperiode)) {
            return KontrollerFaktaPeriode.automatiskBekreftet(søknadsperiode, PERIODE_OK);
        }

        return kontrollerUavklartPeriode(søknadsperiode, eksisterendeDokumentasjon);
    }

    private boolean erAvklartAvVedtakOmPleiepenger(OppgittPeriodeEntitet søknadsperiode) {
        if (!UtsettelseÅrsak.INSTITUSJON_BARN.equals(søknadsperiode.getÅrsak())) {
            return false;
        }
        return pleiepengerInnleggelser.stream()
            .anyMatch(i -> søknadsperiode.getTidsperiode().erOmsluttetAv(i.getPeriode()));
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
        if (farUttakRundtFødsel.isPresent() && erBalansertUttakRundtFødsel(søknadsperiode)) {
            return KontrollerFaktaPeriode.automatiskBekreftet(søknadsperiode, PERIODE_OK);
        }
        if (erGyldigGrunnForTidligOppstart(søknadsperiode)) {
            return KontrollerFaktaPeriode.ubekreftetTidligOppstart(søknadsperiode);
        }
        if (søknadsperiode.isGradert()) {
            validerSøknadsperiodeGraderingOrdinærtArbeidsforhold(søknadsperiode);
        }
        return KontrollerFaktaPeriode.automatiskBekreftet(søknadsperiode, PERIODE_OK);
    }

    private boolean erPeriodenAvklartAvSaksbehandler(OppgittPeriodeEntitet søknadsperiode,
                                                     List<PeriodeUttakDokumentasjonEntitet> eksisterendeDokumentasjon) {
        if (!eksisterendeDokumentasjon.isEmpty()) {
            return true;
        }
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
        if (søknadsperiode.getArbeidsgiver() == null && søknadsperiode.getGraderingAktivitetType() == GraderingAktivitetType.ARBEID) {
            throw KontrollerFaktaUttakFeil.søktGraderingUtenArbeidsgiver(søknadsperiode.getPeriodeType().getKode(),
                søknadsperiode.getFom(), søknadsperiode.getTom());
        }
    }

    private KontrollerFaktaPeriode kontrollerOverføring(OppgittPeriodeEntitet søknadsperiode) {
        return KontrollerFaktaPeriode.ubekreftet(søknadsperiode);
    }

    private boolean erBalansertUttakRundtFødsel(OppgittPeriodeEntitet søknadsperiode) {
        if (farMedmorUtenomFlerbarnsdager(søknadsperiode)) {
            // FAB-direktiv - far/medmor kan ta ut "ifm" fødsel (før termin og første 6 uker).
            // FEDREKVOTE er begrenset til et antall dager - derfor sjekk på om periden er innenfor
            // FORELEDREPENGER kan tas ut ifm fødsel og videre dvs >6uker fom fødsel er ok. Derfor kun sjekk om periode/fom er innenfor intervall
            var fedrekvoteMedSamtidigUttak = FEDREKVOTE.equals(søknadsperiode.getPeriodeType()) && søknadsperiode.isSamtidigUttak() &&
                farUttakRundtFødsel.filter(p -> p.encloses(søknadsperiode.getFom()) && p.encloses(søknadsperiode.getTom())).isPresent();
            var foreldrepengerUtenomSykdom = FORELDREPENGER.equals(søknadsperiode.getPeriodeType()) &&
                farUttakRundtFødsel.filter(p -> p.encloses(søknadsperiode.getFom())).isPresent();
            return fedrekvoteMedSamtidigUttak || foreldrepengerUtenomSykdom;
        }
        return false;
    }

    private boolean erGyldigGrunnForTidligOppstart(OppgittPeriodeEntitet søknadsperiode) {
        // Søker far/medmor uttak av Fellesperiode eller fedrekvote eller foreldrepenger før uke 7 ved fødsel?
        //unntak uttak av flerbarnsdager
        if (farMedmorUtenomFlerbarnsdager(søknadsperiode) && søknadsperiode.getFom().isBefore(fødselsDatoTilTidligOppstart.plusWeeks(6L))) {
            return FELLESPERIODE.equals(søknadsperiode.getPeriodeType()) || FEDREKVOTE.equals(
                søknadsperiode.getPeriodeType()) || FORELDREPENGER.equals(søknadsperiode.getPeriodeType());
        }
        return false;
    }

    private boolean farMedmorUtenomFlerbarnsdager(OppgittPeriodeEntitet søknadsperiode) {
        return fødselsDatoTilTidligOppstart != null && !søknadsperiode.isFlerbarnsdager();
    }

    private KontrollerFaktaPeriode kontrollerUtsettelse(OppgittPeriodeEntitet søknadsperiode) {
        var manuell = utsettelseDokKontrollerer.måSaksbehandlerManueltBekrefte(søknadsperiode);
        return manuell ? KontrollerFaktaPeriode.ubekreftet(søknadsperiode) : KontrollerFaktaPeriode.automatiskBekreftet(søknadsperiode, PERIODE_OK);
    }
}
