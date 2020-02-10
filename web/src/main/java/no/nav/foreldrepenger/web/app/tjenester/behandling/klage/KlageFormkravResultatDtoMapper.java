package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;

class KlageFormkravResultatDtoMapper {
    private KlageFormkravResultatDtoMapper() {
    }

    static Optional<KlageFormkravResultatDto> mapNFPKlageFormkravResultatDto(Behandling behandling, KlageRepository klageRepository) {
        Optional<KlageFormkravEntitet> resultatOpt = klageRepository.hentKlageFormkrav(behandling, KlageVurdertAv.NFP);
        return resultatOpt.map((KlageFormkravEntitet klageFormkrav) -> lagDto(klageFormkrav));
    }

    static Optional<KlageFormkravResultatDto> mapKAKlageFormkravResultatDto(Behandling behandling, KlageRepository klageRepository) {
        Optional<KlageFormkravEntitet> resultatOpt = klageRepository.hentKlageFormkrav(behandling,KlageVurdertAv.NK);
        return resultatOpt.map((KlageFormkravEntitet klageFormkrav) -> lagDto(klageFormkrav));
    }

    private static KlageFormkravResultatDto lagDto(KlageFormkravEntitet klageFormkrav) {
        Long paKlagdBehandlingId = klageFormkrav.hentKlageResultat().getPåKlagdBehandling().map(Behandling :: getId).orElse(null);
        KlageFormkravResultatDto dto = new KlageFormkravResultatDto();
        dto.setPaKlagdBehandlingId(paKlagdBehandlingId);
        dto.setBegrunnelse(klageFormkrav.hentBegrunnelse());
        dto.setErKlagerPart(klageFormkrav.erKlagerPart());
        dto.setErKlageKonkret(klageFormkrav.erKonkret());
        dto.setErKlagefirstOverholdt(klageFormkrav.erFristOverholdt());
        dto.setErSignert(klageFormkrav.erSignert());
        dto.setAvvistArsaker(getAvvistÅrsaker(klageFormkrav));
        return dto;
    }

    private static List<KlageAvvistÅrsak> getAvvistÅrsaker(KlageFormkravEntitet klageFormkrav) {
        List<KlageAvvistÅrsak> klageAvvistÅrsaker = new ArrayList<>();
        klageFormkrav.hentAvvistÅrsaker().forEach(klageAvvistÅrsak -> klageAvvistÅrsaker.add(klageAvvistÅrsak));
        return klageAvvistÅrsaker;
    }
}
