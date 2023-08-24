package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

import java.time.LocalDate;

@JsonTypeName(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST_KODE)
public class VurderSøknadsfristDto extends BekreftetAksjonspunktDto {

    private LocalDate ansesMottattDato;

    private boolean harGyldigGrunn;

    VurderSøknadsfristDto() {
        // for json deserialisering
    }

    public VurderSøknadsfristDto(String begrunnelse, Boolean harGyldigGrunn) {
        super(begrunnelse);
        this.harGyldigGrunn = harGyldigGrunn;
    }

    public LocalDate getAnsesMottattDato() {
        return ansesMottattDato;
    }

    public void setAnsesMottattDato(LocalDate mottatDato) {
        this.ansesMottattDato = mottatDato;
    }

    public boolean harGyldigGrunn() {
        return harGyldigGrunn;
    }

    public void setHarGyldigGrunn(boolean harGyldigGrunn) {
        this.harGyldigGrunn = harGyldigGrunn;
    }
}
