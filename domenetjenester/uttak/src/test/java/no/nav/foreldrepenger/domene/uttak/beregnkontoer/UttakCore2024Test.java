package no.nav.foreldrepenger.domene.uttak.beregnkontoer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;

/**
 * TODO: Fjerne denne før ferien .... vil degradere ettersom tiden går. Klassen UttakCore2024 slettes 2/8.
 * Scenariobeskrivelser
 * - ikrafttredelse-konfig ikke satt (eller lik lovdato), før lovdato, etter lovdato
 * - familihendelse dagens dato eller før/etter ikrafttredelse skal virke som ønsket, inklusive overgang
 */
class UttakCore2024Test {

    private static final LocalDate IKRAFT1 = LocalDate.of(2024, Month.JULY, 1);
    private static final LocalDate IKRAFT2 = LocalDate.of(2024, Month.AUGUST, 2);

    @Test
    void ingen_endring_ikraft() {
        if (!LocalDate.now().isBefore(IKRAFT1)) { // Vil feile 1/7 - har ikke støtte for senere ikrafttredelse
            return;
        }
        var ikrafttredelse1 = IKRAFT1;
        var ikrafttredelse2 = IKRAFT2;
        var uttakcore2024 = new UttakCore2024(ikrafttredelse1, ikrafttredelse2);

        var familiehendelseDatoFør = ikrafttredelse1.minusMonths(1);
        var familiehendelseFør = FamilieHendelse.forFødsel(familiehendelseDatoFør, familiehendelseDatoFør, List.of(new Barn()), 1);
        assertThat(uttakcore2024.utledRegelvalgsdato(familiehendelseFør)).isNull();

        var familiehendelseDatoMellom = ikrafttredelse1.plusMonths(1);
        var familiehendelseMellom = FamilieHendelse.forFødsel(familiehendelseDatoMellom, familiehendelseDatoMellom, List.of(new Barn()), 1);
        assertThat(uttakcore2024.utledRegelvalgsdato(familiehendelseMellom)).isEqualTo(LocalDate.now());

        var familiehendelseDatoEtter = ikrafttredelse2.plusMonths(1);
        var familiehendelseEtter = FamilieHendelse.forFødsel(familiehendelseDatoEtter, familiehendelseDatoEtter, List.of(new Barn()), 1);
        assertThat(uttakcore2024.utledRegelvalgsdato(familiehendelseEtter)).isEqualTo(LocalDate.now());
    }

    @Test
    void første_endring_ikraft() {
        if (!LocalDate.now().isBefore(IKRAFT1)) { // Vil feile etter ikrafttredelse
            return;
        }
        var ikrafttredelse1 = IKRAFT1.minusMonths(2);
        var ikrafttredelse2 = IKRAFT2;
        var uttakcore2024 = new UttakCore2024(ikrafttredelse1, ikrafttredelse2);

        var familiehendelseDatoFør = ikrafttredelse1.minusMonths(1);
        var familiehendelseFør = FamilieHendelse.forFødsel(familiehendelseDatoFør, familiehendelseDatoFør, List.of(new Barn()), 1);
        assertThat(uttakcore2024.utledRegelvalgsdato(familiehendelseFør)).isNull();

        var familiehendelseDatoMellom = ikrafttredelse1.plusMonths(1);
        var familiehendelseMellom = FamilieHendelse.forFødsel(familiehendelseDatoMellom, familiehendelseDatoMellom, List.of(new Barn()), 1);
        assertThat(uttakcore2024.utledRegelvalgsdato(familiehendelseMellom)).isEqualTo(IKRAFT1);

        var familiehendelseDatoEtter = ikrafttredelse2.plusMonths(1);
        var familiehendelseEtter = FamilieHendelse.forFødsel(familiehendelseDatoEtter, familiehendelseDatoEtter, List.of(new Barn()), 1);
        assertThat(uttakcore2024.utledRegelvalgsdato(familiehendelseEtter)).isEqualTo(IKRAFT1);
    }

    @Test
    void begge_endringer_ikraft() {
        if (!LocalDate.now().isBefore(IKRAFT1)) { // Vil feile etter ikraftredelse
            return;
        }
        var ikrafttredelse1 = IKRAFT1.minusMonths(9);
        var ikrafttredelse2 = IKRAFT2.minusMonths(9);
        var uttakcore2024 = new UttakCore2024(ikrafttredelse1, ikrafttredelse2);

        var familiehendelseDatoFør = ikrafttredelse1.minusMonths(1);
        var familiehendelseFør = FamilieHendelse.forFødsel(familiehendelseDatoFør, familiehendelseDatoFør, List.of(new Barn()), 1);
        assertThat(uttakcore2024.utledRegelvalgsdato(familiehendelseFør)).isNull();

        var familiehendelseDatoMellom = ikrafttredelse1.plusMonths(1);
        var familiehendelseMellom = FamilieHendelse.forFødsel(familiehendelseDatoMellom, familiehendelseDatoMellom, List.of(new Barn()), 1);
        assertThat(uttakcore2024.utledRegelvalgsdato(familiehendelseMellom)).isEqualTo(IKRAFT1);

        var familiehendelseDatoEtter = ikrafttredelse2.plusMonths(1);
        var familiehendelseEtter = FamilieHendelse.forFødsel(familiehendelseDatoEtter, familiehendelseDatoEtter, List.of(new Barn()), 1);
        assertThat(uttakcore2024.utledRegelvalgsdato(familiehendelseEtter)).isEqualTo(IKRAFT2);
    }


}
