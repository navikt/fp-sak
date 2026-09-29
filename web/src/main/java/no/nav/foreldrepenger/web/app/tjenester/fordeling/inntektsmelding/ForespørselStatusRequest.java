package no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.vedtak.exception.FunksjonellException;

/**
 * Forespørsel fra fp-inntektsmelding om et sett forespørsler fortsatt trenger inntektsmelding.
 *
 * MIDLERTIDIG. Engangs ryddejobb i fp-inntektsmelding, fjernes når jobben er kjørt. Løpende lukking
 * håndteres av fp-sak selv (se LukkForespørselRequest/LukkForespørslerImTask).
 *
 * Alle forespørslene i en batch må gjelde samme fagsakSaksnummer, maks 100 orgnummer, se
 * {@link #enesteFagsakSaksnummer()}.
 */
public record ForespørselStatusRequest(@NotNull @Size(min = 1, max = MAKS_ANTALL) List<@Valid @NotNull Forespørsel> forespørsler) {

    public static final int MAKS_ANTALL = 100;

    /**
     * Validerer og returnerer det ene fagsakSaksnummeret alle forespørslene i batchen gjelder.
     *
     * @throws FunksjonellException hvis batchen inneholder forespørsler for mer enn ett fagsakSaksnummer.
     */
    public String enesteFagsakSaksnummer() {
        var distinkteSaksnummer = forespørsler.stream().map(Forespørsel::fagsakSaksnummer).distinct().toList();
        if (distinkteSaksnummer.size() != 1) {
            throw new FunksjonellException("FP-947511",
                "Batchen inneholder forespørsler for %d ulike fagsaker.".formatted(distinkteSaksnummer.size()),
                "Del opp batchen slik at alle forespørsler i samme kall gjelder samme fagsakSaksnummer.");
        }
        return distinkteSaksnummer.getFirst();
    }

    public record Forespørsel(@NotNull @Digits(integer = 19, fraction = 0) String fagsakSaksnummer,
                              @NotNull @Pattern(regexp = "^\\d{9}$") String orgnummer) {

        @Override
        public String toString() {
            return getClass().getSimpleName() + "<" + fagsakSaksnummer + ", " + tilMaskertNummer(orgnummer) + ">";
        }
    }
}
