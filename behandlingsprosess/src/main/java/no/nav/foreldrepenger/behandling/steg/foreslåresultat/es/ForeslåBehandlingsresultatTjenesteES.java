package no.nav.foreldrepenger.behandling.steg.foreslåresultat.es;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingEndring;
import no.nav.foreldrepenger.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.AvslagsårsakMapper;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
public class ForeslåBehandlingsresultatTjenesteES implements ForeslåBehandlingsresultatTjeneste {
    private RevurderingEndring revurderingEndring;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingRepository behandlingRepository;

    ForeslåBehandlingsresultatTjenesteES() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBehandlingsresultatTjenesteES(BehandlingsresultatRepository behandlingsresultatRepository,
                                                BehandlingRepository behandlingRepository,
                                                @FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD) RevurderingEndring revurderingEndring) {
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.revurderingEndring = revurderingEndring;
    }

    @Override
    public Behandlingsresultat foreslåBehandlingsresultat(BehandlingReferanse ref) {
        var behandlingsresultat = behandlingsresultatRepository.hent(ref.behandlingId());
        var behandlingResultatType = utledBehandlingsresultatType(behandlingsresultat);
        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(behandlingResultatType);
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
        if (revurderingEndring.erRevurderingMedUendretUtfall(behandling, behandlingResultatType)) {
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.INGEN_ENDRING);
        }
        if (BehandlingResultatType.AVSLÅTT.equals(behandlingResultatType)) {
            behandlingsresultat.getVilkårResultat().hentIkkeOppfyltVilkår()
                .map(AvslagsårsakMapper::finnAvslagsårsak)
                .ifPresent(behandlingsresultat::setAvslagsårsak);
        } else {
            // Må nullstille avslagårsak (for symmetri med setting avslagsårsak ovenfor,
            // hvor avslagårsak kopieres fra et vilkår)
            Optional.ofNullable(behandlingsresultat.getAvslagsårsak())
                .ifPresent(ufjernetÅrsak -> behandlingsresultat.setAvslagsårsak(Avslagsårsak.UDEFINERT));
        }
        return behandlingsresultat;
    }

    private BehandlingResultatType utledBehandlingsresultatType(Behandlingsresultat behandlingsresultat) {
        Objects.requireNonNull(behandlingsresultat, "behandlingsresultat");
        return behandlingsresultat.isInngangsVilkårAvslått() ? BehandlingResultatType.AVSLÅTT : BehandlingResultatType.INNVILGET;
    }

}
