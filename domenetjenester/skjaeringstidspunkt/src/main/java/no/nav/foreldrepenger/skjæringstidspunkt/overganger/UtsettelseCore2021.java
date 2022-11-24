package no.nav.foreldrepenger.skjæringstidspunkt.overganger;

import static no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak.SØKNADSFRIST;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

/*
 * OBSOBSOBS: Endelig ikrafttredelse dato og overgangs vedtas først i Statsråd, etter Stortingets behandling av Prop 127L20/21
 * Klasse for styring av ikrafttredelese nytt regelverk for uttak
 * Metode for å gi ikrafttredelsesdato avhengig av miljø
 * Metode for å vurdere om en Familiehendelse skal vurderes etter nye eller gamle regler. Vil bli oppdatert
 * TODO: Etter dato passert og overgang for testcases -> flytt til sentral konfigklasse - skal ikke lenger ha miljøavvik
 */
@ApplicationScoped
public class UtsettelseCore2021 {

    private static final Period SENESTE_UTTAK_FØR_TERMIN = Period.ofWeeks(3);
    public static final boolean DEFAULT_KREVER_SAMMENHENGENDE_UTTAK = false;

    private static final String PROP_NAME_DATO = "dato.for.nye.uttaksregler";
    private static final LocalDate DATO_FOR_PROD = LocalDate.of(2021,10,1); // LA STÅ.

    private LocalDate ikrafttredelseDato = DATO_FOR_PROD;

    UtsettelseCore2021() {
        // CDI
    }

    @Inject
    public UtsettelseCore2021(@KonfigVerdi(value = PROP_NAME_DATO, required = false) LocalDate ikrafttredelse) {
        // Pass på å ikke endre dato som skal brukes i produksjon før ting er vedtatt ...
        this.ikrafttredelseDato = (Environment.current().isProd() || ikrafttredelse == null) ? DATO_FOR_PROD : ikrafttredelse;
    }

    public boolean kreverSammenhengendeUttak(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        if (familieHendelseGrunnlag == null) return true;
        var bekreftetFamilieHendelse = familieHendelseGrunnlag.getGjeldendeBekreftetVersjon()
            .filter(fh -> !FamilieHendelseType.TERMIN.equals(fh.getType()));
        if (bekreftetFamilieHendelse.map(FamilieHendelseEntitet::getSkjæringstidspunkt).isPresent()) {
            return bekreftetFamilieHendelse.map(FamilieHendelseEntitet::getSkjæringstidspunkt).filter(hendelse -> hendelse.isBefore(ikrafttredelseDato)).isPresent();
        }
        var gjeldendeFH = familieHendelseGrunnlag.getGjeldendeVersjon();
        if (gjeldendeFH == null || gjeldendeFH.getSkjæringstidspunkt() == null) return true;
        if (gjeldendeFH.getSkjæringstidspunkt().isBefore(ikrafttredelseDato)) return true;
        return LocalDate.now().isBefore(ikrafttredelseDato);
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

    public static Optional<LocalDate> finnFørsteDatoFraUttakResultat(List<UttakResultatPeriodeEntitet> perioder, boolean kreverSammenhengendeUttak) {
        if (erAllePerioderAvslåttOgIngenAvslagPgaSøknadsfrist(perioder)) {
            // TODO: Håndtering av tomt Uttak (OBS på allmatch nedenfor). Dagens dato kan være like god som noen annen dato.
            return perioder.stream()
                .filter(p -> kreverSammenhengendeUttak || frittUttakErPeriodeMedUttak(p))
                .map(UttakResultatPeriodeEntitet::getFom)
                .min(Comparator.naturalOrder());
        }
        return perioder.stream()
            .filter(it -> it.isInnvilget() || SØKNADSFRIST.equals(it.getResultatÅrsak()))
            .filter(p -> kreverSammenhengendeUttak || frittUttakErPeriodeMedUttak(p))
            .map(UttakResultatPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder());
    }

    public static Optional<LocalDate> finnSisteDatoFraUttakResultat(List<UttakResultatPeriodeEntitet> perioder, boolean kreverSammenhengendeUttak) {
        return perioder.stream()
            .filter(UttakResultatPeriodeEntitet::isInnvilget)
            .filter(it -> kreverSammenhengendeUttak || frittUttakErPeriodeMedUttak(it))
            .map(UttakResultatPeriodeEntitet::getTom)
            .max(Comparator.naturalOrder());
    }

    public static Optional<LocalDate> finnFørsteDatoFraSøknad(Optional<OppgittFordelingEntitet> oppgittFordeling, boolean kreverSammenhengendeUttak) {
        return oppgittFordeling.map(OppgittFordelingEntitet::getPerioder).orElse(Collections.emptyList()).stream()
            .filter(p -> kreverSammenhengendeUttak || frittUttakErPeriodeMedUttak(p))
            .map(OppgittPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder());
    }

    public static Optional<LocalDate> finnFørsteUtsettelseDatoFraSøknad(Optional<OppgittFordelingEntitet> oppgittFordeling, boolean kreverSammenhengendeUttak) {
        return oppgittFordeling.map(OppgittFordelingEntitet::getPerioder).orElse(Collections.emptyList()).stream()
            .filter(p -> !kreverSammenhengendeUttak && !frittUttakErPeriodeMedUttak(p))
            .map(OppgittPeriodeEntitet::getFom)
            .min(Comparator.naturalOrder());
    }

    public static Optional<LocalDate> finnSisteDatoFraSøknad(Optional<OppgittFordelingEntitet> oppgittFordeling, boolean kreverSammenhengendeUttak) {
        return oppgittFordeling.map(OppgittFordelingEntitet::getPerioder).orElse(Collections.emptyList()).stream()
            .filter(p -> kreverSammenhengendeUttak || frittUttakErPeriodeMedUttak(p))
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
