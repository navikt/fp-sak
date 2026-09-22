package no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import jakarta.validation.constraints.NotNull;

/**
 * Svar per forespørsel. Tre tilstander framfor boolean slik at fp-sak kan svare UKJENT i stedet for
 * å gjette. Ryddejobben i fp-inntektsmelding skal aldri lukke på UKJENT.
 *
 * Vurderingen utledes av årsaken, se {@link #av}. Begge ligger på tråden slik at kallende tjeneste
 * kan filtrere på vurdering uten å kjenne alle årsakskodene, men ugyldige kombinasjoner kan ikke
 * konstrueres.
 *
 * MIDLERTIDIG. Se {@link ForespørselStatusRequest}.
 */
public record ForespørselStatusResponse(@NotNull String fagsakSaksnummer,
                                        @NotNull String orgnummer,
                                        @NotNull Vurdering vurdering,
                                        @NotNull Årsak årsak) {

    public ForespørselStatusResponse {
        if (årsak != null && vurdering != årsak.vurdering()) {
            throw new IllegalArgumentException("Vurdering " + vurdering + " hører ikke til årsak " + årsak + ". Bruk av(..).");
        }
    }

    public static ForespørselStatusResponse av(String fagsakSaksnummer, String orgnummer, Årsak årsak) {
        return new ForespørselStatusResponse(fagsakSaksnummer, orgnummer, årsak.vurdering(), årsak);
    }

    public enum Vurdering {
        TRENGS,
        TRENGS_IKKE,
        UKJENT
    }

    public enum Årsak {
        /** Arbeidsgiveren mangler fortsatt inntektsmelding på saken. */
        MANGLER_INNTEKTSMELDING(Vurdering.TRENGS),

        SAK_AVSLUTTET(Vurdering.TRENGS_IKKE),
        BEHANDLING_AVSLÅTT(Vurdering.TRENGS_IKKE),
        BEHANDLING_HENLAGT(Vurdering.TRENGS_IKKE),
        INNTEKTSMELDING_MOTTATT(Vurdering.TRENGS_IKKE),
        /** Saksbehandler har avklart at inntektsmelding ikke er påkrevd for arbeidsforholdet. */
        AVKLART_IKKE_PÅKREVD(Vurdering.TRENGS_IKKE),
        /** Arbeidsgiveren er ikke blant dem saken krever inntektsmelding fra. */
        ORGNR_IKKE_PÅKREVD(Vurdering.TRENGS_IKKE),
        /** Saken har ingen ytelsesbehandling å vurdere kravet mot. */
        INGEN_BEHANDLING(Vurdering.TRENGS_IKKE),

        /**
         * Saksnummeret finnes ikke i fp-sak, eller saken har en annen ytelse enn forespørselen.
         * Dette er en datafeil som skal ses på manuelt, ikke lukkes automatisk.
         */
        SAK_IKKE_FUNNET(Vurdering.UKJENT);

        private final Vurdering vurdering;

        Årsak(Vurdering vurdering) {
            this.vurdering = vurdering;
        }

        public Vurdering vurdering() {
            return vurdering;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" + fagsakSaksnummer + ", " + tilMaskertNummer(orgnummer) + ", " + vurdering + ", " + årsak + ">";
    }
}
