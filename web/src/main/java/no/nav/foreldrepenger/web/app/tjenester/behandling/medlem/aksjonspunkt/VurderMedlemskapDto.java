package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonTypeName;

import no.nav.foreldrepenger.behandling.aksjonspunkt.BekreftetAksjonspunktDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.validering.ValidKodeverk;

@JsonTypeName(AksjonspunktKodeDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET)
public class VurderMedlemskapDto extends BekreftetAksjonspunktDto {

    @ValidKodeverk
    private Avslagsårsak avslagskode;

    private LocalDate opphørFom;

    public Avslagsårsak getAvslagskode() {
        return avslagskode;
    }

    public LocalDate getOpphørFom() {
        return opphørFom;
    }
}
