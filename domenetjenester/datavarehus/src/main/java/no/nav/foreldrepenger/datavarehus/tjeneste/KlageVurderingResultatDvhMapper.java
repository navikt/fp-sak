package no.nav.foreldrepenger.datavarehus.tjeneste;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.datavarehus.domene.KlageVurderingResultatDvh;

public class KlageVurderingResultatDvhMapper {

    public KlageVurderingResultatDvh map(KlageVurderingResultat klageVurderingResultat) {
        return KlageVurderingResultatDvh.builder()
            .medKlageAvvistÅrsak(klageVurderingResultat.getKlageAvvistÅrsak().getKode())
            .medKlageMedholdÅrsak(klageVurderingResultat.getKlageMedholdÅrsak().getKode())
            .medKlageBehandlingId(klageVurderingResultat.getKlageResultat().getKlageBehandling().getId())
            .medKlageVurdering(klageVurderingResultat.getKlageVurdering().getKode())
            .medKlageVurderingOmgjør(klageVurderingResultat.getKlageVurderingOmgjør().getKode())
            .medKlageVurdertAv(klageVurderingResultat.getKlageVurdertAv().getKode())
            .medOpprettetTidspunkt(klageVurderingResultat.getOpprettetTidspunkt())
            .medVedtaksdatoPåklagdBehandling(klageVurderingResultat.getVedtaksdatoPåklagdBehandling())
            .build();
    }
}
