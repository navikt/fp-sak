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
 * Klasse for styring av ikrafttredelese nytt regelverk for uttak
 * Metode for å gi ikrafttredelsesdato avhengig av miljø
 * Metode for å vurdere om en Familiehendelse skal vurderes etter nye eller gamle regler. Vil bli oppdatert
 * TODO: Etter dato passert og overgang -> flytt til sentral konfigklasse - skal ikke lenger ha miljøavvik
 */
@ApplicationScoped
public class Utsettelse2021 {

    private static final String PROP_NAME_DATO = "dato.for.nye.uttaksregler";
    private static final LocalDate DATO_FOR_PROD = LocalDate.of(2999,12,31); // LA STÅ. Ikke endre før vi er klare

    private LocalDate ikrafttredelseDato = DATO_FOR_PROD;

    Utsettelse2021() {
        // CDI
    }

    @Inject
    public Utsettelse2021(@KonfigVerdi(value = PROP_NAME_DATO) LocalDate ikrafttredelse) {
        // Pass på å ikke endre dato som skal brukes i produksjon før ting er vedtatt ...
        this.ikrafttredelseDato = (Environment.current().isProd() || ikrafttredelse == null) ? DATO_FOR_PROD : ikrafttredelse;
    }

    public LocalDate ikrafttredelseDato() {
        return this.ikrafttredelseDato;
    }

    public boolean skalBehandlesEtterNyeReglerUttak(FamilieHendelseGrunnlagEntitet familieHendelseGrunnlag) {
        if (familieHendelseGrunnlag == null) return false;
        var bekreftetFamilieHendelse = familieHendelseGrunnlag.getGjeldendeBekreftetVersjon()
            .filter(fh -> !FamilieHendelseType.TERMIN.equals(fh.getType()));
        if (bekreftetFamilieHendelse.isPresent()) {
            return bekreftetFamilieHendelse.map(FamilieHendelseEntitet::getSkjæringstidspunkt).filter(t -> !t.isBefore(ikrafttredelseDato())).isPresent();
        }
        var gjeldendeFH = familieHendelseGrunnlag.getGjeldendeVersjon();
        if (gjeldendeFH == null) return false;
        if (gjeldendeFH.getSkjæringstidspunkt().isBefore(ikrafttredelseDato())) return false;
        if (!gjeldendeFH.getGjelderFødsel()) return LocalDate.now().isAfter(ikrafttredelseDato());
        return LocalDate.now().isAfter(ikrafttredelseDato().plusWeeks(2)); // Frist for registrering av fødsel i FREG
    }

}
