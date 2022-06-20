package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
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
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.KravperioderMapper;
import no.nav.foreldrepenger.domene.prosess.KalkulusKonfigInjecter;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.MapBehandlingRef;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.OpptjeningMapperTilKalkulus;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public abstract class BeregningsgrunnlagInputFelles {

    protected BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private KalkulusKonfigInjecter kalkulusKonfigInjecter;

    @Inject
    public BeregningsgrunnlagInputFelles(BehandlingRepository behandlingRepository,
            InntektArbeidYtelseTjeneste iayTjeneste,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
            OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
            InntektsmeldingTjeneste inntektsmeldingTjeneste,
            KalkulusKonfigInjecter kalkulusKonfigInjecter) {
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.skjæringstidspunktTjeneste = Objects.requireNonNull(skjæringstidspunktTjeneste, "skjæringstidspunktTjeneste");
        this.opptjeningForBeregningTjeneste = Objects.requireNonNull(opptjeningForBeregningTjeneste, "opptjeningForBeregningTjeneste");
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.kalkulusKonfigInjecter = kalkulusKonfigInjecter;
    }

    protected BeregningsgrunnlagInputFelles() {
        // for CDI proxy
    }

    public abstract YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref);

    /** Returnerer input hvis data er på tilgjengelig for det, ellers Exception. */
    public BeregningsgrunnlagInput lagInput(Long behandlingId) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return lagInput(behandling, iayGrunnlag);
    }

    public BeregningsgrunnlagInput lagInput(Behandling behandling) {
        var behandlingId = behandling.getId();
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);

        return lagInput(ref, iayGrunnlag);
    }

    public BeregningsgrunnlagInput lagInput(BehandlingReferanse referanse) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(referanse.behandlingId());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(referanse.behandlingId());
        return lagInput(referanse.medSkjæringstidspunkt(skjæringstidspunkt), iayGrunnlag);
    }

    private BeregningsgrunnlagInput lagInput(Behandling behandling, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return lagInput(ref, iayGrunnlag);
    }

    /** Returnerer input hvis data er på tilgjengelig for det, ellers Exception. */
    private BeregningsgrunnlagInput lagInput(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(ref, iayGrunnlag);
        if (opptjeningAktiviteter.isEmpty()) {
            throw new IllegalStateException("No value present: Fant ikke forventet OpptjeningAktiviteter for behandling.");
        }
        var inntektsmeldingDiff = inntektsmeldingTjeneste.hentInntektsmeldingDiffFraOriginalbehandling(ref);
        var inntektsmeldingDiffDto = inntektsmeldingDiff.stream().map(IAYMapperTilKalkulus::mapInntektsmeldingDto)
                .collect(Collectors.toList());
        var iayGrunnlagUtenIMDiff = IAYMapperTilKalkulus.mapGrunnlag(iayGrunnlag, ref.aktørId());

        InntektArbeidYtelseGrunnlagDto iayGrunnlagDto;
        if (!inntektsmeldingDiffDto.isEmpty()) {
            iayGrunnlagDto = settInntektsmeldingDiffPåIAYGrunnlag(iayGrunnlagUtenIMDiff, inntektsmeldingDiffDto);
        } else {
            iayGrunnlagDto = iayGrunnlagUtenIMDiff;
        }

        List<KravperioderPrArbeidsforholdDto> kravperioder = mapKravperioder(ref, iayGrunnlag);
        var ytelseGrunnlag = getYtelsespesifiktGrunnlag(ref);
        var beregningsgrunnlagInput = new BeregningsgrunnlagInput(
                MapBehandlingRef.mapRef(ref),
                iayGrunnlagDto,
                OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(opptjeningAktiviteter.orElseThrow(), iayGrunnlag, ref),
                kravperioder,
                ytelseGrunnlag);
        kalkulusKonfigInjecter.leggTilFeatureToggles(beregningsgrunnlagInput);
        return beregningsgrunnlagInput;
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

}
