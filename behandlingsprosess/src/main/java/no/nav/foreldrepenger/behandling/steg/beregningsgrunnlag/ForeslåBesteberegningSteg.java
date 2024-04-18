package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.mappers.BeregningAksjonspunktResultatMapper;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@BehandlingStegRef(BehandlingStegType.FORESLÅ_BESTEBEREGNING)
@BehandlingTypeRef
@ApplicationScoped
public class ForeslåBesteberegningSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    protected ForeslåBesteberegningSteg() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBesteberegningSteg(BehandlingRepository behandlingRepository,
                                     BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                     BeregningsgrunnlagInputProvider inputTjenesteProvider,
                                     BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste,
                                     SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        var input = getInputTjeneste(ref.fagsakYtelseType()).lagInput(ref.behandlingId());
        if (skalBeregnesAutomatisk(ref)) {
            var resultat = beregningsgrunnlagKopierOgLagreTjeneste.foreslåBesteberegning(input);
            var aksjonspunkter = resultat.getAksjonspunkter().stream().map(BeregningAksjonspunktResultatMapper::map).collect(Collectors.toList());

            if (besteberegningFødendeKvinneTjeneste.trengerManuellKontrollAvAutomatiskBesteberegning(ref)) {
                aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_BESTEBEREGNING));
            }

            return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean skalBeregnesAutomatisk(BehandlingReferanse ref) {
        return besteberegningFødendeKvinneTjeneste.kvalifisererTilAutomatiskBesteberegning(ref);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (tilSteg.equals(BehandlingStegType.FORESLÅ_BESTEBEREGNING)) {
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddForeslåBesteberegningVedTilbakeføring();
        }
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputProvider.getTjeneste(ytelseType);
    }


}
