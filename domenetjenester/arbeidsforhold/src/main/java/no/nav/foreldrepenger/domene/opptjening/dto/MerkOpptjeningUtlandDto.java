package no.nav.foreldrepenger.domene.opptjening.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

import jakarta.validation.Valid;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.UtlandDokumentasjonStatus;

@JsonTypeName(AksjonspunktKodeDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK_KODE)
public class MerkOpptjeningUtlandDto extends BekreftetAksjonspunktDto {

    @Valid
    private UtlandDokumentasjonStatus dokStatus;

    public UtlandDokumentasjonStatus getDokStatus() {
        return dokStatus;
    }
}
