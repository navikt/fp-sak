package no.nav.foreldrepenger.web.app.tjenester.behandling.revurdering.aksjonspunkt;

import java.time.LocalDate;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.util.InputValideringRegex;

public abstract class VarselRevurderingDto extends BekreftetAksjonspunktDto {
    private boolean sendVarsel;

    @Size(max = 10000)
    @Pattern(regexp = InputValideringRegex.FRITEKST)
    private String fritekst;

    private LocalDate frist;

    @ValidKodeverk
    private Venteårsak ventearsak;

    VarselRevurderingDto(String begrunnelse, boolean sendVarsel, String fritekst, LocalDate frist, Venteårsak ventearsak) {
        super(begrunnelse);
        this.sendVarsel = sendVarsel;
        this.fritekst = fritekst;
        this.frist = frist;
        this.ventearsak = ventearsak;
    }

    protected VarselRevurderingDto() {
        super();
    }

    public boolean isSendVarsel() {
        return sendVarsel;
    }

    public String getFritekst() {
        return fritekst;
    }

    public LocalDate getFrist() {
        return frist;
    }

    public Venteårsak getVentearsak() {
        return ventearsak;
    }
}
