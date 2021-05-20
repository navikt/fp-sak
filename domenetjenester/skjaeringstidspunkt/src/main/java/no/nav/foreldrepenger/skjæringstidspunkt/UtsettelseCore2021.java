package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.util.env.Environment;

/*
 * OBSOBSOBS: Endelig ikrafttredelse dato og overgangs vedtas først i Statsråd, etter Stortingets behandling av P127L20/21
 * Klasse for styring av ikrafttredelese nytt regelverk for uttak
 * Metode for å gi ikrafttredelsesdato avhengig av miljø
 * Metode for å vurdere om en Familiehendelse skal vurderes etter nye eller gamle regler. Vil bli oppdatert
 * TODO: Etter dato passert og overgang -> flytt til sentral konfigklasse - skal ikke lenger ha miljøavvik
 */
@ApplicationScoped
public class UtsettelseCore2021 {

    public static final boolean DEFAULT_KREVER_SAMMENHENGENDE_UTTAK = true;

    private static final String PROP_NAME_DATO = "dato.for.nye.uttaksregler";
    private static final LocalDate DATO_FOR_PROD = LocalDate.of(2999,12,31); // LA STÅ. Ikke endre før vi er klare

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
        if (!gjeldendeFH.getGjelderFødsel()) return LocalDate.now().isBefore(ikrafttredelseDato);
        return LocalDate.now().isBefore(ikrafttredelseDato.plusWeeks(2)); // Frist for registrering av fødsel i FREG
    }

}
