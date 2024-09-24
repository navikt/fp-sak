package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.validering.ValidKodeverk;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR)
public class VurderForutgåendeMedlemskapDto extends BekreftetAksjonspunktDto {

    @ValidKodeverk
    private Avslagsårsak avslagskode;

    private LocalDate medlemFom;

    public Avslagsårsak getAvslagskode() {
        return avslagskode;
    }

    public LocalDate getMedlemFom() {
        return medlemFom;
    }
}
