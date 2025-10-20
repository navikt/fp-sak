package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;

public abstract class VedtaksbrevOverstyringDto extends BekreftetAksjonspunktDto {

    private boolean skalBrukeOverstyrendeFritekstBrev;
    private String utfyllendeBrevtekst;

    protected VedtaksbrevOverstyringDto() {
        // For Jackson
    }

    protected VedtaksbrevOverstyringDto(String begrunnelse, boolean skalBrukeOverstyrendeFritekstBrev) {
        super(begrunnelse);
        this.skalBrukeOverstyrendeFritekstBrev = skalBrukeOverstyrendeFritekstBrev;
    }

    public boolean isSkalBrukeOverstyrendeFritekstBrev() {
        return skalBrukeOverstyrendeFritekstBrev;
    }
}
