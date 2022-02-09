package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.opptjening.FrilansAvvikLoggTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.prosess.KalkulusTjeneste;

@FagsakYtelseTypeRef
@BehandlingStegRef(kode = "KOFAKBER")
@BehandlingTypeRef
@ApplicationScoped
public class KontrollerFaktaBeregningSteg implements BeregningsgrunnlagSteg {

    private boolean skalKalleKalkulus;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private FrilansAvvikLoggTjeneste frilansAvvikLoggTjeneste;
    private KalkulusTjeneste kalkulusTjeneste;
    private BeregningTjeneste beregningTjeneste;

    protected KontrollerFaktaBeregningSteg() {
        // for CDI proxy
    }

    @Inject
    public KontrollerFaktaBeregningSteg(BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                        BehandlingRepository behandlingRepository,
                                        BeregningsgrunnlagInputProvider inputTjenesteProvider,
                                        FrilansAvvikLoggTjeneste frilansAvvikLoggTjeneste,
                                        KalkulusTjeneste kalkulusTjeneste,
                                        BeregningTjeneste beregningTjeneste) {
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.frilansAvvikLoggTjeneste = frilansAvvikLoggTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.beregningTjeneste = beregningTjeneste;
        this.skalKalleKalkulus = no.nav.foreldrepenger.konfig.Environment.current().isDev();

    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var aksjonspunkter = beregningTjeneste.beregn(kontekst.getBehandlingId(), BehandlingStegType.KONTROLLER_FAKTA_BEREGNING)
            .getAksjonspunkter();

        // TFP-4427
        frilansAvvikLoggTjeneste.loggFrilansavvikVedBehov(BehandlingReferanse.fra(behandling));

        return BehandleStegResultat.utførtMedAksjonspunktResultater(
            aksjonspunkter.stream().map(BeregningAksjonspunktResultatMapper::map).collect(Collectors.toList()));
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst,
                                   BehandlingStegModell modell,
                                   BehandlingStegType tilSteg,
                                   BehandlingStegType fraSteg) {
        beregningTjeneste.rydd(kontekst, BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, tilSteg);
    }

}
