package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagGUIInput;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektArbeidYtelseGrunnlagDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingAggregatDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.KravperioderPrArbeidsforholdDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.KravperioderMapper;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.MapBehandlingRef;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.OpptjeningMapperTilKalkulus;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class BeregningsgrunnlagGUIInputFelles {

    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;

    @Inject
    public BeregningsgrunnlagGUIInputFelles(BehandlingRepository behandlingRepository,
                                            InntektArbeidYtelseTjeneste iayTjeneste,
                                            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                            InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                            OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste) {
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.skjæringstidspunktTjeneste = Objects.requireNonNull(skjæringstidspunktTjeneste, "skjæringstidspunktTjeneste");
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
    }

    protected BeregningsgrunnlagGUIInputFelles() {
        // for CDI proxy
    }

    public BeregningsgrunnlagGUIInput lagInput(Behandling behandling) {
        var behandlingId = behandling.getId();
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);

        return lagInput(ref, iayGrunnlag).orElseThrow();
    }

    public BeregningsgrunnlagGUIInput lagInput(BehandlingReferanse referanse) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(referanse.behandlingId());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(referanse.behandlingId());
        return lagInput(referanse.medSkjæringstidspunkt(skjæringstidspunkt), iayGrunnlag).orElseThrow();
    }

    public Optional<BeregningsgrunnlagGUIInput> lagInput(Behandling behandling, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return lagInput(ref, iayGrunnlag);
    }

    /**
     * Returnerer input hvis data er på tilgjengelig for det, ellers
     * Optional.empty().
     */
    private Optional<BeregningsgrunnlagGUIInput> lagInput(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var inntektsmeldingDiff = inntektsmeldingTjeneste.hentInntektsmeldingDiffFraOriginalbehandling(ref);
        var inntektsmeldingDiffDto = inntektsmeldingDiff.stream().map(IAYMapperTilKalkulus::mapInntektsmeldingDto)
                .collect(Collectors.toList());
        var iayGrunnlagDtoUtenIMDiff = IAYMapperTilKalkulus.mapGrunnlag(iayGrunnlag, ref.aktørId());

        var ytelseGrunnlag = getYtelsespesifiktGrunnlag(ref);
        var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(ref, iayGrunnlag);
        var inntektsmeldinger = iayGrunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
            .orElse(Collections.emptyList());
        var mappetOpptjening = opptjeningAktiviteter.map(opptjeningAktiviteter1 -> OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(opptjeningAktiviteter1, inntektsmeldinger)).orElse(null);
        InntektArbeidYtelseGrunnlagDto iayGrunnlagDto;
        if (!inntektsmeldingDiffDto.isEmpty()) {
            iayGrunnlagDto = settInntektsmeldingDiffPåIAYGrunnlag(iayGrunnlagDtoUtenIMDiff, inntektsmeldingDiffDto);
        } else {
            iayGrunnlagDto = iayGrunnlagDtoUtenIMDiff;
        }

        List<KravperioderPrArbeidsforholdDto> kravperioder = mapKravperioder(ref, iayGrunnlag);
        var input = new BeregningsgrunnlagGUIInput(
            MapBehandlingRef.mapRef(ref),
            iayGrunnlagDto,
            kravperioder,
            mappetOpptjening,
            ytelseGrunnlag);
        return Optional.of(input);
    }

    private List<KravperioderPrArbeidsforholdDto> mapKravperioder(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var inntektsmeldinger = iayGrunnlag.getInntektsmeldinger().map(InntektsmeldingAggregat::getAlleInntektsmeldinger).orElse(Collections.emptyList());
        return KravperioderMapper.map(ref, inntektsmeldinger, iayGrunnlag);
    }

    private InntektArbeidYtelseGrunnlagDto settInntektsmeldingDiffPåIAYGrunnlag(InntektArbeidYtelseGrunnlagDto iayGrunnlagDto,
            List<InntektsmeldingDto> inntektsmeldingDiffDto) {
        var inntektsmeldingDtos = iayGrunnlagDto.getInntektsmeldinger()
                .map(InntektsmeldingAggregatDto::getAlleInntektsmeldinger)
                .orElse(Collections.emptyList());
        var builder = InntektArbeidYtelseGrunnlagDtoBuilder.oppdatere(iayGrunnlagDto)
                .medInntektsmeldinger(inntektsmeldingDtos, inntektsmeldingDiffDto);
        return builder.build();
    }

    /**
     * Returnerer input hvis data er på tilgjengelig for det, ellers
     * Optional.empty().
     */
    public BeregningsgrunnlagGUIInput lagInput(Long behandlingId) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return lagInput(behandling, iayGrunnlag).orElseThrow();
    }

    public abstract YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref);
}
