package no.nav.foreldrepenger.inngangsvilkaar;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårTypeRef.VilkårTypeRefLiteral;

/**
 * Denne angir implementasjon som skal brukes for en gitt {@link VilkårType} slik at {@link Vilkår} og
 * {@link VilkårResultat} kan fastsettes.
 */
@ApplicationScoped
public class InngangsvilkårTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(InngangsvilkårTjeneste.class);

    private Instance<Inngangsvilkår> alleInngangsvilkår;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    InngangsvilkårTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InngangsvilkårTjeneste(@Any Instance<Inngangsvilkår> alleInngangsvilkår, BehandlingRepositoryProvider repositoryProvider) {
        this.alleInngangsvilkår = alleInngangsvilkår;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
    }

    /**
     * Finn {@link Inngangsvilkår} for angitt {@link VilkårType}. Husk at denne må closes når du er ferdig med den.
     */
    public Inngangsvilkår finnVilkår(VilkårType vilkårType, FagsakYtelseType fagsakYtelseType) {
        var selected = alleInngangsvilkår.select(new VilkårTypeRefLiteral(vilkårType));
        if (selected.isAmbiguous()) {
            selected = selected.select(new FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral(fagsakYtelseType));
            if (selected.isAmbiguous()) {
                throw new IllegalArgumentException("Mer enn en implementasjon funnet for vilkårtype:" + vilkårType);
            }
        } else if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for vilkårtype:" + vilkårType);
        }
        var minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException(
                "Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return minInstans;
    }

    /**
     * Vurder om et angitt {@link VilkårType} er et {@link Inngangsvilkår}
     *
     * @param vilkårType en {@link VilkårType}
     * @return true hvis {@code vilkårType} er et {@link Inngangsvilkår}
     */
    public boolean erInngangsvilkår(VilkårType vilkårType) {
        var selected = alleInngangsvilkår.select(new VilkårTypeRefLiteral(vilkårType));
        return !selected.isUnsatisfied();
    }

    /**
     * Overstyr søkers opplysningsplikt.
     */
    public void overstyrAksjonspunktForSøkersopplysningsplikt(Long behandlingId, VilkårUtfallType utfall) {
        var vilkårType = VilkårType.SØKERSOPPLYSNINGSPLIKT;

        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var vilkårResultat = getBehandlingsresultat(behandlingId).getVilkårResultat();
        var builder = VilkårResultat.builderFraEksisterende(vilkårResultat);

        if (Objects.equals(VilkårUtfallType.OPPFYLT, utfall)) {
            builder.overstyrVilkår(vilkårType, VilkårUtfallType.OPPFYLT, Avslagsårsak.UDEFINERT);
        } else {
            builder.overstyrVilkår(vilkårType, VilkårUtfallType.IKKE_OPPFYLT,  Avslagsårsak.MANGLENDE_DOKUMENTASJON);
        }
        builder.buildFor(behandling);
        var lås = behandlingRepository.taSkriveLås(behandlingId);
        behandlingRepository.lagre(getBehandlingsresultat(behandlingId).getVilkårResultat(), lås);
        behandlingRepository.lagre(behandling, lås);
    }

    /**
     * Overstyr gitt aksjonspunkt på Inngangsvilkår.
     */
    public void overstyrAksjonspunkt(Long behandlingId, VilkårType vilkårType, VilkårUtfallType utfall, Avslagsårsak avslagsårsak) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var vilkårResultat = getBehandlingsresultat(behandlingId).getVilkårResultat();
        var builder = VilkårResultat.builderFraEksisterende(vilkårResultat);

        builder.overstyrVilkår(vilkårType, utfall, avslagsårsak);
        if (utfall.equals(VilkårUtfallType.IKKE_OPPFYLT)) {
            if (avslagsårsak == null || Avslagsårsak.UDEFINERT.equals(avslagsårsak))
                LOG.warn("Overstyrer til IKKE OPPFYLT uten gyldig avslagskode, behandling {} vilkårtype {} kode {}", behandlingId, vilkårType, avslagsårsak);
        }
        var resultat = builder.buildFor(behandling);
        var lås = behandlingRepository.taSkriveLås(behandlingId);
        behandlingRepository.lagre(resultat, lås);
        behandlingRepository.lagre(behandling, lås);
    }

    public Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hent(behandlingId);
    }

}
