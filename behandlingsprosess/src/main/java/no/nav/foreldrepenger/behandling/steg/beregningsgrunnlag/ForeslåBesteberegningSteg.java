package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.ArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@BehandlingStegRef(BehandlingStegType.FORESLÅ_BESTEBEREGNING)
@BehandlingTypeRef
@ApplicationScoped
public class ForeslåBesteberegningSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BeregningTjeneste beregningTjeneste;

    protected ForeslåBesteberegningSteg() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBesteberegningSteg(BehandlingRepository behandlingRepository,
                                     BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste,
                                     SkjæringstidspunktTjeneste skjæringstidspunktTjeneste, BeregningTjeneste beregningTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.beregningTjeneste = beregningTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        if (skalBeregnesAutomatisk(ref, skjæringstidspunkt)) {
            var resultat = beregningTjeneste.beregn(ref, BehandlingStegType.FORESLÅ_BESTEBEREGNING);
            var aksjonspunkter = new ArrayList<>(resultat.getAksjonspunkter());

            if (besteberegningFødendeKvinneTjeneste.trengerManuellKontrollAvAutomatiskBesteberegning(ref)) {
                aksjonspunkter.add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.MANUELL_KONTROLL_AV_BESTEBEREGNING));
            }

            return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean skalBeregnesAutomatisk(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        return besteberegningFødendeKvinneTjeneste.kvalifisererTilAutomatiskBesteberegning(ref, stp);
    }
}
