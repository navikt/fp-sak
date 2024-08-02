package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * OBSOBSOBS: Endelig ikrafttredelse dato og overgangs vedtas først i Statsråd, etter Stortingets behandling av Proposisjonene
 * Klasse for styring av ikrafttredelese nytt regelverk for minsterett og uttak ifm fødsel
 * Metode for å gi ikrafttredelsesdato avhengig av miljø
 * Metode for å vurdere om en Familiehendelse skal vurderes etter nye eller gamle regler. Vil bli oppdatert
 * Støtter ikke ikrafttredelse senere enn loven tilsier
 * TODO: Slett denne etter at siste dato er passert
 */
@ApplicationScoped
public class UttakCore2024 {


    private static final Logger LOG = LoggerFactory.getLogger(UttakCore2024.class);
    private static final Environment ENV = Environment.current();

    private static final String PROP_NAME_DATO_DEL1 = "dato.for.aatti.prosent";
    private static final String PROP_NAME_DATO_DEL2 = "dato.for.minsterett.andre";
    private static final LocalDate DATO_FOR_PROD_DEL1 = LocalDate.of(2024, Month.JULY,1); // LA STÅ
    private static final LocalDate DATO_FOR_PROD_DEL2 = LocalDate.of(2024,Month.AUGUST,2); // LA STÅ.

    private LocalDate ikrafttredelseDato1 = DATO_FOR_PROD_DEL1;
    private LocalDate ikrafttredelseDato2 = DATO_FOR_PROD_DEL2;

    UttakCore2024() {
        // CDI
    }

    @Inject
    public UttakCore2024(@KonfigVerdi(value = PROP_NAME_DATO_DEL1, required = false) LocalDate ikrafttredelse1,
                         @KonfigVerdi(value = PROP_NAME_DATO_DEL2, required = false) LocalDate ikrafttredelse2) {
        this.ikrafttredelseDato1 = bestemDato(DATO_FOR_PROD_DEL1, ikrafttredelse1);
        this.ikrafttredelseDato2 = bestemDato(DATO_FOR_PROD_DEL2, ikrafttredelse2);
    }

    /*
     * For produksjon: Sørge for at gammelt regelverk benyttes inntil ikrafttredelse er passert - uansett når familiehendelsen er
     * For testmiljø: Kunne sette tidligere ikrafttredelse og sørge for at nytt regelverk brukes - dvs map til før/etter prod-ikrafttredelse
     * For lokal test og verdikjede
     *  - Først validere at mekanismen fungerer - dvs offisiell ikrafttredelse
     *  - Endre default slik at tester bruker nytt regelverk - med mindre man ønsker teste regresjon
     *  - Tidstester - At overgangsmekaismen fungerer - vedtak fattet på gammelt regelverk skal omgjøres til nytt dersom saken tilsier det
     */
    public LocalDate utledRegelvalgsdato(FamilieHendelse familieHendelse) {
        var familieHendelseDato = Optional.ofNullable(familieHendelse)
            .map(FamilieHendelse::getFamilieHendelseDato).orElse(null);
        if (familieHendelseDato == null) return null;
        // ikrafttredelseDato1 kan være før, på eller etter PROP_NAME_DATO_DEL1
        // Først sak som skal ha regelverk før endring uansett dagens dato - sørg for mapping til riktig dato
        if (familieHendelseDato.isBefore(ikrafttredelseDato1)) {
            return null;
        }
        var idag = LocalDate.now();
        // Mellom ikrafttredelser
        if (familieHendelseDato.isBefore(ikrafttredelseDato2)) {
            if (idag.isBefore(ikrafttredelseDato1)) {
                // Skal ha konfig før Endring1. ikrafttredelseDato1 kan være før eller lik DATO_FOR_PROD_DEL1
                return idag;
            } else {
                // Skal ha konfig for Endring1 men ikke Endring2.
                return idag.isBefore(DATO_FOR_PROD_DEL1) ? DATO_FOR_PROD_DEL1 : null;
            }
        }
        if (idag.isBefore(ikrafttredelseDato1)) {
            // Skal ha konfig før Endring1 eller Endring2. ikrafttredelseDato1 kan være før eller lik DATO_FOR_PROD_DEL1
            return idag;
        } else if (idag.isBefore(ikrafttredelseDato2)) {
            // Skal ha konfig for Endring1 men ikke Endring2. ikrafttredelseDato2 kan være før eller etter DATO_FOR_PROD_DEL1
            return idag.isBefore(DATO_FOR_PROD_DEL1) ? DATO_FOR_PROD_DEL1 : idag;
        } else {
            if (ENV.isProd()) {
                // Skal ha konfig for Endring1 og Endring2. ikrafttredelseDato2 kan være før eller etter DATO_FOR_PROD_DEL1/DEL2
                return idag.isBefore(DATO_FOR_PROD_DEL2) ? DATO_FOR_PROD_DEL2 : null;
            } else {
                return idag.isBefore(DATO_FOR_PROD_DEL2) || familieHendelseDato.isBefore(DATO_FOR_PROD_DEL2) ? DATO_FOR_PROD_DEL2 : null;
            }
        }
    }

    private static LocalDate bestemDato(LocalDate ikrafttredelseFraLov, LocalDate ikrafttredelseFraKonfig) {
        if (ENV.isProd()) {
            return ikrafttredelseFraLov;
        } else if (ikrafttredelseFraKonfig != null && ikrafttredelseFraKonfig.isAfter(ikrafttredelseFraLov)) {
            throw new IllegalArgumentException("Støtter ikke forsinket iverksettelse i test");
        } else {
            LOG.info("UTTAK CORE 2024 ikrafttredelse {}", ikrafttredelseFraKonfig);
            return ikrafttredelseFraKonfig == null ? ikrafttredelseFraLov : ikrafttredelseFraKonfig;

        }
    }


}
