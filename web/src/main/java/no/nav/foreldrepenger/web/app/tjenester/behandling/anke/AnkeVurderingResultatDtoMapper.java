package no.nav.foreldrepenger.web.app.tjenester.behandling.anke;

import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeOmgjørÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingOmgjør;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;

public class AnkeVurderingResultatDtoMapper {

    private AnkeVurderingResultatDtoMapper() {
    }

    public static Optional<AnkeVurderingResultatDto> mapAnkeVurderingResultatDto(Behandling behandling, AnkeRepository ankeRepository) {
        Optional<AnkeVurderingResultatEntitet> resultatOpt = ankeRepository.hentAnkeVurderingResultat(behandling.getId());
        return resultatOpt.map(AnkeVurderingResultatDtoMapper::lagDto);
    }

    private static AnkeVurderingResultatDto lagDto(AnkeVurderingResultatEntitet ankeVurderingResultat) {
        String ankeOmgjørÅrsak = ankeVurderingResultat.getAnkeOmgjørÅrsak().equals(AnkeOmgjørÅrsak.UDEFINERT) ? null : ankeVurderingResultat.getAnkeOmgjørÅrsak().getKode();
        String ankeOmgjørÅrsakNavn = ankeVurderingResultat.getAnkeOmgjørÅrsak().equals(AnkeOmgjørÅrsak.UDEFINERT) ? null : ankeVurderingResultat.getAnkeOmgjørÅrsak().getNavn();
        String ankeVurderingOmgjør = ankeVurderingResultat.getAnkeVurderingOmgjør().equals(AnkeVurderingOmgjør.UDEFINERT) ? null : ankeVurderingResultat.getAnkeVurderingOmgjør().getKode();
        String ankeVurdering = ankeVurderingResultat.getAnkeVurdering().equals(AnkeVurdering.UDEFINERT) ? null : ankeVurderingResultat.getAnkeVurdering().getKode();

        Long paAnketBehandlingId = ankeVurderingResultat.getAnkeResultat().getPåAnketBehandling().map(Behandling :: getId).orElse(null);
        UUID paAnketBehandlingUuid = ankeVurderingResultat.getAnkeResultat().getPåAnketBehandling().map(Behandling :: getUuid).orElse(null);
        AnkeVurderingResultatDto dto = new AnkeVurderingResultatDto();

        dto.setAnkeVurdering(ankeVurdering);
        dto.setAnkeVurderingOmgjoer(ankeVurderingOmgjør);
        dto.setBegrunnelse(ankeVurderingResultat.getBegrunnelse());
        dto.setFritekstTilBrev(ankeVurderingResultat.getFritekstTilBrev());
        dto.setAnkeOmgjoerArsak(ankeOmgjørÅrsak);
        dto.setAnkeOmgjoerArsakNavn(ankeOmgjørÅrsakNavn);
        dto.setGodkjentAvMedunderskriver(ankeVurderingResultat.godkjentAvMedunderskriver());
        dto.setErAnkerIkkePart(ankeVurderingResultat.erAnkerIkkePart());
        dto.setErFristIkkeOverholdt(ankeVurderingResultat.erFristIkkeOverholdt());
        dto.setErIkkeKonkret(ankeVurderingResultat.erIkkeKonkret());
        dto.setErIkkeSignert(ankeVurderingResultat.erIkkeSignert());
        dto.setErSubsidiartRealitetsbehandles(ankeVurderingResultat.erSubsidiartRealitetsbehandles());
        dto.setPaAnketBehandlingId(paAnketBehandlingId);
        dto.setPaAnketBehandlingUuid(paAnketBehandlingUuid);
        return dto;
    }
}
