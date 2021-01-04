package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AvslagbartAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.VurdereYtelseSammeBarnAnnenForelderAksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.VurdereYtelseSammeBarnSøkerAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

/**
 * Håndterer oppdatering av Aksjonspunkt og endringshistorikk ved vurdering av ytelse knyttet til samme barn.
 */
public abstract class VurdereYtelseSammeBarnOppdaterer implements AksjonspunktOppdaterer<AvslagbartAksjonspunktDto> {

    private HistorikkTjenesteAdapter historikkAdapter;

    protected VurdereYtelseSammeBarnOppdaterer() {
        // for CDI proxy
    }

    @Inject
    VurdereYtelseSammeBarnOppdaterer(HistorikkTjenesteAdapter historikkAdapter) {
        this.historikkAdapter = historikkAdapter;
    }

    @Override
    public OppdateringResultat oppdater(AvslagbartAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Behandling behandling = param.getBehandling();
        Optional<Vilkår> relevantVilkår = finnRelevantVilkår(behandling);
        if (relevantVilkår.isPresent()) {
            Vilkår vilkår = relevantVilkår.get();
            boolean totrinn = endringsHåndtering(behandling, vilkår, dto, finnTekstForFelt(vilkår), param);
            if (dto.getErVilkarOk()) {
                var resultatBuilder = OppdateringResultat.utenTransisjon();
                resultatBuilder.leggTilVilkårResultat(vilkår.getVilkårType(), VilkårUtfallType.OPPFYLT);
                return resultatBuilder.medTotrinnHvis(totrinn).build();
            } else {
                var resultatBuilder = OppdateringResultat.utenTransisjon();
                var avslagsårsak = dto.getAvslagskode() == null ? null : Avslagsårsak.fraKode(dto.getAvslagskode());
                resultatBuilder.leggTilAvslåttVilkårResultat(vilkår.getVilkårType(), avslagsårsak);
                return resultatBuilder.medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR).build();
            }
        }
        return OppdateringResultat.utenOveropp();

    }

    private boolean endringsHåndtering(Behandling behandling, Vilkår vilkår, AvslagbartAksjonspunktDto dto, HistorikkEndretFeltType historikkEndretFeltType, AksjonspunktOppdaterParameter param) {
        String aksjonspunktKode = dto.getKode();
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.fraKode(aksjonspunktKode);
        return new HistorikkAksjonspunktAdapter(behandling, historikkAdapter, param)
                .håndterAksjonspunkt(aksjonspunktDefinisjon, vilkår, dto.getErVilkarOk(), dto.getBegrunnelse(), historikkEndretFeltType);
    }

    private HistorikkEndretFeltType finnTekstForFelt(Vilkår vilkår) {
        VilkårType vilkårType = vilkår.getVilkårType();
        if (VilkårType.FØDSELSVILKÅRET_MOR.equals(vilkårType) || VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR.equals(vilkårType)) {
            return HistorikkEndretFeltType.FODSELSVILKARET;
        } else if (VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD.equals(vilkårType)) {
            return HistorikkEndretFeltType.ADOPSJONSVILKARET;
        }
        return HistorikkEndretFeltType.UDEFINIERT;
    }

    private Optional<Vilkår> finnRelevantVilkår(Behandling behandling) {

        List<VilkårType> relevanteVilkårTyper = Arrays.asList(VilkårType.FØDSELSVILKÅRET_MOR, VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR, VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD);
        List<Vilkår> vilkårene = behandling.getBehandlingsresultat().getVilkårResultat().getVilkårene();

        return vilkårene.stream()
                .filter(v -> relevanteVilkårTyper.contains(v.getVilkårType()))
                .findFirst();
    }

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = VurdereYtelseSammeBarnSøkerAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class VurdereYtelseSammeBarnSøkerOppdaterer extends VurdereYtelseSammeBarnOppdaterer {
        VurdereYtelseSammeBarnSøkerOppdaterer() {
            // for CDI proxy
        }

        @Inject
        public VurdereYtelseSammeBarnSøkerOppdaterer(HistorikkTjenesteAdapter historikkAdapter) {
            super(historikkAdapter);
        }
    }

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = VurdereYtelseSammeBarnAnnenForelderAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class VurdereYtelseSammeBarnAnnenForelderOppdaterer extends VurdereYtelseSammeBarnOppdaterer {
        public VurdereYtelseSammeBarnAnnenForelderOppdaterer() {
            // for CDI proxy
        }

        @Inject
        public VurdereYtelseSammeBarnAnnenForelderOppdaterer(HistorikkTjenesteAdapter historikkAdapter) {
            super(historikkAdapter);
        }
    }
}
