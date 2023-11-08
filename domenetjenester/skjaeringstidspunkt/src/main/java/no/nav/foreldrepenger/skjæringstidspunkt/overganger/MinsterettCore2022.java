package no.nav.foreldrepenger.skjæringstidspunkt.overganger;

import java.time.LocalDate;
import java.time.Period;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

/*
 * OBSOBSOBS: Endelig ikrafttredelse dato og overgangs vedtas først i Statsråd, etter Stortingets behandling av Prop 15L21/22
 * Klasse for styring av ikrafttredelese nytt regelverk for minsterett og uttak ifm fødsel
 * Metode for å gi ikrafttredelsesdato avhengig av miljø
 * Metode for å vurdere om en Familiehendelse skal vurderes etter nye eller gamle regler. Vil bli oppdatert
 * TODO: Etter dato passert og overgang for testcases -> flytt til sentral konfigklasse - skal ikke lenger ha miljøavvik
 */
@ApplicationScoped
public class MinsterettCore2022 {

    private static final String PROP_NAME_DATO = "dato.for.minsterett.forste";
    private static final LocalDate DATO_FOR_PROD = LocalDate.of(2022,8,2); // LA STÅ.

    public static final boolean DEFAULT_SAK_UTEN_MINSTERETT = true;

    private static final Period FAR_TIDLIGSTE_UTTAK_FØR_TERMIN = Period.ofWeeks(2); // Vi har ikke uttak-konfig tilgjengelig her.

    private LocalDate ikrafttredelseDato = DATO_FOR_PROD;

    MinsterettCore2022() {
        // CDI
    }

    @Inject
    public MinsterettCore2022(@KonfigVerdi(value = PROP_NAME_DATO, required = false) LocalDate ikrafttredelse) {
        // Pass på å ikke endre dato som skal brukes i produksjon før ting er vedtatt ...
        this.ikrafttredelseDato = Environment.current().isProd() || ikrafttredelse == null ? DATO_FOR_PROD : ikrafttredelse;
    }

    public boolean utenMinsterett(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
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

    public static LocalDate førsteUttaksDatoForBeregning(RelasjonsRolleType rolle, FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag,
                                                         LocalDate førsteUttaksdato, boolean utenMinsterett) {
        // // 14-10 første ledd (far+medmor) med uttak ifm fødsel iht P15L 2021/22. Øvrige tilfelle sendes videre.
        var gjeldendeFH = familieHendelseGrunnlag.getGjeldendeVersjon();
        if (!utenMinsterett && gjeldendeFH.getGjelderFødsel() && !RelasjonsRolleType.MORA.equals(rolle)) {
            // Juster førsteUttaksdato som er før fødsel eller T-2 til tidligste lovlige dato
            var termindatoMinusPeriode = familieHendelseGrunnlag.getGjeldendeTerminbekreftelse()
                .map(TerminbekreftelseEntitet::getTermindato)
                .map(t -> t.minus(FAR_TIDLIGSTE_UTTAK_FØR_TERMIN));
            var tidligstedato = gjeldendeFH.getFødselsdato()
                .filter(f -> termindatoMinusPeriode.isEmpty() || f.isBefore(termindatoMinusPeriode.get()))
                .or(() -> termindatoMinusPeriode);
            var uttaksdato = tidligstedato.filter(førsteUttaksdato::isBefore).orElse(førsteUttaksdato);
            return VirkedagUtil.fomVirkedag(uttaksdato);
        }
        // 14-10 første ledd (mor), andre ledd. Eldre tilfelle
        return UtsettelseCore2021.førsteUttaksDatoForBeregning(rolle, familieHendelseGrunnlag, førsteUttaksdato);
    }

}
