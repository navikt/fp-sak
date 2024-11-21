package no.nav.foreldrepenger.skjæringstidspunkt.overganger;

import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.SØKNADSFRIST;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;

public class UtsettelseCore2021 {

    private static final Period SENESTE_UTTAK_FØR_TERMIN = Period.ofWeeks(3);

    public static final LocalDate IKRAFT_FRA_DATO = LocalDate.of(2021, Month.OCTOBER,1); // LA STÅ.

    private UtsettelseCore2021() {
    }

    // Input til uttaksregler
    public static LocalDate kreverSammenhengendeUttakTilOgMed() {
        return IKRAFT_FRA_DATO.minusDays(1);
    }

    public static boolean kreverSammenhengendeUttak(OppgittPeriodeEntitet oppgittPeriode) {
        return oppgittPeriode.getFom().isBefore(IKRAFT_FRA_DATO);
    }

    public static boolean kreverSammenhengendeUttak(UttakResultatPeriodeEntitet uttakResultatPeriode) {
        return uttakResultatPeriode.getFom().isBefore(IKRAFT_FRA_DATO);
    }

    public static boolean kreverSammenhengendeUttak(LocalDate skjæringstidspunkt) {
        return skjæringstidspunkt.isBefore(IKRAFT_FRA_DATO);
    }

    public static boolean kreverSammenhengendeUttakMorsMaxdato(LocalDate morsMaxdato) {
        return morsMaxdato.isBefore(IKRAFT_FRA_DATO.minusDays(1));
    }

    public static boolean kreverSammenhengendeUttak(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        if (familieHendelseGrunnlag == null) return true;
        var bekreftetFamilieHendelse = familieHendelseGrunnlag.getGjeldendeBekreftetVersjon()
            .filter(fh -> !FamilieHendelseType.TERMIN.equals(fh.getType()));
        if (bekreftetFamilieHendelse.map(FamilieHendelseEntitet::getSkjæringstidspunkt).isPresent()) {
            return bekreftetFamilieHendelse.map(FamilieHendelseEntitet::getSkjæringstidspunkt).filter(hendelse -> hendelse.isBefore(IKRAFT_FRA_DATO)).isPresent();
        }
        var gjeldendeFH = familieHendelseGrunnlag.getGjeldendeVersjon();
        if (gjeldendeFH == null || gjeldendeFH.getSkjæringstidspunkt() == null) return true;
        if (gjeldendeFH.getSkjæringstidspunkt().isBefore(IKRAFT_FRA_DATO)) return true;
        return LocalDate.now().isBefore(IKRAFT_FRA_DATO);
    }

    public static LocalDate førsteUttaksDatoForBeregning(RelasjonsRolleType rolle, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag, LocalDate førsteUttaksdato) {
        // FAR/MEDMOR eller adopsjon skal ikke begynnne før familiehendelsedato
        var gjeldendeFH = familieHendelseGrunnlag.getGjeldendeVersjon();
        if (!gjeldendeFH.getGjelderFødsel() || !RelasjonsRolleType.MORA.equals(rolle)) {
            // 14-10 første ledd (far+medmor), andre ledd
            var uttaksdato = gjeldendeFH.getSkjæringstidspunkt() != null && førsteUttaksdato.isBefore(gjeldendeFH.getSkjæringstidspunkt()) ?
                gjeldendeFH.getSkjæringstidspunkt() : førsteUttaksdato;
            return VirkedagUtil.fomVirkedag(uttaksdato);
        }
        // 14-10 første ledd (mor) - settes til tidligst av (førstuttaksdato, termin-3, fødsel). Ingen sjekk på før T-12uker.
        var termindatoMinusPeriode = familieHendelseGrunnlag.getGjeldendeTerminbekreftelse()
            .map(TerminbekreftelseEntitet::getTermindato)
            .map(t -> t.minus(SENESTE_UTTAK_FØR_TERMIN));
        var fødselsdato = gjeldendeFH.getFødselsdato()
            .filter(f -> termindatoMinusPeriode.isEmpty() || f.isBefore(termindatoMinusPeriode.get()))
            .or(() -> termindatoMinusPeriode);
        var senesteStartdatoMor = fødselsdato.filter(førsteUttaksdato::isAfter).orElse(førsteUttaksdato);
        return VirkedagUtil.fomVirkedag(senesteStartdatoMor);
    }

    public static Optional<LocalDate> finnFørsteDatoFraUttakResultat(List<UttakResultatPeriodeEntitet> perioder) {
        if (erAllePerioderAvslåttOgIngenAvslagPgaSøknadsfrist(perioder)) {
            // TODO: Håndtering av tomt Uttak (OBS på allmatch nedenfor). Dagens dato kan være like god som noen annen dato.
            return perioder.stream()
                .filter(p -> kreverSammenhengendeUttak(p) || frittUttakErPeriodeMedUttak(p))
                .map(UttakResultatPeriodeEntitet::getFom)
                .min(Comparator.naturalOrder());
        }
        return perioder.stream()
            .filter(it -> it.isInnvilget() || SØKNADSFRIST.equals(it.getResultatÅrsak()))
            .filter(p -> kreverSammenhengendeUttak(p) || frittUttakErPeriodeMedUttak(p))
            .map(UttakResultatPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder());
    }

    public static Optional<LocalDate> finnSisteDatoFraUttakResultat(List<UttakResultatPeriodeEntitet> perioder) {
        return perioder.stream()
            .filter(UttakResultatPeriodeEntitet::isInnvilget)
            .filter(it -> kreverSammenhengendeUttak(it) || frittUttakErPeriodeMedUttak(it))
            .map(UttakResultatPeriodeEntitet::getTom)
            .max(Comparator.naturalOrder());
    }

    public static Optional<LocalDate> finnFørsteDatoFraSøknad(Optional<OppgittFordelingEntitet> oppgittFordeling) {
        return finnFørsteDatoFra(oppgittFordeling.map(OppgittFordelingEntitet::getPerioder).orElse(Collections.emptyList()));
    }

    public static Optional<LocalDate> finnFørsteDatoFra(List<OppgittPeriodeEntitet> perioder) {
        return finnFørsteOppgittPeriodeFra(perioder).map(OppgittPeriodeEntitet::getFom);
    }

    public static Optional<OppgittPeriodeEntitet> finnFørsteOppgittPeriodeFra(List<OppgittPeriodeEntitet> perioder) {
        return perioder.stream()
            .filter(p -> kreverSammenhengendeUttak(p) || frittUttakErPeriodeMedUttak(p))
            .min(Comparator.comparing(OppgittPeriodeEntitet::getFom));
    }

    public static Optional<LocalDate> finnFørsteUtsettelseDatoFraSøknad(Optional<OppgittFordelingEntitet> oppgittFordeling) {
        return oppgittFordeling.map(OppgittFordelingEntitet::getPerioder).orElse(Collections.emptyList()).stream()
            .filter(p -> !kreverSammenhengendeUttak(p) && !frittUttakErPeriodeMedUttak(p))
            .map(OppgittPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder());
    }

    public static Optional<LocalDate> finnSisteDatoFraSøknad(Optional<OppgittFordelingEntitet> oppgittFordeling) {
        return oppgittFordeling.map(OppgittFordelingEntitet::getPerioder).orElse(Collections.emptyList()).stream()
            .filter(p -> kreverSammenhengendeUttak(p) || frittUttakErPeriodeMedUttak(p))
            .map(OppgittPeriodeEntitet::getTom)
            .max(Comparator.naturalOrder());
    }

    private static boolean frittUttakErPeriodeMedUttak(UttakResultatPeriodeEntitet periode) {
        return SØKNADSFRIST.equals(periode.getResultatÅrsak()) ||
            periode.getAktiviteter().stream().anyMatch(a -> a.getUtbetalingsgrad().harUtbetaling() && a.getTrekkdager().merEnn0());
    }

    private static boolean frittUttakErPeriodeMedUttak(OppgittPeriodeEntitet periode) {
        return !(periode.isUtsettelse() || periode.isOpphold());
    }

    private static boolean erAllePerioderAvslåttOgIngenAvslagPgaSøknadsfrist(List<UttakResultatPeriodeEntitet> uttakResultatPerioder) {
        return uttakResultatPerioder.stream().allMatch(ut -> PeriodeResultatType.AVSLÅTT.equals(ut.getResultatType())
            && !SØKNADSFRIST.equals(ut.getResultatÅrsak()));
    }


}
