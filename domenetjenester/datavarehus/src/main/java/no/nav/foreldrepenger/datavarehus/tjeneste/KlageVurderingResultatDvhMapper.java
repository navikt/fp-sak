package no.nav.foreldrepenger.datavarehus.tjeneste;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.datavarehus.domene.KlageVurderingResultatDvh;

public class KlageVurderingResultatDvhMapper {

    public static KlageVurderingResultatDvh map(KlageVurderingResultat klageVurderingResultat) {
        return KlageVurderingResultatDvh.builder()
            .medKlageMedholdÅrsak(klageVurderingResultat.getKlageMedholdÅrsak().getKode())
            .medKlageBehandlingId(klageVurderingResultat.getKlageResultat().getKlageBehandlingId())
            .medKlageVurdering(klageVurderingResultat.getKlageVurdering().getKode())
            .medKlageVurderingOmgjør(klageVurderingResultat.getKlageVurderingOmgjør().getKode())
            .medKlageVurdertAv(klageVurderingResultat.getKlageVurdertAv().getKode())
            .medOpprettetTidspunkt(klageVurderingResultat.getOpprettetTidspunkt())
            .build();
    }
}
