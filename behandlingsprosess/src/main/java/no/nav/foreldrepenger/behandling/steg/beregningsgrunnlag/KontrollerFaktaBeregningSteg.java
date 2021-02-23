package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.List;
import java.util.Objects;
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
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.BeregningsgrunnlagKopierOgLagreTjeneste;

@FagsakYtelseTypeRef("*")
@BehandlingStegRef(kode = "KOFAKBER")
@BehandlingTypeRef
@ApplicationScoped
public class KontrollerFaktaBeregningSteg implements BeregningsgrunnlagSteg {

    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;

    protected KontrollerFaktaBeregningSteg() {
        // for CDI proxy
    }

    @Inject
    public KontrollerFaktaBeregningSteg(BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                        BehandlingRepository behandlingRepository,
                                        BeregningsgrunnlagInputProvider inputTjenesteProvider) {
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var input = getInputTjeneste(behandling.getFagsakYtelseType()).lagInput(behandling);
        List<BeregningAksjonspunktResultat> aksjonspunkter = beregningsgrunnlagKopierOgLagreTjeneste.kontrollerFaktaBeregningsgrunnlag(input);
        return BehandleStegResultat
                .utførtMedAksjonspunktResultater(aksjonspunkter.stream().map(BeregningResultatMapper::map).collect(Collectors.toList()));
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        if (BehandlingStegType.KONTROLLER_FAKTA_BEREGNING.equals(tilSteg)) {
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).gjenopprettOppdatertBeregningsgrunnlag();
        }
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputProvider.getTjeneste(ytelseType);
    }
}
