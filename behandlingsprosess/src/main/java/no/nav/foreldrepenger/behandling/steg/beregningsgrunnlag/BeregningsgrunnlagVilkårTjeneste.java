package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;

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
        var vilkårResultatBuilder = opprettVilkårsResultat(kontekst.getBehandlingId(), beregningsgrunnlagResultat, vilkårOppfylt);
        if (!vilkårOppfylt) {
            var behandlingsresultat = getBehandlingsresultat(kontekst.getBehandlingId());
            Behandlingsresultat.builderEndreEksisterende(behandlingsresultat).medBehandlingResultatType(BehandlingResultatType.AVSLÅTT);
            behandlingsresultat.setAvslagsårsak(Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG);
            behandlingsresultatRepository.lagre(kontekst.getBehandlingId(), behandlingsresultat);
        }
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        vilkårResultatBuilder.buildFor(behandling);
        behandlingRepository.lagre(getBehandlingsresultat(kontekst.getBehandlingId()).getVilkårResultat(), kontekst.getSkriveLås());
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hent(behandlingId);
    }

    private VilkårResultat.Builder opprettVilkårsResultat(Long behandlingId, BeregningsgrunnlagVilkårOgAkjonspunktResultat resultat, boolean oppfylt) {
        var vilkårResultat = getBehandlingsresultat(behandlingId).getVilkårResultat();
        var builder = VilkårResultat.builderFraEksisterende(vilkårResultat);
        var vilkårBuilder = builder.getVilkårBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .medRegelEvaluering(resultat.getRegelEvalueringVilkårVurdering())
            .medRegelInput(resultat.getRegelInputVilkårVurdering())
            .medRegelVersjon(resultat.getRegelVersjonVilkårVurdering());
        if (oppfylt) {
            vilkårBuilder.medVilkårUtfall(VilkårUtfallType.OPPFYLT, VilkårUtfallMerknad.UDEFINERT);
        } else {
            vilkårBuilder.medVilkårUtfall(VilkårUtfallType.IKKE_OPPFYLT, VilkårUtfallMerknad.VM_1041);
        }
        return builder.leggTilVilkår(vilkårBuilder);
    }

    void ryddVedtaksresultatOgVilkår(BehandlingskontrollKontekst kontekst) {
        var behandlingresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());
        ryddOppVilkårsvurdering(kontekst, behandlingresultatOpt);
        nullstillVedtaksresultat(kontekst, behandlingresultatOpt);
    }

    private void ryddOppVilkårsvurdering(BehandlingskontrollKontekst kontekst, Optional<Behandlingsresultat> behandlingresultatOpt) {
        var vilkårResultatOpt = behandlingresultatOpt
                .map(Behandlingsresultat::getVilkårResultat);
        if (vilkårResultatOpt.isEmpty()) {
            return;
        }
        var vilkårResultat = vilkårResultatOpt.get();
        vilkårResultat.getVilkårene().stream()
            .filter(vilkår -> vilkår.getVilkårType().equals(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .findFirst()
            .ifPresent(bv -> {
                var builder = VilkårResultat.builderFraEksisterende(vilkårResultat)
                    .leggTilVilkårIkkeVurdert(bv.getVilkårType());
                behandlingRepository.lagre(builder.buildFor(behandlingRepository.hentBehandling(kontekst.getBehandlingId())), kontekst.getSkriveLås());
        });
    }

    private void nullstillVedtaksresultat(BehandlingskontrollKontekst kontekst, Optional<Behandlingsresultat> behandlingresultatOpt) {
        if (behandlingresultatOpt.isEmpty()
                || Objects.equals(behandlingresultatOpt.get().getBehandlingResultatType(), BehandlingResultatType.IKKE_FASTSATT)) {
            return;
        }
        var builder = Behandlingsresultat.builderEndreEksisterende(behandlingresultatOpt.get())
                .medBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT);
        behandlingsresultatRepository.lagre(kontekst.getBehandlingId(), builder.build());
    }

}
