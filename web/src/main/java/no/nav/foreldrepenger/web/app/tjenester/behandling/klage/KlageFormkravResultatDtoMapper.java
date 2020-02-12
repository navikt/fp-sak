package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageFormkravEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageVurdertAv;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.FptilbakeRestKlient;
import no.nav.foreldrepenger.økonomi.tilbakekreving.klient.TilbakekrevingVedtakDto;

class KlageFormkravResultatDtoMapper {
    private KlageFormkravResultatDtoMapper() {
    }

    static Optional<KlageFormkravResultatDto> mapNFPKlageFormkravResultatDto(Behandling behandling, KlageRepository klageRepository, FptilbakeRestKlient fptilbakeRestKlient) {
        Optional<KlageFormkravEntitet> resultatOpt = klageRepository.hentKlageFormkrav(behandling, KlageVurdertAv.NFP);
        return resultatOpt.map((KlageFormkravEntitet klageFormkrav) -> lagDto(klageFormkrav,fptilbakeRestKlient));
    }

    static Optional<KlageFormkravResultatDto> mapKAKlageFormkravResultatDto(Behandling behandling, KlageRepository klageRepository, FptilbakeRestKlient fptilbakeRestKlient) {
        Optional<KlageFormkravEntitet> resultatOpt = klageRepository.hentKlageFormkrav(behandling,KlageVurdertAv.NK);
        return resultatOpt.map((KlageFormkravEntitet klageFormkrav) -> lagDto(klageFormkrav,fptilbakeRestKlient));
    }

    private static KlageFormkravResultatDto lagDto(KlageFormkravEntitet klageFormkrav, FptilbakeRestKlient fptilbakeRestKlient) {
        Long paKlagdBehandlingId = klageFormkrav.hentKlageResultat().getPåKlagdBehandling().map(Behandling::getId).orElse(null);
        Optional<UUID> paKlagdEksternBehandlingUuid = klageFormkrav.hentKlageResultat().getPåKlagdEksternBehandling();
        if (paKlagdBehandlingId == null && paKlagdEksternBehandlingUuid.isPresent()) {
            paKlagdBehandlingId = hentPåklagdBehandlingIdForEksternApplikasjon(paKlagdEksternBehandlingUuid.get(), fptilbakeRestKlient);
        }
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

    private static Long hentPåklagdBehandlingIdForEksternApplikasjon(UUID paKlagdEksternBehandlingUuid, FptilbakeRestKlient fptilbakeRestKlient){
        TilbakekrevingVedtakDto tilbakekrevingVedtakInfo = fptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(paKlagdEksternBehandlingUuid);
        return  tilbakekrevingVedtakInfo != null ? tilbakekrevingVedtakInfo.getBehandlingId() : null;
    }
}
