package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderForutgåendeMedlemskapDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderForutgåendeMedlemskapsvilkårOppdaterer implements AksjonspunktOppdaterer<VurderForutgåendeMedlemskapDto> {

    private MedlemskapAksjonspunktFellesTjeneste medlemskapAksjonspunktFellesTjeneste;

    @Inject
    public VurderForutgåendeMedlemskapsvilkårOppdaterer(
        MedlemskapAksjonspunktFellesTjeneste medlemskapAksjonspunktFellesTjeneste) {
        this.medlemskapAksjonspunktFellesTjeneste = medlemskapAksjonspunktFellesTjeneste;
    }

    VurderForutgåendeMedlemskapsvilkårOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(VurderForutgåendeMedlemskapDto dto, AksjonspunktOppdaterParameter param) {
        var avslagskode = dto.getAvslagskode();
        var medlemFom = dto.getMedlemFom();
        var begrunnelse = dto.getBegrunnelse();
        var utfall = medlemskapAksjonspunktFellesTjeneste.oppdaterForutgående(param.getRef(), avslagskode, medlemFom, begrunnelse, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP);
        if (VilkårUtfallType.OPPFYLT.equals(utfall)) {
            return OppdateringResultat.utenTransisjon().leggTilManueltOppfyltVilkår(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE).build();
        } else {
            return OppdateringResultat.utenTransisjon()
                .medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR)
                .leggTilManueltAvslåttVilkår(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE, avslagskode)
                .medVilkårResultatType(VilkårResultatType.AVSLÅTT)
                .build();
        }

    }
}
