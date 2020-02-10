package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

@JsonTypeName(AksjonspunktKodeDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL_KODE)
public class BekreftVedtakUtenTotrinnskontrollDto extends VedtaksbrevOverstyringDto {


    BekreftVedtakUtenTotrinnskontrollDto() {
        // For Jackson
    }

    public BekreftVedtakUtenTotrinnskontrollDto(String begrunnelse, String overskrift, String fritekstBrev,
                                                boolean skalBrukeOverstyrendeFritekstBrev) { // NOSONAR
        super(begrunnelse, overskrift, fritekstBrev, skalBrukeOverstyrendeFritekstBrev);
    }


}
