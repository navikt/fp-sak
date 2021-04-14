package no.nav.foreldrepenger.behandling.steg.foreslåresultat.es;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.steg.foreslåresultat.AvslagsårsakTjeneste;
import no.nav.foreldrepenger.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;

@ApplicationScoped
@FagsakYtelseTypeRef("ES")
public class ForeslåBehandlingsresultatTjenesteImpl implements ForeslåBehandlingsresultatTjeneste {
    private AvslagsårsakTjeneste avslagsårsakTjeneste;
    private RevurderingEndring revurderingEndring;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingRepository behandlingRepository;

    ForeslåBehandlingsresultatTjenesteImpl() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBehandlingsresultatTjenesteImpl(BehandlingsresultatRepository behandlingsresultatRepository,
            BehandlingRepository behandlingRepository,
            AvslagsårsakTjeneste avslagsårsakTjeneste,
            @FagsakYtelseTypeRef("ES") RevurderingEndring revurderingEndring) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.avslagsårsakTjeneste = avslagsårsakTjeneste;
        this.revurderingEndring = revurderingEndring;
    }

    @Override
    public Behandlingsresultat foreslåBehandlingsresultat(BehandlingReferanse ref) {
        var behandlingsresultat = behandlingsresultatRepository.hent(ref.getBehandlingId());
        var behandlingResultatType = utledBehandlingsresultatType(behandlingsresultat);
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(behandlingResultatType);
        var behandling = behandlingRepository.hentBehandling(ref.getBehandlingId());
        if (revurderingEndring.erRevurderingMedUendretUtfall(behandling, behandlingResultatType)) {
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.INGEN_ENDRING);
        }
        if (BehandlingResultatType.AVSLÅTT.equals(behandlingResultatType)) {
            var ikkeOppfyltVilkår = behandlingsresultat.getVilkårResultat().hentIkkeOppfyltVilkår();
            ikkeOppfyltVilkår.ifPresent(vilkår -> {
                var avslagsårsak = avslagsårsakTjeneste.finnAvslagsårsak(vilkår);
                behandlingsresultat.setAvslagsårsak(avslagsårsak);
            });
        } else {
            // Må nullstille avslagårsak (for symmetri med setting avslagsårsak ovenfor,
            // hvor avslagårsak kopieres fra et vilkår)
            var avslagsårsakOpt = Optional.ofNullable(behandlingsresultat.getAvslagsårsak());
            avslagsårsakOpt.ifPresent(ufjernetÅrsak -> behandlingsresultat.setAvslagsårsak(Avslagsårsak.UDEFINERT));
        }
        return behandlingsresultat;
    }

    private BehandlingResultatType utledBehandlingsresultatType(Behandlingsresultat behandlingsresultat) {
        Objects.requireNonNull(behandlingsresultat, "behandlingsresultat"); // NOSONAR
        return behandlingsresultat.isVilkårAvslått() ? BehandlingResultatType.AVSLÅTT : BehandlingResultatType.INNVILGET;
    }

}
