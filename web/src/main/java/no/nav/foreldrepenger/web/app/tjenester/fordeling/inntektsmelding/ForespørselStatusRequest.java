package no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Forespørsel fra fp-inntektsmelding om en enkelt (fagsakSaksnummer, orgnummer)-kombinasjon fortsatt trenger
 * inntektsmelding.
 * <p>
 * MIDLERTIDIG. Engangs ryddejobb i fp-inntektsmelding, fjernes når jobben er kjørt. Løpende lukking
 * håndteres av fp-sak selv (se LukkForespørselRequest/LukkForespørslerImTask).
 */
public record ForespørselStatusRequest(@NotNull @Digits(integer = 19, fraction = 0) String fagsakSaksnummer,
                                       @NotNull @Pattern(regexp = "^\\d{9}$") String orgnummer) {

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + fagsakSaksnummer + ", " + tilMaskertNummer(orgnummer) + ">";
    }
}
