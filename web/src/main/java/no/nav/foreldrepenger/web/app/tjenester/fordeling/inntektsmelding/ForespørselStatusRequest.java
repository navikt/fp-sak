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
 * MIDLERTIDIG. Laget for en engangs ryddejobb i fp-inntektsmelding som går over forespørsler med
 * status UNDER_BEHANDLING fram til en gitt kuttdato. Skal ikke forvaltes videre, og skal fjernes
 * når jobben er kjørt. Løpende lukking av forespørsler håndteres av fp-sak selv, se
 * LukkForespørselRequest og LukkForespørslerImTask.
 *
 * Kuttdatoen settes i spørringen i fp-inntektsmelding og er ikke en del av denne kontrakten.
 * fp-sak svarer alltid ut fra sakens tilstand på svartidspunktet, og gjør ingen sammenligning av
 * datoer/skjæringstidspunkt.
 *
 * Alle forespørslene i en batch må gjelde samme fagsakSaksnummer (maks 100 orgnummer for den ene
 * saken per kall). Dette skyldes at ABAC/PDP (AppPdpRequestBuilderImpl) kun støtter 0 eller 1
 * saksnummer per tilgangskontroll-vurdering. Kaller (fp-inntektsmelding) gjør ett kall per
 * fagsakSaksnummer den vil sjekke, med de aktuelle orgnumrene i samme kall.
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
                              @NotNull @Pattern(regexp = "^\\d{9}$") String orgnummer,
                              @NotNull @Valid YtelseType ytelsetype) {

        @Override
        public String toString() {
            return getClass().getSimpleName() + "<" + fagsakSaksnummer + ", " + tilMaskertNummer(orgnummer) + ", " + ytelsetype + ">";
        }
    }

    public enum YtelseType {
        FORELDREPENGER,
        SVANGERSKAPSPENGER
    }
}
