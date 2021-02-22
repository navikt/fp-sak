package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType.IKKE_VURDERT;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.domene.prosess.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;

@ApplicationScoped
class BeregningsgrunnlagVilkårTjeneste {

    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    protected BeregningsgrunnlagVilkårTjeneste() {
        // CDI Proxy
    }

    @Inject
    public BeregningsgrunnlagVilkårTjeneste(BehandlingRepository behandlingRepository,
            BehandlingsresultatRepository behandlingsresultatRepository) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
    }

    void lagreVilkårresultat(BehandlingskontrollKontekst kontekst, BeregningsgrunnlagVilkårOgAkjonspunktResultat beregningsgrunnlagResultat) {
        boolean vilkårOppfylt = beregningsgrunnlagResultat.getVilkårOppfylt();
        String regelEvaluering = beregningsgrunnlagResultat.getRegelEvalueringVilkårVurdering();
        String regelInput = beregningsgrunnlagResultat.getRegelInputVilkårVurdering();
        VilkårResultat.Builder vilkårResultatBuilder = opprettVilkårsResultat(kontekst.getBehandlingId(), regelEvaluering, regelInput, vilkårOppfylt);
        if (!vilkårOppfylt) {
            Behandlingsresultat behandlingsresultat = getBehandlingsresultat(kontekst.getBehandlingId());
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            behandlingsresultat.setAvslagsårsak(Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG);
            behandlingsresultatRepository.lagre(kontekst.getBehandlingId(), behandlingsresultat);
        }
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        vilkårResultatBuilder.buildFor(behandling);
        behandlingRepository.lagre(getBehandlingsresultat(kontekst.getBehandlingId()).getVilkårResultat(), kontekst.getSkriveLås());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hent(behandlingId);
    }

    private VilkårResultat.Builder opprettVilkårsResultat(Long behandlingId, String regelEvaluering, String regelInput, boolean oppfylt) {
        VilkårResultat vilkårResultat = getBehandlingsresultat(behandlingId).getVilkårResultat();
        VilkårResultat.Builder builder = VilkårResultat.builderFraEksisterende(vilkårResultat);
        return builder
                .medVilkårResultatType(oppfylt ? VilkårResultatType.INNVILGET : VilkårResultatType.AVSLÅTT)
                .leggTilVilkårResultat(
                        VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
                        oppfylt ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT,
                        oppfylt ? VilkårUtfallMerknad.UDEFINERT : VilkårUtfallMerknad.VM_1041,
                        new Properties(),
                        oppfylt ? null : Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG,
                        false,
                        false,
                        regelEvaluering,
                        regelInput);
    }

    void ryddVedtaksresultatOgVilkår(BehandlingskontrollKontekst kontekst) {
        Optional<Behandlingsresultat> behandlingresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());
        ryddOppVilkårsvurdering(kontekst, behandlingresultatOpt);
        nullstillVedtaksresultat(kontekst, behandlingresultatOpt);
    }

    private void ryddOppVilkårsvurdering(BehandlingskontrollKontekst kontekst, Optional<Behandlingsresultat> behandlingresultatOpt) {
        Optional<VilkårResultat> vilkårResultatOpt = behandlingresultatOpt
                .map(Behandlingsresultat::getVilkårResultat);
        if (!vilkårResultatOpt.isPresent()) {
            return;
        }
        VilkårResultat vilkårResultat = vilkårResultatOpt.get();
        Optional<Vilkår> beregningsvilkåret = vilkårResultat.getVilkårene().stream()
                .filter(vilkår -> vilkår.getVilkårType().equals(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
                .findFirst();
        if (!beregningsvilkåret.isPresent()) {
            return;
        }
        VilkårResultat.Builder builder = VilkårResultat.builderFraEksisterende(vilkårResultat)
                .leggTilVilkår(beregningsvilkåret.get().getVilkårType(), IKKE_VURDERT);
        behandlingRepository.lagre(builder.buildFor(behandlingRepository.hentBehandling(kontekst.getBehandlingId())), kontekst.getSkriveLås());
    }

    private void nullstillVedtaksresultat(BehandlingskontrollKontekst kontekst, Optional<Behandlingsresultat> behandlingresultatOpt) {
        if (behandlingresultatOpt.isEmpty()
                || Objects.equals(behandlingresultatOpt.get().getBehandlingResultatType(), BehandlingResultatType.IKKE_FASTSATT)) {
            return;
        }
        Behandlingsresultat.Builder builder = Behandlingsresultat.builderEndreEksisterende(behandlingresultatOpt.get())
                .medBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT);
        behandlingsresultatRepository.lagre(kontekst.getBehandlingId(), builder.build());
    }

}
