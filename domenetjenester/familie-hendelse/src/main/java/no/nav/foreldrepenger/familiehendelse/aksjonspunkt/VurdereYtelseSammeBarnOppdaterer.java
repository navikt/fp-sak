package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.util.Arrays;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AvslagbartAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.VurdereYtelseSammeBarnAnnenForelderAksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.VurdereYtelseSammeBarnSøkerAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.vedtak.exception.FunksjonellException;

/**
 * Håndterer oppdatering av Aksjonspunkt og endringshistorikk ved vurdering av ytelse knyttet til samme barn.
 */
public abstract class VurdereYtelseSammeBarnOppdaterer implements AksjonspunktOppdaterer<AvslagbartAksjonspunktDto> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    VurdereYtelseSammeBarnOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                     BehandlingsresultatRepository behandlingsresultatRepository,
                                     BehandlingRepository behandlingRepository) {
        this.historikkAdapter = historikkAdapter;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingRepository = behandlingRepository;
    }

    protected VurdereYtelseSammeBarnOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(AvslagbartAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingReferanse = param.getRef();
        var relevantVilkår = finnRelevantVilkår(behandlingReferanse);
        if (relevantVilkår.isPresent()) {
            var vilkår = relevantVilkår.get();
            var totrinn = endringsHåndtering(behandlingReferanse, vilkår, dto, finnTekstForFelt(vilkår), param);
            if (dto.getErVilkarOk()) {
                var resultatBuilder = OppdateringResultat.utenTransisjon();
                resultatBuilder.leggTilManueltOppfyltVilkår(vilkår.getVilkårType());
                return resultatBuilder.medTotrinnHvis(totrinn).build();
            } else {
                var resultatBuilder = OppdateringResultat.utenTransisjon();
                var avslagsårsak = Avslagsårsak.fraDefinertKode(dto.getAvslagskode())
                    .orElseThrow(() -> new FunksjonellException("FP-MANGLER-ÅRSAK", "Ugyldig avslagsårsak", "Velg gyldig avslagsårsak"));

                return resultatBuilder
                    .medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR)
                    .leggTilManueltAvslåttVilkår(vilkår.getVilkårType(), avslagsårsak)
                    .build();
            }
        }
        return OppdateringResultat.utenOverhopp();

    }

    private boolean endringsHåndtering(BehandlingReferanse behandlingReferanse,
                                       Vilkår vilkår,
                                       AvslagbartAksjonspunktDto dto,
                                       HistorikkEndretFeltType historikkEndretFeltType,
                                       AksjonspunktOppdaterParameter param) {
        var aksjonspunktDefinisjon = dto.getAksjonspunktDefinisjon();
        var behandling = behandlingRepository.hentBehandling(behandlingReferanse.behandlingId());
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId()).orElse(null);
        return new HistorikkAksjonspunktAdapter(behandlingsresultat, historikkAdapter, param)
            .håndterAksjonspunkt(aksjonspunktDefinisjon, vilkår, dto.getErVilkarOk(), dto.getBegrunnelse(), historikkEndretFeltType);
    }

    private HistorikkEndretFeltType finnTekstForFelt(Vilkår vilkår) {
        var vilkårType = vilkår.getVilkårType();
        if (VilkårType.FØDSELSVILKÅRET_MOR.equals(vilkårType) || VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR.equals(
            vilkårType)) {
            return HistorikkEndretFeltType.FODSELSVILKARET;
        }
        if (VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD.equals(vilkårType)) {
            return HistorikkEndretFeltType.ADOPSJONSVILKARET;
        }
        return HistorikkEndretFeltType.UDEFINIERT;
    }

    private Optional<Vilkår> finnRelevantVilkår(BehandlingReferanse behandlingReferanse) {

        var relevanteVilkårTyper = Arrays.asList(VilkårType.FØDSELSVILKÅRET_MOR,
            VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR, VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD);
        var vilkårene = behandlingsresultatRepository.hent(behandlingReferanse.behandlingId()).getVilkårResultat().getVilkårene();

        return vilkårene.stream().filter(v -> relevanteVilkårTyper.contains(v.getVilkårType())).findFirst();
    }

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = VurdereYtelseSammeBarnSøkerAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class VurdereYtelseSammeBarnSøkerOppdaterer extends VurdereYtelseSammeBarnOppdaterer {
        VurdereYtelseSammeBarnSøkerOppdaterer() {
            // for CDI proxy
        }

        @Inject
        public VurdereYtelseSammeBarnSøkerOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                                     BehandlingsresultatRepository behandlingsresultatRepository,
                                                     BehandlingRepository behandlingRepository) {
            super(historikkAdapter, behandlingsresultatRepository, behandlingRepository);
        }
    }

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = VurdereYtelseSammeBarnAnnenForelderAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class VurdereYtelseSammeBarnAnnenForelderOppdaterer extends VurdereYtelseSammeBarnOppdaterer {
        public VurdereYtelseSammeBarnAnnenForelderOppdaterer() {
            // for CDI proxy
        }

        @Inject
        public VurdereYtelseSammeBarnAnnenForelderOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                                             BehandlingsresultatRepository behandlingsresultatRepository,
                                                             BehandlingRepository behandlingRepository) {
            super(historikkAdapter, behandlingsresultatRepository, behandlingRepository);
        }
    }
}
