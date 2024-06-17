package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageHjemmel;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;

public record KlageVurderingResultatDto(String klageVurdertAv, KlageVurdering klageVurdering, String begrunnelse, KlageMedholdÅrsak klageMedholdArsak,
                                        KlageVurderingOmgjør klageVurderingOmgjoer, KlageHjemmel klageHjemmel, boolean godkjentAvMedunderskriver,
                                        String fritekstTilBrev) {
}
