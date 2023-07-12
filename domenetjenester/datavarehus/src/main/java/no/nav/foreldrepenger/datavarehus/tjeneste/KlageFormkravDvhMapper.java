package no.nav.foreldrepenger.datavarehus.tjeneste;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.datavarehus.domene.KlageFormkravDvh;

class KlageFormkravDvhMapper {

    private KlageFormkravDvhMapper() {
    }

    static KlageFormkravDvh map(KlageFormkravEntitet klageFormkrav) {
        return KlageFormkravDvh.builder()
            .medErFristOverholdt(klageFormkrav.erFristOverholdt())
            .medErKlagerPart(klageFormkrav.erKlagerPart())
            .medErKonkret(klageFormkrav.erKonkret())
            .medErSignert(klageFormkrav.erSignert())
            .medGjelderVedtak(klageFormkrav.hentGjelderVedtak())
            .medKlageBehandlingId(klageFormkrav.hentKlageResultat().getKlageBehandlingId())
            .medKlageVurdertAv(klageFormkrav.getKlageVurdertAv().getKode())
            .medOpprettetTidspunkt(klageFormkrav.getOpprettetTidspunkt())
            .build();
    }
}
