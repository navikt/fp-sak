package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;

public record KlageFormkravResultatDto(@NotNull Long paKlagdBehandlingId,
                                       @NotNull UUID paKlagdBehandlingUuid,
                                       @NotNull BehandlingType paklagdBehandlingType,
                                       @NotNull String begrunnelse,
                                       @NotNull boolean erKlagerPart,
                                       @NotNull boolean erKlageKonkret,
                                       @NotNull boolean erKlagefirstOverholdt,
                                       @NotNull boolean erSignert,
                                       @NotNull List<KlageAvvistÅrsak> avvistArsaker) {
}
