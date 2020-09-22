package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import static no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.IAYMapperTilKalkulus.mapArbeidsforholdOpplysninger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagGUIInput;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektArbeidYtelseGrunnlagDtoBuilder;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingAggregatDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.InntektsmeldingDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.AndelGraderingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.MapBehandlingRef;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.RefusjonskravDato;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;


public abstract class BeregningsgrunnlagGUIInputFelles {

    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private AndelGraderingTjeneste andelGraderingTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;



    @Inject
    public BeregningsgrunnlagGUIInputFelles(BehandlingRepository behandlingRepository,
                                             InntektArbeidYtelseTjeneste iayTjeneste,
                                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                             AndelGraderingTjeneste andelGraderingTjeneste,
                                             InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                             ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.skjæringstidspunktTjeneste = Objects.requireNonNull(skjæringstidspunktTjeneste, "skjæringstidspunktTjeneste");
        this.andelGraderingTjeneste = Objects.requireNonNull(andelGraderingTjeneste, "andelGrderingTjeneste");
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
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
        var iayGrunnlag = iayTjeneste.hentGrunnlag(referanse.getBehandlingId());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(referanse.getBehandlingId());
        return lagInput(referanse.medSkjæringstidspunkt(skjæringstidspunkt), iayGrunnlag).orElseThrow();
    }

    public Optional<BeregningsgrunnlagGUIInput> lagInput(Behandling behandling, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return lagInput(ref, iayGrunnlag);
    }

    /** Returnerer input hvis data er på tilgjengelig for det, ellers Optional.empty(). */
    private Optional<BeregningsgrunnlagGUIInput> lagInput(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var aktivitetGradering = andelGraderingTjeneste.utled(ref);
        List<RefusjonskravDato> refusjonskravDatoer = inntektsmeldingTjeneste.hentAlleRefusjonskravDatoerForFagsak(ref.getSaksnummer());
        List<Inntektsmelding> inntektsmeldingDiff = inntektsmeldingTjeneste.hentInntektsmeldingDiffFraOriginalbehandling(ref);
        List<InntektsmeldingDto> inntektsmeldingDiffDto = inntektsmeldingDiff.stream().map(IAYMapperTilKalkulus::mapInntektsmeldingDto).collect(Collectors.toList());
        InntektArbeidYtelseGrunnlagDto iayGrunnlagDtoUtenIMDiff = IAYMapperTilKalkulus.mapGrunnlag(iayGrunnlag);

        var ytelseGrunnlag = getYtelsespesifiktGrunnlag(ref);

        InntektArbeidYtelseGrunnlagDto iayGrunnlagDto;
        if (!inntektsmeldingDiffDto.isEmpty()) {
            iayGrunnlagDto = settInntektsmeldingDiffPåIAYGrunnlag(iayGrunnlagDtoUtenIMDiff, inntektsmeldingDiffDto);
        } else {
            iayGrunnlagDto = iayGrunnlagDtoUtenIMDiff;
        }

        Map<Arbeidsgiver, ArbeidsgiverOpplysninger> arbeidsgiverOpplysninger = iayGrunnlag.getAktørArbeidFraRegister(ref.getAktørId())
            .map(AktørArbeid::hentAlleYrkesaktiviteter)
            .orElse(Collections.emptyList())
            .stream()
            .map(Yrkesaktivitet::getArbeidsgiver)
            .distinct()
            .collect(Collectors.toMap(a -> a, arbeidsgiverTjeneste::hent));
        InntektArbeidYtelseGrunnlagDto iayGrunnlagMedArbeidsforholdOpplysninger = InntektArbeidYtelseGrunnlagDtoBuilder.oppdatere(iayGrunnlagDto)
            .medArbeidsgiverOpplysninger(mapArbeidsforholdOpplysninger(arbeidsgiverOpplysninger, iayGrunnlag.getArbeidsforholdOverstyringer()))
            .build();
        return Optional.of(new BeregningsgrunnlagGUIInput(
            MapBehandlingRef.mapRef(ref),
            iayGrunnlagMedArbeidsforholdOpplysninger,
            aktivitetGradering,
            IAYMapperTilKalkulus.mapRefusjonskravDatoer(refusjonskravDatoer),
            ytelseGrunnlag));
    }

    private InntektArbeidYtelseGrunnlagDto settInntektsmeldingDiffPåIAYGrunnlag(InntektArbeidYtelseGrunnlagDto iayGrunnlagDto, List<InntektsmeldingDto> inntektsmeldingDiffDto) {
        List<InntektsmeldingDto> inntektsmeldingDtos = iayGrunnlagDto.getInntektsmeldinger()
            .map(InntektsmeldingAggregatDto::getAlleInntektsmeldinger)
            .orElse(Collections.emptyList());
        InntektArbeidYtelseGrunnlagDtoBuilder builder = InntektArbeidYtelseGrunnlagDtoBuilder.oppdatere(iayGrunnlagDto)
            .medInntektsmeldinger(inntektsmeldingDtos, inntektsmeldingDiffDto);
        return builder.build();
    }

    /** Returnerer input hvis data er på tilgjengelig for det, ellers Optional.empty(). */
    public BeregningsgrunnlagGUIInput lagInput(Long behandlingId) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return lagInput(behandling, iayGrunnlag).orElseThrow();
    }
    public abstract YtelsespesifiktGrunnlag getYtelsespesifiktGrunnlag(BehandlingReferanse ref);
}
