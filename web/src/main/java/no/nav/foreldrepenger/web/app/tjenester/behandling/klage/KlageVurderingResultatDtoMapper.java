package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;

public class KlageVurderingResultatDtoMapper {

    private KlageVurderingResultatDtoMapper() {
    }

    public static KlageVurderingResultatDto mapKlageVurderingResultatDto(KlageVurderingResultat klageVurderingResultat) {
        String klageMedholdArsak = klageVurderingResultat.getKlageMedholdÅrsak().equals(KlageMedholdÅrsak.UDEFINERT) ? null : klageVurderingResultat.getKlageMedholdÅrsak().getKode();
        String klageMedholdArsakNavn = klageVurderingResultat.getKlageMedholdÅrsak().equals(KlageMedholdÅrsak.UDEFINERT) ? null : klageVurderingResultat.getKlageMedholdÅrsak().getNavn();
        String klageVurderingOmgjør = klageVurderingResultat.getKlageVurderingOmgjør().equals(KlageVurderingOmgjør.UDEFINERT) ? null: klageVurderingResultat.getKlageVurderingOmgjør().getKode();
        String klageVurdering = klageVurderingResultat.getKlageVurdering().equals(KlageVurdering.UDEFINERT) ? null : klageVurderingResultat.getKlageVurdering().getKode();
        KlageVurderingResultatDto dto = new KlageVurderingResultatDto();

        dto.setKlageVurdering(klageVurdering);
        dto.setKlageVurderingOmgjoer(klageVurderingOmgjør);
        dto.setBegrunnelse(klageVurderingResultat.getBegrunnelse());
        dto.setFritekstTilBrev(klageVurderingResultat.getFritekstTilBrev());
        dto.setKlageMedholdArsak(klageMedholdArsak);
        dto.setKlageMedholdArsakNavn(klageMedholdArsakNavn);
        dto.setKlageVurdertAv(klageVurderingResultat.getKlageVurdertAv().getKode());
        dto.setGodkjentAvMedunderskriver(klageVurderingResultat.isGodkjentAvMedunderskriver());
        return dto;
    }
}
