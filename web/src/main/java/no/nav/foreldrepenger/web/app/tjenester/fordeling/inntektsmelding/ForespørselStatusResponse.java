package no.nav.foreldrepenger.web.app.tjenester.fordeling.inntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import jakarta.validation.constraints.NotNull;

/**
 * Svar per forespørsel. Bruk {@link #av} for å opprette - garanterer at vurdering og årsak stemmer overens.
 *
 * MIDLERTIDIG. Se {@link ForespørselStatusRequest}.
 */
public record ForespørselStatusResponse(@NotNull String fagsakSaksnummer,
                                        @NotNull String orgnummer,
                                        @NotNull Vurdering vurdering,
                                        @NotNull Årsak årsak) {

    public static ForespørselStatusResponse av(String fagsakSaksnummer, String orgnummer, Årsak årsak) {
        return new ForespørselStatusResponse(fagsakSaksnummer, orgnummer, årsak.vurdering(), årsak);
    }

    public enum Vurdering {
        TRENGS,
        TRENGS_IKKE,
        UKJENT
    }

    public enum Årsak {
        IM_MANGLER(Vurdering.TRENGS),
        SAK_AVSLUTTET(Vurdering.TRENGS_IKKE),
        BEHANDLING_AVSLUTTET(Vurdering.TRENGS_IKKE),
        IM_MOTTATT(Vurdering.TRENGS_IKKE),
        IM_AVKLART_IKKE_PÅKREVD(Vurdering.TRENGS_IKKE),
        IM_ALDRI_PÅKREVD(Vurdering.TRENGS_IKKE),
        INGEN_BEHANDLING(Vurdering.TRENGS_IKKE),
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
