package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;

import java.util.List;
import java.util.UUID;

public record KlageFormkravResultatDto(Long paKlagdBehandlingId,
                                      UUID paKlagdBehandlingUuid,
                                      BehandlingType paklagdBehandlingType,
                                      String begrunnelse,
                                      boolean erKlagerPart,
                                      boolean erKlageKonkret,
                                      boolean erKlagefirstOverholdt,
                                      boolean erSignert,
                                      List<KlageAvvistÅrsak> avvistArsaker) {
}
