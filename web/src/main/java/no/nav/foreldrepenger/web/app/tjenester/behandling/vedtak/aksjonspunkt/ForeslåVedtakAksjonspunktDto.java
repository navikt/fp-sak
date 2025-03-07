package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_KODE)
public class ForeslåVedtakAksjonspunktDto extends VedtaksbrevOverstyringDto {


    ForeslåVedtakAksjonspunktDto() {
        // for jackson
    }

    public ForeslåVedtakAksjonspunktDto(String begrunnelse, String overskrift, String fritekst, String fritekstBrevHtml, boolean skalBrukeOverstyrendeFritekstBrev) {
        super(begrunnelse, overskrift, fritekst, fritekstBrevHtml, skalBrukeOverstyrendeFritekstBrev);
    }

}
