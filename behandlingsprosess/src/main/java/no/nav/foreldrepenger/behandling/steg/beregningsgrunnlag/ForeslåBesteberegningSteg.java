package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
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
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@FagsakYtelseTypeRef("FP")
@BehandlingStegRef(kode = "FORS_BESTEBEREGNING")
@BehandlingTypeRef
@ApplicationScoped
public class ForeslåBesteberegningSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
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
                                     SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                     BeregningsgrunnlagRepository beregningsgrunnlagRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
        this.opptjeningRepository = opptjeningRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        var input = getInputTjeneste(ref.getFagsakYtelseType()).lagInput(ref.getBehandlingId());
        if (skalBeregnesAutomatisk(ref, input)) {
            var resultat = beregningsgrunnlagKopierOgLagreTjeneste.foreslåBesteberegning(input);
            var aksjonspunkter = resultat.getAksjonspunkter().stream().map(BeregningResultatMapper::map).collect(Collectors.toList());
            aksjonspunkter.add(opprettKontrollpunkt());
            return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
        }

        return BehandleStegResultat.utførtMedAksjonspunktResultater(Collections.emptyList());
    }

    private AksjonspunktResultat opprettKontrollpunkt() {
        return AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_AUTOMATISK_BESTEBEREGNING);
    }

    private boolean skalBeregnesAutomatisk(BehandlingReferanse ref, BeregningsgrunnlagInput input) {
        var kvalifisererTilBesteberegning = besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(ref);
        if (!kvalifisererTilBesteberegning) {
            return false;
        }
        var kanBehandlesAutomatisk = kanBehandlesAutomatisk(ref, input);
        var erManueltVurdert = erBesteberegningManueltVurdert(ref);
        return kanBehandlesAutomatisk && !erManueltVurdert;
    }

    private boolean erBesteberegningManueltVurdert(BehandlingReferanse ref) {
        var beregningsgrunnlagEntitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(ref.getBehandlingId());
        return beregningsgrunnlagEntitet.map(BeregningsgrunnlagEntitet::getFaktaOmBeregningTilfeller)
            .orElse(Collections.emptyList()).stream().anyMatch(tilf ->tilf.equals(FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING));
    }

    private boolean kanBehandlesAutomatisk(BehandlingReferanse ref, BeregningsgrunnlagInput input) {
        var opptjening = opptjeningRepository.finnOpptjening(ref.getBehandlingId());
        var opptjeningAktiviteter = opptjening.map(Opptjening::getOpptjeningAktivitet).orElse(Collections.emptyList());
        var harKunDpEllerArbeidIOpptjeningsperioden = opptjeningAktiviteter.stream().allMatch(a -> a.getAktivitetType().equals(OpptjeningAktivitetType.DAGPENGER) || a.getAktivitetType().equals(OpptjeningAktivitetType.ARBEID));
        return input.isEnabled("automatisk-besteberegning", false) &&
            harKunDpEllerArbeidIOpptjeningsperioden;
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
