package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;

@FagsakYtelseTypeRef("*")
@BehandlingStegRef(kode = "FASTSETT_STP_BER")
@BehandlingTypeRef
@ApplicationScoped
public class FastsettBeregningsaktiviteterSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private InngangsvilkårTjeneste inngangsvilkårTjeneste;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputTjeneste;

    protected FastsettBeregningsaktiviteterSteg() {
        // for CDI proxy
    }

    @Inject
    public FastsettBeregningsaktiviteterSteg(BehandlingRepository behandlingRepository,
                                             InngangsvilkårTjeneste inngangsvilkårTjeneste,
                                             BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                             BeregningInfotrygdsakTjeneste beregningInfotrygdsakTjeneste,
                                             BeregningsgrunnlagInputProvider inputTjenesteProvider) {
        this.behandlingRepository = behandlingRepository;
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.beregningsgrunnlagInputTjeneste = Objects.requireNonNull(inputTjenesteProvider, "inputTjenestene");
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        preconditions(behandling);
        var input = getInputTjeneste(behandling.getFagsakYtelseType()).lagInput(behandling);
        List<BeregningAksjonspunktResultat> aksjonspunktResultater = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);
        if (aksjonspunktResultater == null) {
            return BehandleStegResultat.fremoverført(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT);
        } else {
            // hent på nytt i tilfelle lagret og flushet
            return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultater.stream().map(BeregningResultatMapper::map).collect(Collectors.toList()));
        }
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING.equals(tilSteg)) {
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).gjenopprettFastsattBeregningAktivitetBeregningsgrunnlag();
        } else {
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddFastsettSkjæringstidspunktVedTilbakeføring();
        }
    }

    private void preconditions(Behandling behandling) {
        VilkårResultat vilkårResultat = behandling.getBehandlingsresultat().getVilkårResultat();
        Optional<Vilkår> ikkeOppfyltVilkår = vilkårResultat.getVilkårene().stream()
            .filter(vilkår -> !vilkår.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.OPPFYLT))
            .filter(vilkår -> inngangsvilkårTjeneste.erInngangsvilkår(vilkår.getVilkårType()))
            .findFirst();
        ikkeOppfyltVilkår.ifPresent(vilkår -> {
            throw new IllegalStateException("Utvikler-feil: Det finnes vilkår som ikke er oppfylt " + vilkår.getVilkårType());
        });
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputTjeneste.getTjeneste(ytelseType);
    }
}
