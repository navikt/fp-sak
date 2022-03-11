package no.nav.foreldrepenger.web.app.tjenester.behandling.klage.aksjonspunkt;

import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.validering.ValidKodeverk;

public record SendTilKabalDto(@Valid @NotNull UUID behandlingUuid,
                              @ValidKodeverk KlageHjemmel klageHjemmel) {

}
