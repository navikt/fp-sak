package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@FagsakYtelseTypeRef("FP")
@BehandlingStegRef(kode = "FORS_BESTEBEREGNING")
@BehandlingTypeRef
@ApplicationScoped
public class ForeslåBesteberegningSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private OpptjeningRepository opptjeningRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    protected ForeslåBesteberegningSteg() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBesteberegningSteg(BehandlingRepository behandlingRepository,
                                     BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                     BeregningsgrunnlagInputProvider inputTjenesteProvider,
                                     BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste,
                                     OpptjeningRepository opptjeningRepository,
                                     SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
        this.opptjeningRepository = opptjeningRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId());
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        if (besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(ref) && skalBehandlesAutomatisk(ref)) {
            var input = getInputTjeneste(ref.getFagsakYtelseType()).lagInput(ref.getBehandlingId());
            BeregningsgrunnlagVilkårOgAkjonspunktResultat resultat = beregningsgrunnlagKopierOgLagreTjeneste.foreslåBesteberegning(input);
            List<AksjonspunktResultat> aksjonspunkter = resultat.getAksjonspunkter().stream().map(BeregningResultatMapper::map).collect(Collectors.toList());
            return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
        }

        return BehandleStegResultat.utførtMedAksjonspunktResultater(Collections.emptyList());
    }

    private boolean skalBehandlesAutomatisk(BehandlingReferanse ref) {
        Optional<Opptjening> opptjening = opptjeningRepository.finnOpptjening(ref.getBehandlingId());
        List<OpptjeningAktivitet> opptjeningAktiviteter = opptjening.map(Opptjening::getOpptjeningAktivitet).orElse(Collections.emptyList());
        return opptjeningAktiviteter.stream()
            .allMatch(a -> a.getAktivitetType().equals(OpptjeningAktivitetType.DAGPENGER) || a.getAktivitetType().equals(OpptjeningAktivitetType.ARBEID));
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
