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
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagGUIInputFelles;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.BeregningsgrunnlagInputProvider;
import no.nav.foreldrepenger.domene.mappers.fra_entitet_til_domene.FraEntitetTilBehandlingsmodellMapper;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.rest.BeregningDtoTjeneste;

@ApplicationScoped
public class BeregningFPSAK implements BeregningAPI {
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningDtoTjeneste beregningDtoTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BeregningsgrunnlagInputProvider inputTjenesteProvider;

    BeregningFPSAK() {
        // CDI
    }

    @Inject
    public BeregningFPSAK(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                          BeregningDtoTjeneste beregningDtoTjeneste,
                          InntektArbeidYtelseTjeneste iayTjeneste,
                          BeregningsgrunnlagInputProvider inputTjenesteProvider) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.beregningDtoTjeneste = beregningDtoTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.inputTjenesteProvider = inputTjenesteProvider;
    }

    @Override
    public Optional<BeregningsgrunnlagGrunnlag> hent(BehandlingReferanse referanse) {
        var entitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(referanse.behandlingId());
        return entitet.map(FraEntitetTilBehandlingsmodellMapper::mapBeregningsgrunnlagGrunnlag);
    }

    @Override
    public void beregn(BehandlingReferanse behandlingReferanse, BehandlingStegType stegType) {

    }

    @Override
    public Optional<BeregningsgrunnlagDto> hentGUIDto(BehandlingReferanse referanse) {
        var iayGrunnlagOpt = iayTjeneste.finnGrunnlag(referanse.behandlingId());
        return iayGrunnlagOpt.flatMap(iayGrunnlag -> {
            var input = getInputTjeneste(referanse.fagsakYtelseType()).lagInput(referanse, iayGrunnlag);
            if (input.isPresent()) {
                return beregningDtoTjeneste.lagBeregningsgrunnlagDto(input.get());
            }
            return Optional.empty();
        });
    }

    private BeregningsgrunnlagGUIInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return inputTjenesteProvider.getRestInputTjeneste(ytelseType);
    }

}
