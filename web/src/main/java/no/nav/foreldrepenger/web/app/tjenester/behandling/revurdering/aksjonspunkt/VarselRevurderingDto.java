package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.validering.ValidKodeverk;

@JsonTypeName(AksjonspunktKodeDefinisjon.VARSEL_REVURDERING_MANUELL_KODE)
public class VarselRevurderingDto extends BekreftetAksjonspunktDto {

    private boolean sendVarsel;

    private LocalDate frist;

    @ValidKodeverk
    private Venteårsak ventearsak;

    protected VarselRevurderingDto() {
        super();
    }

    public boolean isSendVarsel() {
        return sendVarsel;
    }

    public LocalDate getFrist() {
        return frist;
    }

    public Venteårsak getVentearsak() {
        return ventearsak;
    }
}
