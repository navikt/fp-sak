package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageMedholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurderingResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;

public class KlageVurderingResultatDtoMapper {

    private KlageVurderingResultatDtoMapper() {
    }

    public static Optional<KlageVurderingResultatDto> mapNFPKlageVurderingResultatDto(Behandling behandling, KlageRepository klageRepository) {
        Optional<KlageVurderingResultat> resultatOpt = klageRepository.hentKlageVurderingResultat(behandling.getId(),KlageVurdertAv.NFP);
        return resultatOpt.map(KlageVurderingResultatDtoMapper::lagDto);
    }

    public static Optional<KlageVurderingResultatDto> mapNKKlageVurderingResultatDto(Behandling behandling,KlageRepository klageRepository) {
        Optional<KlageVurderingResultat> resultatOpt = klageRepository.hentKlageVurderingResultat(behandling.getId(),KlageVurdertAv.NK);
        return resultatOpt.map(KlageVurderingResultatDtoMapper::lagDto);
    }

    private static KlageVurderingResultatDto lagDto(KlageVurderingResultat klageVurderingResultat) {
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
