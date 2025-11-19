package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;

public record KlageFormkravResultatDto(@NotNull UUID paKlagdBehandlingUuid,
                                       @NotNull UUID påKlagdBehandlingUuid,
                                       @NotNull String begrunnelse,
                                       @NotNull boolean erKlagerPart,
                                       @NotNull boolean erKlageKonkret,
                                       @NotNull boolean erKlagefirstOverholdt,
                                       @NotNull boolean erSignert,
                                       @NotNull List<KlageAvvistÅrsak> avvistArsaker,
                                       @NotNull List<KlageAvvistÅrsak> avvistÅrsaker) {
}
