package no.nav.foreldrepenger.behandling.revurdering.ytelse;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulator.guitjenester.GraderingUtenBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.BeregningUttakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.entiteter.BGAndelArbeidsforhold;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.BehandlingslagerTilKalkulusMapper;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class UttakInputTjeneste {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private MedlemTjeneste medlemTjeneste;
    private BehandlingRepository behandlingRepository;
    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private SøknadRepository søknadRepository;
    private PersonopplysningRepository personopplysningRepository;
    private BeregningUttakTjeneste beregningUttakTjeneste;
    private no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.UttakGrunnlagTjeneste fpUttakGrunnlagTjeneste;
    private no.nav.foreldrepenger.behandling.revurdering.ytelse.svp.UttakGrunnlagTjeneste svpUttakGrunnlagTjeneste;

    @Inject
    public UttakInputTjeneste(BehandlingRepositoryProvider repositoryProvider,
                              HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                              InntektArbeidYtelseTjeneste iayTjeneste,
                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                              MedlemTjeneste medlemTjeneste,
                              BeregningUttakTjeneste beregningUttakTjeneste,
                              no.nav.foreldrepenger.behandling.revurdering.ytelse.fp.UttakGrunnlagTjeneste fpUttakGrunnlagTjeneste,
                              no.nav.foreldrepenger.behandling.revurdering.ytelse.svp.UttakGrunnlagTjeneste svpUttakGrunnlagTjeneste) {
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.skjæringstidspunktTjeneste = Objects.requireNonNull(skjæringstidspunktTjeneste, "skjæringstidspunktTjeneste");
        this.medlemTjeneste = Objects.requireNonNull(medlemTjeneste, "medlemTjeneste");
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.personopplysningRepository = repositoryProvider.getPersonopplysningRepository();
        this.beregningUttakTjeneste = beregningUttakTjeneste;
        this.fpUttakGrunnlagTjeneste = fpUttakGrunnlagTjeneste;
        this.svpUttakGrunnlagTjeneste = svpUttakGrunnlagTjeneste;
    }

    UttakInputTjeneste() {
        // for CDI proxy
    }

    public UttakInput lagInput(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return lagInput(behandling);
    }

    public UttakInput lagInput(Behandling behandling) {
        var behandlingId = behandling.getId();
        var iayGrunnlag = iayTjeneste.finnGrunnlag(behandlingId).orElse(null);
        var medlemskapOpphørsdato = medlemTjeneste.hentOpphørsdatoHvisEksisterer(behandlingId);
        return lagInput(behandling, iayGrunnlag, medlemskapOpphørsdato.orElse(null));
    }

    public UttakInput lagInput(Behandling behandling, InntektArbeidYtelseGrunnlag iayGrunnlag, LocalDate medlemskapOpphørsdato) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        var søknadEntitet = søknadRepository.hentSøknadHvisEksisterer(ref.behandlingId());
        var søknadOpprettetTidspunkt = søknadEntitet.map(SøknadEntitet::getOpprettetTidspunkt).orElse(null);
        var ytelsespesifiktGrunnlag = lagYtelsesspesifiktGrunnlag(ref);
        var årsaker = finnÅrsaker(ref);
        var input = new UttakInput(ref, iayGrunnlag, ytelsespesifiktGrunnlag)
            .medMedlemskapOpphørsdato(medlemskapOpphørsdato)
            .medSøknadOpprettetTidspunkt(søknadOpprettetTidspunkt)
            .medBehandlingÅrsaker(map(årsaker))
            .medBehandlingManueltOpprettet(erManueltOpprettet(årsaker));
        var beregningsgrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(ref.behandlingId());
        if (beregningsgrunnlag.isPresent()) {
            var bgStatuser = lagBeregningsgrunnlagStatuser(beregningsgrunnlag.get());
            var finnesAndelerMedGraderingUtenBeregningsgrunnlag = finnesAndelerMedGraderingUtenBeregningsgrunnlag(ref, beregningsgrunnlag.get());
            input = input.medBeregningsgrunnlagStatuser(bgStatuser)
                    .medFinnesAndelerMedGraderingUtenBeregningsgrunnlag(finnesAndelerMedGraderingUtenBeregningsgrunnlag);
        }
        return input;
    }

    private boolean finnesAndelerMedGraderingUtenBeregningsgrunnlag(BehandlingReferanse ref, BeregningsgrunnlagEntitet beregningsgrunnlag) {
        var aktivitetGradering = beregningUttakTjeneste.finnAktivitetGraderinger(ref);
        var andelerMedGraderingUtenBG = GraderingUtenBeregningsgrunnlagTjeneste
                .finnAndelerMedGraderingUtenBG(BehandlingslagerTilKalkulusMapper.mapBeregningsgrunnlag(beregningsgrunnlag), aktivitetGradering);

        return !andelerMedGraderingUtenBG.isEmpty();
    }

    private Set<BehandlingÅrsakType> map(Set<BehandlingÅrsak> årsaker) {
        return årsaker.stream().map(BehandlingÅrsak::getBehandlingÅrsakType).collect(Collectors.toSet());
    }

    private boolean erManueltOpprettet(Set<BehandlingÅrsak> årsaker) {
        return årsaker.stream().anyMatch(BehandlingÅrsak::erManueltOpprettet);
    }

    private Set<BehandlingÅrsak> finnÅrsaker(BehandlingReferanse ref) {
        var behandling = behandlingRepository.hentBehandling(ref.behandlingId());
        return new HashSet<>(behandling.getBehandlingÅrsaker());
    }

    private Set<BeregningsgrunnlagStatus> lagBeregningsgrunnlagStatuser(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
                .flatMap(beregningsgrunnlagPeriode -> beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
                .map(this::mapAndel)
                .collect(Collectors.toSet());
    }

    private BeregningsgrunnlagStatus mapAndel(BeregningsgrunnlagPrStatusOgAndel andel) {
        var arbeidsforholdRef = andel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getArbeidsforholdRef).orElse(null);
        var arbeidsgiver = andel.getArbeidsgiver().orElse(null);
        return new BeregningsgrunnlagStatus(mapStatus(andel.getAktivitetStatus()), arbeidsgiver, arbeidsforholdRef);
    }

    private AktivitetStatus mapStatus(no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus aktivitetStatus) {
        return AktivitetStatus.fraKode(aktivitetStatus.getKode());
    }

    private YtelsespesifiktGrunnlag lagYtelsesspesifiktGrunnlag(BehandlingReferanse ref) {
        var ytelseType = ref.fagsakYtelseType();

        return switch (ytelseType) {
            case FORELDREPENGER -> fpUttakGrunnlagTjeneste.grunnlag(ref);
            case SVANGERSKAPSPENGER -> svpUttakGrunnlagTjeneste.grunnlag(ref);
            default -> throw new IllegalStateException("Finner ikke tjeneste for å lage ytelsesspesifikt grunnlag for ytelsetype " + ytelseType);
        };
    }
}
