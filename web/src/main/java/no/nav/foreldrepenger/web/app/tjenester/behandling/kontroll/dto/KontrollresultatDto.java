package no.nav.foreldrepenger.web.app.tjenester.behandling.kontroll.dto;

import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.FaresignalVurdering;
import no.nav.foreldrepenger.behandlingslager.risikoklassifisering.Kontrollresultat;

import java.util.List;

public record KontrollresultatDto(Kontrollresultat kontrollresultat,
                                  FaresignalgruppeDto iayFaresignaler,
                                  FaresignalgruppeDto medlFaresignaler,
                                  FaresignalVurdering faresignalVurdering) {
    public static KontrollresultatDto ikkeKlassifisert() {
        return new KontrollresultatDto(Kontrollresultat.IKKE_KLASSIFISERT, null, null, null);
    }

    public record FaresignalgruppeDto(List<String> faresignaler) {}

}
