package no.nav.foreldrepenger.inngangsvilkaar;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
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
        Instance<Inngangsvilkår> selected = alleInngangsvilkår.select(new VilkårTypeRefLiteral(vilkårType.getKode()));
        if (selected.isAmbiguous()) {
            selected = selected.select(new FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral(fagsakYtelseType.getKode()));
            if (selected.isAmbiguous()) {
                throw new IllegalArgumentException("Mer enn en implementasjon funnet for vilkårtype:" + vilkårType);
            }
        } else if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for vilkårtype:" + vilkårType);
        }
        Inngangsvilkår minInstans = selected.get();
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
        Instance<Inngangsvilkår> selected = alleInngangsvilkår.select(new VilkårTypeRefLiteral(vilkårType.getKode()));
        return !selected.isUnsatisfied();
    }

    /**
     * Overstyr søkers opplysningsplikt.
     */
    public void overstyrAksjonspunktForSøkersopplysningsplikt(Long behandlingId, VilkårUtfallType utfall, BehandlingskontrollKontekst kontekst) {
        Avslagsårsak avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON;
        VilkårType vilkårType = VilkårType.SØKERSOPPLYSNINGSPLIKT;

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        VilkårResultat vilkårResultat = getBehandlingsresultat(behandlingId).getVilkårResultat();
        VilkårResultat.Builder builder = VilkårResultat.builderFraEksisterende(vilkårResultat);

        if (Objects.equals(VilkårUtfallType.OPPFYLT, utfall)) {
            builder.overstyrVilkår(vilkårType, utfall, null);
            if (!finnesOverstyrteAvviste(vilkårResultat, vilkårType)) {
                builder.medVilkårResultatType(VilkårResultatType.IKKE_FASTSATT);
            }
        } else {
            builder.overstyrVilkår(vilkårType, utfall, avslagsårsak);
            builder.medVilkårResultatType(VilkårResultatType.AVSLÅTT);
        }
        builder.buildFor(behandling);
        behandlingRepository.lagre(getBehandlingsresultat(behandlingId).getVilkårResultat(), kontekst.getSkriveLås());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    /**
     * Overstyr gitt aksjonspunkt på Inngangsvilkår.
     */
    public void overstyrAksjonspunkt(Long behandlingId, VilkårType vilkårType, VilkårUtfallType utfall, String avslagsårsakKode,
                                     BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        VilkårResultat vilkårResultat = getBehandlingsresultat(behandlingId).getVilkårResultat();
        VilkårResultat.Builder builder = VilkårResultat.builderFraEksisterende(vilkårResultat);

        Avslagsårsak avslagsårsak = finnAvslagsårsak(avslagsårsakKode, utfall);
        builder.overstyrVilkår(vilkårType, utfall, avslagsårsak);
        if (utfall.equals(VilkårUtfallType.IKKE_OPPFYLT)) {
            builder.medVilkårResultatType(VilkårResultatType.AVSLÅTT);
        } else if (utfall.equals(VilkårUtfallType.OPPFYLT)) {
            if (!finnesOverstyrteAvviste(vilkårResultat, vilkårType)) {
                builder.medVilkårResultatType(VilkårResultatType.IKKE_FASTSATT);
            }
        }
        VilkårResultat resultat = builder.buildFor(behandling);
        behandlingRepository.lagre(resultat, kontekst.getSkriveLås());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    public Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hent(behandlingId);
    }

    private boolean finnesOverstyrteAvviste(VilkårResultat vilkårResultat, VilkårType vilkårType) {
        return vilkårResultat.getVilkårene().stream()
            .filter(vilkår -> !vilkår.getVilkårType().equals(vilkårType))
            .anyMatch(vilkår -> vilkår.erOverstyrt() && vilkår.erIkkeOppfylt());
    }

    private Avslagsårsak finnAvslagsårsak(String avslagsÅrsakKode, VilkårUtfallType utfall) {
        Avslagsårsak avslagsårsak;
        if (avslagsÅrsakKode == null || utfall.equals(VilkårUtfallType.OPPFYLT)) {
            avslagsårsak = Avslagsårsak.UDEFINERT;
        } else {
            avslagsårsak = Avslagsårsak.fraKode(avslagsÅrsakKode);
        }
        return avslagsårsak;
    }
}
