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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.pleiepenger.PleiepengerInnleggelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PerioderUttakDokumentasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.uttak.TidsperiodeFarRundtFødsel;
import no.nav.foreldrepenger.domene.uttak.fakta.KontrollerFaktaUttakFeil;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.fpsak.tidsserie.LocalDateInterval;

final class SøknadsperiodeDokKontrollerer {

    private static final Logger LOG = LoggerFactory.getLogger(SøknadsperiodeDokKontrollerer.class);

    private final List<PeriodeUttakDokumentasjonEntitet> dokumentasjonPerioder;
    private final LocalDate fødselsDatoTilTidligOppstart; // Kun satt dersom far/medmor og termin/fødsel
    private final UtsettelseDokKontrollerer utsettelseDokKontrollerer;
    private final List<PleiepengerInnleggelseEntitet> pleiepengerInnleggelser;
    private final Optional<LocalDateInterval> farUttakRundtFødsel;
    private boolean logg = false;

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
                                                  UttakInput uttakInput, boolean logg) {
        var dokumentasjonPerioder = hentDokumentasjonPerioder(ytelseFordeling);
        var farUttakRundtFødsel = TidsperiodeFarRundtFødsel.intervallFarRundtFødsel(uttakInput);
        var kontrollerer = new SøknadsperiodeDokKontrollerer(dokumentasjonPerioder,
            fødselsDatoTilTidligOppstart, utledUtsettelseKontrollerer(uttakInput),
            finnPerioderMedPleiepengerInnleggelse(uttakInput), farUttakRundtFødsel);
        kontrollerer.logg = logg;
        return kontrollerer.kontrollerSøknadsperioder(
            ytelseFordeling.getGjeldendeSøknadsperioder().getOppgittePerioder());
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

        if (erPeriodenAvklartAvSaksbehandler(søknadsperiode)) {
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
            if (logg) {
                LOG.info("FAKTA UTTAK kontroller - ubekreftet periode tidligstart");
            }
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
        try {
            if (logg) LOG.info("FAKTA UTTAK kontroller - ubekreftet periode overføring {}", søknadsperiode.getÅrsak().getKode());
        } catch (Exception e) {
            //
        }

        return KontrollerFaktaPeriode.ubekreftet(søknadsperiode);
    }

    private boolean erBalansertUttakRundtFødsel(OppgittPeriodeEntitet søknadsperiode) {
        if (farMedmorUtenomFlerbarnsdager(søknadsperiode)) {
            var fedrekvoteMedSamtidigUttak = FEDREKVOTE.equals(søknadsperiode.getPeriodeType()) && søknadsperiode.isSamtidigUttak();
            var foreldrepengerUtenomSykdom = FORELDREPENGER.equals(søknadsperiode.getPeriodeType()) &&
                !Set.of(MorsAktivitet.TRENGER_HJELP, MorsAktivitet.INNLAGT).contains(søknadsperiode.getMorsAktivitet());
            var periodeKanAvklaresAutomatisk = fedrekvoteMedSamtidigUttak || foreldrepengerUtenomSykdom;
            // FAB-direktiv - søknadsperioden er helt innenfor periode rundt fødsel der far/medmor kan ta ut
            return periodeKanAvklaresAutomatisk && farUttakRundtFødsel.filter(p -> p.encloses(søknadsperiode.getFom()) && p.encloses(søknadsperiode.getTom())).isPresent();
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
        if (manuell) {
            try {
                if (logg) LOG.info("FAKTA UTTAK kontroller - ubekreftet periode utsettelse {}", søknadsperiode.getÅrsak().getKode());
            } catch (Exception e) {
                //
            }

        }
        return manuell ? KontrollerFaktaPeriode.ubekreftet(søknadsperiode) : KontrollerFaktaPeriode.automatiskBekreftet(søknadsperiode, PERIODE_OK);
    }
}
