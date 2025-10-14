package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;


public final class VilkårDtoMapper {

    private VilkårDtoMapper() {
        // SONAR - Utility classes should not have public constructors
    }

    public static List<VilkårDto> lagVilkarDto(Behandling behandling, Behandlingsresultat behandlingsresultat) {
        return Optional.ofNullable(behandlingsresultat)
            .map(Behandlingsresultat::getVilkårResultat)
            .map(VilkårResultat::getVilkårene).orElse(List.of()).stream()
            .map(vilkår -> lagVilkårDto(behandling, vilkår))
            .toList();
    }

    private static VilkårDto lagVilkårDto(Behandling behandling, Vilkår vilkår) {
        return new VilkårDto(vilkår.getVilkårType(), vilkår.getVilkårType().getLovReferanse(behandling.getFagsakYtelseType()),
            new ArrayList<>(vilkår.getVilkårType().getAvslagsårsaker()), vilkår.getGjeldendeVilkårUtfall(),
            Optional.ofNullable(vilkår.getAvslagsårsak()).map(Avslagsårsak::getKode).orElse(null), erOverstyrbar(vilkår, behandling));
    }

    // Angir om vilkåret kan overstyres (forutsetter at bruker har tilgang til å overstyre)
    private static boolean erOverstyrbar(Vilkår vilkår, Behandling behandling) {
        if (VilkårType.OMSORGSOVERTAKELSEVILKÅR.equals(vilkår.getVilkårType())) {
            // Tilbyr ikke overstyring
            return false;
        }
        if (behandling.erÅpnetForEndring()) {
            // Manuelt åpnet for endring, må dermed også tillate overstyring
            return true;
        }
        if (!behandling.harSattStartpunkt() || BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            // Startpunkt for behandling N/A, må dermed også tillate overstyring
            return true;
        }
        var vilkårLøstFørStartpunkt = StartpunktType.finnVilkårHåndtertInnenStartpunkt(behandling.getStartpunkt())
            .contains(vilkår.getVilkårType());
        return !vilkårLøstFørStartpunkt;
    }
}
