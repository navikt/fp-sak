package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_KODE)
public class ForeslaVedtakAksjonspunktDto extends VedtaksbrevOverstyringDto {


    ForeslaVedtakAksjonspunktDto() {
        // for jackson
    }

    public ForeslaVedtakAksjonspunktDto(String begrunnelse, String overskrift, String fritekst,
                                        boolean skalBrukeOverstyrendeFritekstBrev) {
        super(begrunnelse, overskrift, fritekst, skalBrukeOverstyrendeFritekstBrev);
    }

}
