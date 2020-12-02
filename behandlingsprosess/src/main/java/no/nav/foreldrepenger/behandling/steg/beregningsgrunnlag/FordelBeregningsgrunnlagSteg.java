package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import static no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.output.BeregningAksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.BeregningsgrunnlagKopierOgLagreTjeneste;

@FagsakYtelseTypeRef("*")
@BehandlingStegRef(kode = "FORDEL_BERGRUNN")
@BehandlingTypeRef
@ApplicationScoped
public class FordelBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;

    protected FordelBeregningsgrunnlagSteg() {
        // CDI Proxy
    }

    @Inject
    public FordelBeregningsgrunnlagSteg(BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
            BehandlingRepository behandlingRepository,
            BeregningsgrunnlagInputProvider inputTjenesteProvider,
            BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste) {
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var input = getInputTjeneste(behandling.getFagsakYtelseType()).lagInput(behandlingId);
        // TODO Når alle saker som står i ap 5059 er kjørt videre eller stilt tilbake
        // kan innholdet i else fjernes,
        // vi har flyttet vilkårsvurdering og periodisiering et steg tilbake i
        // prosessen.
        // Altså når erBGVilkårVurdert(behandling) alltid gir true
        if (erBGVilkårVurdert(behandling)) {
            var beregningsgrunnlagResultat = beregningsgrunnlagKopierOgLagreTjeneste.fordelBeregningsgrunnlagUtenVilkårOgPeriodisering(input);
            List<BeregningAksjonspunktResultat> aksjonspunkter = beregningsgrunnlagResultat.getAksjonspunkter();
            return BehandleStegResultat
                    .utførtMedAksjonspunktResultater(aksjonspunkter.stream().map(BeregningResultatMapper::map).collect(Collectors.toList()));
        } else {
            var beregningsgrunnlagResultat = beregningsgrunnlagKopierOgLagreTjeneste.fordelBeregningsgrunnlag(input);
            beregningsgrunnlagVilkårTjeneste.lagreVilkårresultat(kontekst, beregningsgrunnlagResultat);
            if (Boolean.FALSE.equals(beregningsgrunnlagResultat.getVilkårOppfylt())) {
                return BehandleStegResultat.fremoverført(FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT);
            } else {
                List<BeregningAksjonspunktResultat> aksjonspunkter = beregningsgrunnlagResultat.getAksjonspunkter();
                return BehandleStegResultat
                        .utførtMedAksjonspunktResultater(aksjonspunkter.stream().map(BeregningResultatMapper::map).collect(Collectors.toList()));
            }
        }
    }

    public boolean erBGVilkårVurdert(Behandling behandling) {
        VilkårResultat vilkårResultat = behandling.getBehandlingsresultat() == null ? null : behandling.getBehandlingsresultat().getVilkårResultat();
        List<Vilkår> vilkårene = vilkårResultat == null ? Collections.emptyList() : vilkårResultat.getVilkårene();
        Optional<Vilkår> bgVilkår = vilkårene.stream().filter(vk -> VilkårType.BEREGNINGSGRUNNLAGVILKÅR.equals(vk.getVilkårType())).findFirst();
        if (bgVilkår.isEmpty()) {
            return false;
        }
        VilkårResultatType vilkårResultatType = bgVilkår.get().getVilkårResultat().getVilkårResultatType();
        return vilkårResultatType.equals(VilkårResultatType.INNVILGET) || vilkårResultatType.equals(VilkårResultatType.AVSLÅTT);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        if (tilSteg.equals(BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG)) {
            Set<Aksjonspunkt> aps = behandlingRepository.hentBehandling(kontekst.getBehandlingId()).getAksjonspunkter();
            boolean harAksjonspunktSomErUtførtIUtgang = tilSteg.getAksjonspunktDefinisjonerUtgang().stream()
                    .anyMatch(ap -> aps.stream().filter(a -> a.getAksjonspunktDefinisjon().equals(ap))
                            .anyMatch(a -> !a.erAvbrutt()));
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst)
                    .ryddFordelBeregningsgrunnlagVedTilbakeføring(harAksjonspunktSomErUtførtIUtgang);
        }
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputProvider.getTjeneste(ytelseType);
    }
}
