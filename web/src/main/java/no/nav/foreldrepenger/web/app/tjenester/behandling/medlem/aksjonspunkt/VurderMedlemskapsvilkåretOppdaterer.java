package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderMedlemskapDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderMedlemskapsvilkåretOppdaterer implements AksjonspunktOppdaterer<VurderMedlemskapDto> {

    private MedlemskapAksjonspunktFellesTjeneste medlemskapAksjonspunktFellesTjeneste;

    @Inject
    public VurderMedlemskapsvilkåretOppdaterer(
        MedlemskapAksjonspunktFellesTjeneste medlemskapAksjonspunktFellesTjeneste) {
        this.medlemskapAksjonspunktFellesTjeneste = medlemskapAksjonspunktFellesTjeneste;
    }

    VurderMedlemskapsvilkåretOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(VurderMedlemskapDto dto, AksjonspunktOppdaterParameter param) {
        var avslagskode = dto.getAvslagskode();
        var behandlingId = param.getBehandlingId();
        var opphørFom = dto.getOpphørFom();
        var begrunnelse = dto.getBegrunnelse();
        var utfall = medlemskapAksjonspunktFellesTjeneste.oppdater(behandlingId, avslagskode, opphørFom, begrunnelse,
            SkjermlenkeType.FAKTA_OM_MEDLEMSKAP);
        if (VilkårUtfallType.OPPFYLT.equals(utfall)) {
            return OppdateringResultat.utenTransisjon().leggTilManueltOppfyltVilkår(VilkårType.MEDLEMSKAPSVILKÅRET).build();
        } else {
            return OppdateringResultat.utenTransisjon()
                .medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR)
                .leggTilManueltAvslåttVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, avslagskode)
                .build();
        }
    }
}
