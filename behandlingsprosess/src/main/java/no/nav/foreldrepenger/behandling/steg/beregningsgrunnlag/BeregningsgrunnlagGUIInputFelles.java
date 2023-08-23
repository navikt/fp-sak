package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import jakarta.inject.Inject;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagGUIInput;
import no.nav.folketrygdloven.kalkulator.input.YtelsespesifiktGrunnlag;
import no.nav.folketrygdloven.kalkulator.modell.avklaringsbehov.AvklaringsbehovDto;
import no.nav.folketrygdloven.kalkulator.modell.iay.KravperioderPrArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.AvklaringsbehovDefinisjon;
import no.nav.folketrygdloven.kalkulus.kodeverk.AvklaringsbehovStatus;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.IAYMapperTilKalkulus;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.KravperioderMapper;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.MapBehandlingRef;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.OpptjeningMapperTilKalkulus;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public abstract class BeregningsgrunnlagGUIInputFelles {

    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    @Inject
    public BeregningsgrunnlagGUIInputFelles(BehandlingRepository behandlingRepository,
                                            InntektArbeidYtelseTjeneste iayTjeneste,
                                            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                            OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
                                            InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.skjæringstidspunktTjeneste = Objects.requireNonNull(skjæringstidspunktTjeneste, "skjæringstidspunktTjeneste");
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    protected BeregningsgrunnlagGUIInputFelles() {
        // for CDI proxy
    }

    public BeregningsgrunnlagGUIInput lagInput(Behandling behandling) {
        var behandlingId = behandling.getId();
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);

        return lagInput(ref, iayGrunnlag, behandling.getAksjonspunkter()).orElseThrow();
    }

    public Optional<BeregningsgrunnlagGUIInput> lagInput(Behandling behandling, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return lagInput(ref, iayGrunnlag, behandling.getAksjonspunkter());
    }

    /**
     * Returnerer input hvis data er på tilgjengelig for det, ellers
     * Optional.empty().
     */
    private Optional<BeregningsgrunnlagGUIInput> lagInput(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag, Set<Aksjonspunkt> aksjonspunkter) {
        var inntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, ref.getUtledetSkjæringstidspunkt(), iayGrunnlag, true);
        var iayGrunnlagDto = IAYMapperTilKalkulus.mapGrunnlag(iayGrunnlag, inntektsmeldinger, ref.aktørId());
        var ytelseGrunnlag = getYtelsespesifiktGrunnlag(ref);
        var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(ref, iayGrunnlag);
        var mappetOpptjening = opptjeningAktiviteter
            .map(opptjeningAktiviteter1 -> OpptjeningMapperTilKalkulus.mapOpptjeningAktiviteter(opptjeningAktiviteter1, iayGrunnlag,
            ref)).orElse(null);

        var kravperioder = mapKravperioder(ref, iayGrunnlag);
        var input = new BeregningsgrunnlagGUIInput(
            MapBehandlingRef.mapRef(ref),
            iayGrunnlagDto,
            kravperioder,
            mappetOpptjening,
            ytelseGrunnlag);
        input.medAvklaringsbehov(mapAvklaringsbehov(aksjonspunkter));
        return Optional.of(input);
    }

    private List<AvklaringsbehovDto> mapAvklaringsbehov(Set<Aksjonspunkt> aksjonspunkter) {
        return aksjonspunkter.stream()
            .map(this::mapTilAvklaringsbehov)
            .flatMap(Optional::stream)
            .toList();
    }

    private Optional<AvklaringsbehovDto> mapTilAvklaringsbehov(Aksjonspunkt ap) {
        var definisjon =  switch (ap.getAksjonspunktDefinisjon()) {
            case FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS -> AvklaringsbehovDefinisjon.FASTSETT_BG_AT_FL;
            case FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD -> AvklaringsbehovDefinisjon.FASTSETT_BG_TB_ARB;
            case FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET -> AvklaringsbehovDefinisjon.FASTSETT_BG_SN_NY_I_ARB_LIVT;
            case VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE -> AvklaringsbehovDefinisjon.VURDER_VARIG_ENDRT_NYOPPSTR_NAERNG_SN;
            case FORDEL_BEREGNINGSGRUNNLAG -> AvklaringsbehovDefinisjon.FORDEL_BG;
            case AVKLAR_AKTIVITETER -> AvklaringsbehovDefinisjon.AVKLAR_AKTIVITETER;
            case VURDER_FAKTA_FOR_ATFL_SN -> AvklaringsbehovDefinisjon.VURDER_FAKTA_ATFL_SN;
            case VURDER_REFUSJON_BERGRUNN -> AvklaringsbehovDefinisjon.VURDER_REFUSJONSKRAV;
            case OVERSTYRING_AV_BEREGNINGSAKTIVITETER -> AvklaringsbehovDefinisjon.OVST_BEREGNINGSAKTIVITETER;
            case OVERSTYRING_AV_BEREGNINGSGRUNNLAG -> AvklaringsbehovDefinisjon.OVST_INNTEKT;
            case AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST -> AvklaringsbehovDefinisjon.AUTO_VENT_PÅ_INNTKT_RAP_FRST;
            case AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT -> AvklaringsbehovDefinisjon.AUTO_VENT_PÅ_SISTE_AAP_DP_MELDKRT;
            default -> null; // Aksjonspunkt som ikke er relatert til beregning
        };
        if (definisjon == null) {
            return Optional.empty();
        }
        var status = switch(ap.getStatus()) {
            case OPPRETTET -> AvklaringsbehovStatus.OPPRETTET;
            case AVBRUTT -> AvklaringsbehovStatus.AVBRUTT;
            case UTFØRT -> AvklaringsbehovStatus.UTFØRT;
        };
        return Optional.of(new AvklaringsbehovDto(definisjon, status, ap.getBegrunnelse(), false, ap.getEndretAv(), ap.getEndretTidspunkt()));
    }

    private List<KravperioderPrArbeidsforholdDto> mapKravperioder(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var alleInntektsmeldingerForFagsak = inntektsmeldingTjeneste.hentAlleInntektsmeldingerForFagsak(ref.saksnummer());
        return KravperioderMapper.map(ref, alleInntektsmeldingerForFagsak, iayGrunnlag);
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
