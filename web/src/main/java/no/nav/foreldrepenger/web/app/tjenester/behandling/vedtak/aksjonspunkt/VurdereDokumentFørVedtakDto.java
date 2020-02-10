package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK_KODE)
public class VurdereDokumentFørVedtakDto extends BekreftetAksjonspunktDto {


    VurdereDokumentFørVedtakDto() {
        // For Jackson
    }

    public VurdereDokumentFørVedtakDto(String begrunnelse) { // NOSONAR
        super(begrunnelse);
    }

}
