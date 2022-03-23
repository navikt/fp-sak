package no.nav.foreldrepenger.skjæringstidspunkt.overganger;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
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
    private static final LocalDate DATO_FOR_PROD = LocalDate.of(2999,8,2); // LA STÅ.

    public static final boolean DEFAULT_SAK_UTEN_MINSTERETT = true;

    private LocalDate ikrafttredelseDato = DATO_FOR_PROD;

    MinsterettCore2022() {
        // CDI
    }

    @Inject
    public MinsterettCore2022(@KonfigVerdi(value = PROP_NAME_DATO, required = false) LocalDate ikrafttredelse) {
        // Pass på å ikke endre dato som skal brukes i produksjon før ting er vedtatt ...
        this.ikrafttredelseDato = (Environment.current().isProd() || ikrafttredelse == null) ? DATO_FOR_PROD : ikrafttredelse;
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

}
