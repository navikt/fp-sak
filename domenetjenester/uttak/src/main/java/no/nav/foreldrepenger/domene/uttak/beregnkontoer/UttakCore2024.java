package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

/*
 * OBSOBSOBS: Endelig ikrafttredelse dato og overgangs vedtas først i Statsråd, etter Stortingets behandling av Proposisjonene
 * Klasse for styring av ikrafttredelese nytt regelverk for minsterett og uttak ifm fødsel
 * Metode for å gi ikrafttredelsesdato avhengig av miljø
 * Metode for å vurdere om en Familiehendelse skal vurderes etter nye eller gamle regler. Vil bli oppdatert
 * TODO: Etter dato passert og overgang for testcases -> flytt til sentral konfigklasse - skal ikke lenger ha miljøavvik
 */
@ApplicationScoped
public class UttakCore2024 {

    private static final String PROP_NAME_DATO_DEL1 = "dato.for.aatti.prosent";
    private static final String PROP_NAME_DATO_DEL2 = "dato.for.minsterett.andre";
    private static final LocalDate DATO_FOR_PROD_DEL1 = LocalDate.of(3024, Month.JULY,1); // LA STÅ etter endring til 2024
    private static final LocalDate DATO_FOR_PROD_DEL2 = LocalDate.of(3024,Month.AUGUST,2); // LA STÅ.

    private LocalDate ikrafttredelseDato1 = DATO_FOR_PROD_DEL1;
    private LocalDate ikrafttredelseDato2 = DATO_FOR_PROD_DEL2;

    UttakCore2024() {
        // CDI
    }

    @Inject
    public UttakCore2024(@KonfigVerdi(value = PROP_NAME_DATO_DEL1, required = false) LocalDate ikrafttredelse1,
                         @KonfigVerdi(value = PROP_NAME_DATO_DEL2, required = false) LocalDate ikrafttredelse2) {
        // Pass på å ikke endre dato som skal brukes i produksjon før ting er vedtatt ...
        this.ikrafttredelseDato1 = Environment.current().isProd() || ikrafttredelse1 == null ? DATO_FOR_PROD_DEL1 : ikrafttredelse1;
        this.ikrafttredelseDato2 = Environment.current().isProd() || ikrafttredelse2 == null ? DATO_FOR_PROD_DEL2 : ikrafttredelse2;
    }

    public LocalDate utledRegelvalgsdato(FamilieHendelse familieHendelse) {
        var familieHendelseDato = Optional.ofNullable(familieHendelse)
            .map(FamilieHendelse::getFamilieHendelseDato);
        if (familieHendelseDato.isEmpty() || familieHendelseDato.get().isBefore(ikrafttredelseDato1)) return null;
        return LocalDate.now().isBefore(ikrafttredelseDato2) ? LocalDate.now() : null;
    }


}
