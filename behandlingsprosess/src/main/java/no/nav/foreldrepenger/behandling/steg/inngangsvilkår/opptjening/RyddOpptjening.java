package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType.IKKE_VURDERT;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

public class RyddOpptjening {

    private final OpptjeningRepository opptjeningRepository;
    private final BehandlingRepository behandlingRepository;
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final BehandlingskontrollKontekst kontekst;

    public RyddOpptjening(BehandlingRepositoryProvider repositoryProvider,
                          BehandlingskontrollKontekst kontekst) {
        this.opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.kontekst = kontekst;
    }

    public void ryddOpp() {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var vilkår = ryddOppVilkårsvurderinger(behandling);
        if (vilkår.isPresent()) {
            opptjeningRepository.deaktiverOpptjening(behandling);
            tilbakestillOpptjenigsperiodevilkår(behandling);
        }
    }

    public void ryddOppAktiviteter() {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        ryddOppVilkårsvurderinger(behandling);
    }

    private Optional<Vilkår> ryddOppVilkårsvurderinger(Behandling behandling) {
        var vilkårResultat = hentVilkårResultat(behandling);
        if (vilkårResultat == null) {
            return Optional.empty();
        }
        var opptjeningVilkår = vilkårResultat.getVilkårene()
            .stream()
            .filter(vilkåret -> vilkåret.getVilkårType().equals(VilkårType.OPPTJENINGSVILKÅRET))
            .findFirst();

        if (opptjeningVilkår.isPresent()) {
            var builder = VilkårResultat.builderFraEksisterende(vilkårResultat)
                .leggTilVilkår(opptjeningVilkår.get().getVilkårType(), IKKE_VURDERT);
            builder.buildFor(behandling);
            behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        }
        return opptjeningVilkår;
    }

    private VilkårResultat hentVilkårResultat(Behandling behandling) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        var vilkårResultatOpt = behandlingsresultat.map(Behandlingsresultat::getVilkårResultat);
        return vilkårResultatOpt.orElse(null);
    }

    private void tilbakestillOpptjenigsperiodevilkår(Behandling behandling) {
        var vilkårResultat = hentVilkårResultat(behandling);
        if (vilkårResultat == null) {
            return;
        }
        var opptjeningPeriodeVilkår = vilkårResultat.getVilkårene()
            .stream()
            .filter(vilkåret -> vilkåret.getVilkårType().equals(VilkårType.OPPTJENINGSPERIODEVILKÅR))
            .findFirst();
        if (opptjeningPeriodeVilkår.isPresent()) {
            var builder = VilkårResultat.builderFraEksisterende(vilkårResultat)
                .leggTilVilkår(opptjeningPeriodeVilkår.get().getVilkårType(), IKKE_VURDERT);
            builder.buildFor(behandling);
            behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        }
    }
}
