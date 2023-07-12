package no.nav.foreldrepenger.datavarehus.tjeneste;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.datavarehus.domene.KlageVurderingResultatDvh;

class KlageVurderingResultatDvhMapper {

    private KlageVurderingResultatDvhMapper() {
    }

    static KlageVurderingResultatDvh map(KlageVurderingResultat klageVurderingResultat) {
        return KlageVurderingResultatDvh.builder()
            .medKlageMedholdÅrsak(klageVurderingResultat.getKlageMedholdÅrsak().getKode())
            .medKlageBehandlingId(klageVurderingResultat.getKlageResultat().getKlageBehandlingId())
            .medKlageVurdering(klageVurderingResultat.getKlageVurdering().getKode())
            .medKlageVurderingOmgjør(klageVurderingResultat.getKlageVurderingOmgjør().getKode())
            .medKlageHjemmel(klageVurderingResultat.getKlageHjemmel().getKode())
            .medKlageVurdertAv(klageVurderingResultat.getKlageVurdertAv().getKode())
            .medOpprettetTidspunkt(klageVurderingResultat.getOpprettetTidspunkt())
            .build();
    }
}
