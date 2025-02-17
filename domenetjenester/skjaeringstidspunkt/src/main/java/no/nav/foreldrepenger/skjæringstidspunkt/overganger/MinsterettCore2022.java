package no.nav.foreldrepenger.skjæringstidspunkt.overganger;

import java.time.LocalDate;
import java.time.Month;
import java.time.Period;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.domene.tid.VirkedagUtil;

public class MinsterettCore2022 {

    public static final LocalDate IKRAFT_FRA_DATO = LocalDate.of(2022, Month.AUGUST,2); // LA STÅ.

    public static final boolean DEFAULT_SAK_UTEN_MINSTERETT = true;

    private static final Period FAR_TIDLIGSTE_UTTAK_FØR_TERMIN = Period.ofWeeks(2); // Vi har ikke uttak-konfig tilgjengelig her.

    private final LocalDate ikrafttredelseDato;

    public MinsterettCore2022() {
        this(IKRAFT_FRA_DATO);
    }

    MinsterettCore2022(LocalDate ikrafttredelseDato) {
        this.ikrafttredelseDato = ikrafttredelseDato;
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
        if (!utenMinsterett && gjeldendeFH.getGjelderFødsel() && !RelasjonsRolleType.MORA.equals(rolle) && førsteUttaksdato != null) {
            // Juster førsteUttaksdato som er mer enn 2 uker før fødsel / termin til tidligste lovlige dato
            var tidligsteFamilieHendelseDato = tidligsteFamilieHendelseDato(familieHendelseGrunnlag)
                .map(t -> t.minus(FAR_TIDLIGSTE_UTTAK_FØR_TERMIN));
            var uttaksdato = tidligsteFamilieHendelseDato.filter(førsteUttaksdato::isBefore).orElse(førsteUttaksdato);
            return VirkedagUtil.fomVirkedag(uttaksdato);
        }
        // 14-10 første ledd (mor), andre ledd. Eldre tilfelle
        return UtsettelseCore2021.førsteUttaksDatoForBeregning(rolle, familieHendelseGrunnlag, førsteUttaksdato);
    }

    private static Optional<LocalDate> tidligsteFamilieHendelseDato(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        var termindato = familieHendelseGrunnlag.getGjeldendeTerminbekreftelse()
            .map(TerminbekreftelseEntitet::getTermindato);
        return familieHendelseGrunnlag.getGjeldendeVersjon().getFødselsdato()
            .filter(f -> termindato.isEmpty() || f.isBefore(termindato.get()))
            .or(() -> termindato);
    }

}
