package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulus_til_modell.FraKalkulusMapper;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.prosess.KalkulusTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class BeregningKalkulus implements BeregningAPI {

    private BehandlingRepository behandlingRepository;
    private Instance<SkjæringstidspunktTjeneste> skjæringstidspunktTjeneste;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private KalkulusTjeneste kalkulusTjeneste;

    public BeregningKalkulus() {
    }

    @Inject
    public BeregningKalkulus(BehandlingRepository behandlingRepository,
                             Instance<SkjæringstidspunktTjeneste> skjæringstidspunktTjeneste,
                             BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                             KalkulusTjeneste kalkulusTjeneste) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.behandlingRepository = behandlingRepository;
    }


    /**
     * Kjører beregning for angitt steg
     *
     * @param behandlingId       behandlingId
     * @param behandlingStegType Stegtype
     * @return Resultatstruktur med aksjonspunkter og eventuell vilkårsvurdering
     */
    @Override
    public BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(Long behandlingId, BehandlingStegType behandlingStegType) {
        var behandlingReferanse = lagReferanseMedSkjæringstidspunkt(behandlingId);
        return kalkulusTjeneste.beregn(behandlingReferanse, behandlingStegType);
    }

    /**
     * Henter beregningsgrunnlag
     *
     * @param behandlingId behandlingId
     * @return BeregningsgrunnlagGrunnlag
     */
    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hent(Long behandlingId) {
        var behandlingReferanse = lagReferanseMedSkjæringstidspunkt(behandlingId);
        return kalkulusTjeneste.hentGrunnlag(behandlingReferanse).map(FraKalkulusMapper::mapBeregningsgrunnlagGrunnlag);
    }

    @Override
    public Optional<BeregningsgrunnlagDto> hentForGUI(Long behandlingId) {
        var behandlingReferanse = lagReferanseMedSkjæringstidspunkt(behandlingId);
        return kalkulusTjeneste.hentDtoForVisning(behandlingReferanse);

    }

    /**
     * Kopierer beregningsgrunnlag
     *
     * @param behandlingId behandlingId
     */
    @Override
    public void kopier(Long behandlingId) {
        kalkulusTjeneste.kopier(behandlingId);
    }

    /**
     * Rydder beregningsgrunnlag og tilhørende resultat for et hopp bakover kall i oppgitt steg
     *
     * @param kontekst           Behandlingskontrollkontekst
     * @param behandlingStegType steget ryddkallet kjøres fra
     * @param tilSteg            Siste steg i hopp bakover transisjonen
     */
    @Override
    public void rydd(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType, BehandlingStegType tilSteg) {
        ryddMedKalkulus(kontekst, behandlingStegType, tilSteg);
    }

    private void ryddMedKalkulus(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType, BehandlingStegType tilSteg) {
        if (!behandlingStegType.equals(tilSteg)) {
            switch (behandlingStegType) {
                case FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING:
                    kalkulusTjeneste.deaktiver(kontekst.getBehandlingId());
                case VURDER_VILKAR_BERGRUNN:
                    beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst);
            }
        }
    }

    private BehandlingReferanse lagReferanseMedSkjæringstidspunkt(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkt = getSkjæringstidspunkt(behandlingId, behandling.getFagsakYtelseType());
        return BehandlingReferanse.fra(behandling).medSkjæringstidspunkt(skjæringstidspunkt);
    }

    private Skjæringstidspunkt getSkjæringstidspunkt(Long behandlingId, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(skjæringstidspunktTjeneste, fagsakYtelseType)
            .map(t -> t.getSkjæringstidspunkter(behandlingId))
            .orElse(null);
    }


}
