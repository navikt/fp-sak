package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.vedtak.util.InputValideringRegex;

public abstract class VedtaksbrevOverstyringDto extends BekreftetAksjonspunktDto {

    @Size(max = 200)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String overskrift;

    @Size(max = 10000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String fritekstBrev;

    private boolean skalBrukeOverstyrendeFritekstBrev;

    protected VedtaksbrevOverstyringDto() {
        // For Jackson
    }

    protected VedtaksbrevOverstyringDto(String begrunnelse, String overskrift, String fritekstBrev, boolean skalBrukeOverstyrendeFritekstBrev) {
        super(begrunnelse);
        this.overskrift = overskrift;
        this.fritekstBrev = fritekstBrev;
        this.skalBrukeOverstyrendeFritekstBrev = skalBrukeOverstyrendeFritekstBrev;
    }

    public String getOverskrift() {
        return overskrift;
    }

    public String getFritekstBrev() {
        return fritekstBrev;
    }

    public boolean isSkalBrukeOverstyrendeFritekstBrev() {
        return skalBrukeOverstyrendeFritekstBrev;
    }
}
