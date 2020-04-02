package no.nav.foreldrepenger.web.app.tjenester.behandling.klage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
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
        Optional<Behandling> paKlagdBehandling = klageFormkrav.hentKlageResultat().getPåKlagdBehandling();
        Optional<UUID> paKlagdEksternBehandlingUuid = klageFormkrav.hentKlageResultat().getPåKlagdEksternBehandling();
        KlageFormkravResultatDto dto = new KlageFormkravResultatDto();
        if (paKlagdBehandling.isEmpty() && paKlagdEksternBehandlingUuid.isPresent()) {
            Optional<TilbakekrevingVedtakDto> tilbakekrevingVedtakDto = hentPåklagdBehandlingIdForEksternApplikasjon(paKlagdEksternBehandlingUuid.get(), fptilbakeRestKlient);
            if (tilbakekrevingVedtakDto.isPresent()) {
                dto.setPaKlagdBehandlingId(tilbakekrevingVedtakDto.get().getBehandlingId());
                dto.setPaklagdBehandlingType(BehandlingType.fraKode(tilbakekrevingVedtakDto.get().getTilbakekrevingBehandlingType()));
            }
        } else {
            dto.setPaKlagdBehandlingId(paKlagdBehandling.map(Behandling::getId).orElse(null));
            dto.setPaklagdBehandlingType(paKlagdBehandling.map(Behandling::getType).orElse(null));
        }
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

    private static Optional<TilbakekrevingVedtakDto> hentPåklagdBehandlingIdForEksternApplikasjon(UUID paKlagdEksternBehandlingUuid, FptilbakeRestKlient fptilbakeRestKlient){
        return Optional.ofNullable(fptilbakeRestKlient.hentTilbakekrevingsVedtakInfo(paKlagdEksternBehandlingUuid));
    }
}
