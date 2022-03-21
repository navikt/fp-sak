package no.nav.foreldrepenger.web.app.tjenester.behandling.anke;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;

public record AnkebehandlingDto(AnkeVurderingResultatDto ankeVurderingResultat,
                                KlageHjemmel hjemmelFraKlage,
                                List<KlageHjemmel> aktuelleHjemler,
                                boolean enableKabal,
                                boolean underBehandlingKabal,
                                boolean behandletAvKabal) {

}
