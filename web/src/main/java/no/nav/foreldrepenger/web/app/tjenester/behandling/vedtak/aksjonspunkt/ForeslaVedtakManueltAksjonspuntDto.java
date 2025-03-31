package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_MANUELT_KODE)
public class ForeslaVedtakManueltAksjonspuntDto extends VedtaksbrevOverstyringDto {


    ForeslaVedtakManueltAksjonspuntDto() {
        // for jackson
    }

    public ForeslaVedtakManueltAksjonspuntDto(String begrunnelse, boolean skalBrukeOverstyrendeFritekstBrev) {
        super(begrunnelse, skalBrukeOverstyrendeFritekstBrev);
    }


}
