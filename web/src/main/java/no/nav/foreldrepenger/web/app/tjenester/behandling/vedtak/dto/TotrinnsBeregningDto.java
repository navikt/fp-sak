package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;


public record TotrinnsBeregningDto(@NotNull boolean fastsattVarigEndringNæring,
                                   @NotNull boolean fastsattVarigEndringNaering,
                                   List<FaktaOmBeregningTilfelle> faktaOmBeregningTilfeller) {
}
