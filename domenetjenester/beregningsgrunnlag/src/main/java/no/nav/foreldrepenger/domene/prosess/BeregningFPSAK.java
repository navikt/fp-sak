package no.nav.foreldrepenger.domene.prosess;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulus.response.v1.beregningsgrunnlag.gui.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.mappers.fra_entitet_til_domene.FraEntitetTilBehandlingsmodellMapper;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagGUIInputFelles;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagInputFelles;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.rest.BeregningDtoTjeneste;

@ApplicationScoped
public class BeregningFPSAK implements BeregningAPI {
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningDtoTjeneste beregningDtoTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BeregningsgrunnlagInputProvider inputTjenesteProvider;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;

    BeregningFPSAK() {
        // CDI
    }

    @Inject
    public BeregningFPSAK(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                          BeregningDtoTjeneste beregningDtoTjeneste,
                          InntektArbeidYtelseTjeneste iayTjeneste,
                          BeregningsgrunnlagInputProvider inputTjenesteProvider,
                          BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.beregningDtoTjeneste = beregningDtoTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.inputTjenesteProvider = inputTjenesteProvider;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
    }

    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hent(BehandlingReferanse referanse) {
        var entitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.behandlingId());
        return entitet.map(FraEntitetTilBehandlingsmodellMapper::mapBeregningsgrunnlagGrunnlag);
    }

    @Override
    public BeregningsgrunnlagVilkårOgAkjonspunktResultat beregn(BehandlingReferanse behandlingReferanse, BehandlingStegType stegType) {
        var inputTjeneste = getInputTjeneste(behandlingReferanse.fagsakYtelseType());
        return switch (stegType) {
            case FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING -> beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsaktiviteter(inputTjeneste.lagInput(behandlingReferanse));
            case KONTROLLER_FAKTA_BEREGNING -> beregningsgrunnlagKopierOgLagreTjeneste.kontrollerFaktaBeregningsgrunnlag(inputTjeneste.lagInput(behandlingReferanse));
            case FORESLÅ_BEREGNINGSGRUNNLAG -> beregningsgrunnlagKopierOgLagreTjeneste.foreslåBeregningsgrunnlag(inputTjeneste.lagInput(behandlingReferanse));
            case FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG -> beregningsgrunnlagKopierOgLagreTjeneste.fortsettForeslåBeregningsgrunnlag(inputTjeneste.lagInput(behandlingReferanse));
            case FORESLÅ_BESTEBEREGNING -> beregningsgrunnlagKopierOgLagreTjeneste.foreslåBesteberegning(inputTjeneste.lagInput(behandlingReferanse));
            case VURDER_VILKAR_BERGRUNN -> beregningsgrunnlagKopierOgLagreTjeneste.vurderVilkårBeregningsgrunnlag(inputTjeneste.lagInput(behandlingReferanse));
            case VURDER_REF_BERGRUNN -> beregningsgrunnlagKopierOgLagreTjeneste.vurderRefusjonBeregningsgrunnlag(inputTjeneste.lagInput(behandlingReferanse));
            case FORDEL_BEREGNINGSGRUNNLAG -> beregningsgrunnlagKopierOgLagreTjeneste.fordelBeregningsgrunnlag(inputTjeneste.lagInput(behandlingReferanse));
            case FASTSETT_BEREGNINGSGRUNNLAG -> beregningsgrunnlagKopierOgLagreTjeneste.fastsettBeregningsgrunnlag(inputTjeneste.lagInput(behandlingReferanse));
            default -> throw new IllegalStateException("Ukjent beregningssteg " + stegType);
        };
    }

    @Override
    public Optional<BeregningsgrunnlagDto> hentGUIDto(BehandlingReferanse referanse) {
        var iayGrunnlagOpt = iayTjeneste.finnGrunnlag(referanse.behandlingId());
        return iayGrunnlagOpt.flatMap(iayGrunnlag -> {
            var input = getInputTjenesteGUI(referanse.fagsakYtelseType()).lagInput(referanse, iayGrunnlag);
            if (input.isPresent()) {
                return beregningDtoTjeneste.lagBeregningsgrunnlagDto(input.get());
            }
            return Optional.empty();
        });
    }

    private BeregningsgrunnlagGUIInputFelles getInputTjenesteGUI(FagsakYtelseType ytelseType) {
        return inputTjenesteProvider.getRestInputTjeneste(ytelseType);
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return inputTjenesteProvider.getTjeneste(ytelseType);
    }

}
