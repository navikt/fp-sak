package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Collections;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.detaljert.BeregningsgrunnlagGrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.prosess.BeregningAPI;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.prosess.KalkulusTjeneste;

@ApplicationScoped
public class BeregningTjeneste implements BeregningAPI {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private KalkulusTjeneste kalkulusTjeneste;
    private boolean skalKalleKalkulus;

    public BeregningTjeneste() {
    }

    @Inject
    public BeregningTjeneste(BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                             BehandlingRepository behandlingRepository,
                             BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                             BeregningsgrunnlagInputProvider inputTjenesteProvider,
                             KalkulusTjeneste kalkulusTjeneste) {
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.skalKalleKalkulus = no.nav.foreldrepenger.konfig.Environment.current().isDev();
    }


    /**
     * Kjører beregning for angitt steg
     *
     * @param behandlingReferanse Behandlingreferanse
     * @param behandlingStegType  Stegtype
     * @return Resultatstruktur med aksjonspunkter og eventuell vilkårsvurdering
     */
    @Override
    public BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(BehandlingReferanse behandlingReferanse, BehandlingStegType behandlingStegType) {
        if (skalKalleKalkulus) {
            return kalkulusTjeneste.beregn(behandlingReferanse, behandlingStegType);
        } else {
            return beregnUtenKalkulus(behandlingReferanse, behandlingStegType);
        }
    }

    /**
     * Henter beregningsgrunnlag
     *
     * @param behandlingReferanse Behandlingreferanse
     * @return BeregningsgrunnlagGrunnlag
     */
    @Override
    public BeregningsgrunnlagGrunnlagDto hent(BehandlingReferanse behandlingReferanse) {
        if (skalKalleKalkulus) {
            return kalkulusTjeneste.hentGrunnlag(behandlingReferanse);
        } else {
            // TODO: Kall database og map til kontrakt?
            return null;
        }
    }

    /**
     * Kopierer beregningsgrunnlag
     *
     * @param behandlingReferanse Behandlingreferanse
     * @param behandlingStegType  Behandlingstegtype
     */
    @Override
    public void kopier(BehandlingReferanse behandlingReferanse, BehandlingStegType behandlingStegType) {
        // TODO: Utvid kalkulus sitt kopier-endepunkt med stegtype for å åpne for kopiering av fastsatt beregningsgrunnlag
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

        if (skalKalleKalkulus) {
            ryddMedKalkulus(kontekst, behandlingStegType, tilSteg);
        } else {
            ryddUtenKalkulus(kontekst, behandlingStegType, tilSteg);
        }

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

    private BeregningsgrunnlagVilkårOgAkjonspunktResultat beregnUtenKalkulus(BehandlingReferanse behandlingReferanse,
                                                                             BehandlingStegType behandlingStegType) {
        var inputTjeneste = beregningsgrunnlagInputProvider.getTjeneste(behandlingReferanse.getFagsakYtelseType());
        var input = inputTjeneste.lagInput(behandlingReferanse.getBehandlingId());
        switch (behandlingStegType) {
            case FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING:
                var aksjonspunktListe = beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(input);
                return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(aksjonspunktListe);
            case KONTROLLER_FAKTA_BEREGNING:
                aksjonspunktListe = beregningsgrunnlagKopierOgLagreTjeneste.kontrollerFaktaBeregningsgrunnlag(input);
                return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(aksjonspunktListe);
            case FORESLÅ_BEREGNINGSGRUNNLAG:
                return beregningsgrunnlagKopierOgLagreTjeneste.foreslåBeregningsgrunnlag(input);
            case FORESLÅ_BESTEBEREGNING:
                return beregningsgrunnlagKopierOgLagreTjeneste.foreslåBesteberegning(input);
            case VURDER_VILKAR_BERGRUNN:
                return beregningsgrunnlagKopierOgLagreTjeneste.vurderVilkårBeregningsgrunnlag(input);
            case VURDER_REF_BERGRUNN:
                return beregningsgrunnlagKopierOgLagreTjeneste.vurderRefusjonBeregningsgrunnlag(input);
            case FORDEL_BEREGNINGSGRUNNLAG:
                return beregningsgrunnlagKopierOgLagreTjeneste.fordelBeregningsgrunnlag(input);
            case FASTSETT_BEREGNINGSGRUNNLAG:
                beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsgrunnlag(input);
                return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(Collections.emptyList());
            default:
                throw new IllegalStateException("Ugyldig steg for beregning " + behandlingStegType);
        }
    }


    private void ryddUtenKalkulus(BehandlingskontrollKontekst kontekst, BehandlingStegType behandlingStegType, BehandlingStegType tilSteg) {
        if (!tilSteg.equals(behandlingStegType)) {
            switch (behandlingStegType) {
                case FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING:
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddFastsettSkjæringstidspunktVedTilbakeføring();
                case VURDER_VILKAR_BERGRUNN:
                    beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst);
            }
        } else {
            switch (behandlingStegType) {
                case FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING:
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst)
                        .gjenopprettFastsattBeregningAktivitetBeregningsgrunnlag();
                case KONTROLLER_FAKTA_BEREGNING:
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).gjenopprettOppdatertBeregningsgrunnlag();
                case FORESLÅ_BEREGNINGSGRUNNLAG:
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddForeslåBeregningsgrunnlagVedTilbakeføring();
                case FORESLÅ_BESTEBEREGNING:
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddForeslåBesteberegningVedTilbakeføring();
                case VURDER_VILKAR_BERGRUNN:
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddVurderVilkårBeregningsgrunnlagVedTilbakeføring();
                case VURDER_REF_BERGRUNN:
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst)
                        .ryddVurderRefusjonBeregningsgrunnlagVedTilbakeføring();
                case FORDEL_BEREGNINGSGRUNNLAG:
                    var aps = behandlingRepository.hentBehandling(kontekst.getBehandlingId()).getAksjonspunkter();
                    var harAksjonspunktSomErUtførtIUtgang = tilSteg.getAksjonspunktDefinisjonerUtgang()
                        .stream()
                        .anyMatch(ap -> aps.stream().filter(a -> a.getAksjonspunktDefinisjon().equals(ap)).anyMatch(a -> !a.erAvbrutt()));
                    beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst)
                        .ryddFordelBeregningsgrunnlagVedTilbakeføring(harAksjonspunktSomErUtførtIUtgang);
            }
        }
    }


}
