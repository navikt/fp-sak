package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.sammebarn;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdateringTransisjon;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AvslagbartAksjonspunktDto;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.HistorikkSammeBarnTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.sammebarn.dto.VurdereYtelseSammeBarnSøkerAksjonspunktDto;
import no.nav.vedtak.exception.FunksjonellException;

/**
 * Håndterer oppdatering av Aksjonspunkt og endringshistorikk ved vurdering av ytelse knyttet til samme barn.
 */
@ApplicationScoped
@DtoTilServiceAdapter(dto = VurdereYtelseSammeBarnSøkerAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurdereYtelseSammeBarnSøkerOppdaterer implements AksjonspunktOppdaterer<AvslagbartAksjonspunktDto> {

    private HistorikkSammeBarnTjeneste historikkSammeBarnTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    VurdereYtelseSammeBarnSøkerOppdaterer(HistorikkSammeBarnTjeneste historikkSammeBarnTjeneste, BehandlingsresultatRepository behandlingsresultatRepository) {
        this.historikkSammeBarnTjeneste = historikkSammeBarnTjeneste;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    protected VurdereYtelseSammeBarnSøkerOppdaterer() {
        // for CDI proxy
    }

    @Override
    public OppdateringResultat oppdater(AvslagbartAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var ref = param.getRef();
        var behandlingsresultat = behandlingsresultatRepository.hent(ref.behandlingId());
        var relevantVilkår = finnRelevantVilkår(behandlingsresultat);
        if (relevantVilkår.isPresent()) {
            var vilkår = relevantVilkår.get();
            historikkSammeBarnTjeneste.lagHistorikkinnslagForAksjonspunkt(ref, behandlingsresultat, dto, vilkår);
            if (Boolean.TRUE.equals(dto.getErVilkarOk())) {
                var nyttUtfall = Boolean.TRUE.equals(dto.getErVilkarOk()) ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
                var totrinn = !Objects.equals(vilkår.getGjeldendeVilkårUtfall(), nyttUtfall);
                var resultatBuilder = OppdateringResultat.utenTransisjon();
                resultatBuilder.leggTilManueltOppfyltVilkår(vilkår.getVilkårType());
                return resultatBuilder.medTotrinnHvis(totrinn).build();
            } else {
                var resultatBuilder = OppdateringResultat.utenTransisjon();
                var avslagsårsak = Avslagsårsak.fraDefinertKode(dto.getAvslagskode())
                    .orElseThrow(() -> new FunksjonellException("FP-MANGLER-ÅRSAK", "Ugyldig avslagsårsak", "Velg gyldig avslagsårsak"));
                return resultatBuilder
                    .medFremoverHopp(AksjonspunktOppdateringTransisjon.AVSLAG_VILKÅR)
                    .leggTilManueltAvslåttVilkår(vilkår.getVilkårType(), avslagsårsak)
                    .build();
            }
        }
        return OppdateringResultat.utenOverhopp();

    }

    private Optional<Vilkår> finnRelevantVilkår(Behandlingsresultat behandlingsresultat) {
        var relevanteVilkårTyper = Set.of(
            VilkårType.FØDSELSVILKÅRET_MOR,
            VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR,
            VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD,
            VilkårType.OMSORGSOVERTAKELSEVILKÅR
        );
        return behandlingsresultat.getVilkårResultat().getVilkårene().stream()
            .filter(v -> relevanteVilkårTyper.contains(v.getVilkårType()))
            .findFirst();
    }
}
