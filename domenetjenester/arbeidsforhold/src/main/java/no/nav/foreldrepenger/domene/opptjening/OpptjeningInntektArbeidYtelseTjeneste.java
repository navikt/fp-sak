package no.nav.foreldrepenger.domene.opptjening;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

/** Henter inntekter, arbeid, og ytelser relevant for opptjening. */
@ApplicationScoped
public class OpptjeningInntektArbeidYtelseTjeneste {

    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private OpptjeningRepository opptjeningRepository;
    private OpptjeningsperioderTjeneste opptjeningsperioderTjeneste;

    OpptjeningInntektArbeidYtelseTjeneste() {
        // for CDI proxy
    }

    @Inject
    public OpptjeningInntektArbeidYtelseTjeneste(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                                     OpptjeningRepository opptjeningRepository,
                                                     OpptjeningsperioderTjeneste opptjeningsperioderTjeneste) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.opptjeningRepository = opptjeningRepository;
        this.opptjeningsperioderTjeneste = opptjeningsperioderTjeneste;
    }

    public Opptjening hentOpptjening(Long behandlingId) {
        Optional<Opptjening> optional = opptjeningRepository.finnOpptjening(behandlingId);
        return optional
            .orElseThrow(() -> new IllegalStateException("Utvikler-feil: Mangler Opptjening for Behandling: " + behandlingId));
    }

    /** Hent alle inntekter for søker der det finnes arbeidsgiver*/
    public List<OpptjeningInntektPeriode> hentRelevanteOpptjeningInntekterForVilkårVurdering(Long behandlingId, AktørId aktørId, LocalDate skjæringstidspunkt) {
        Optional<InntektArbeidYtelseGrunnlag> grunnlagOpt = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);

        if (grunnlagOpt.isPresent()) {
            InntektArbeidYtelseGrunnlag grunnlag = grunnlagOpt.get();
            var filter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)).før(skjæringstidspunkt).filterPensjonsgivende();

            var result = filter.filter((inntekt, inntektspost) -> inntekt.getArbeidsgiver() != null)
                .mapInntektspost((inntekt, inntektspost) -> {
                    Opptjeningsnøkkel opptjeningsnøkkel = new Opptjeningsnøkkel(null, inntekt.getArbeidsgiver());
                    return new OpptjeningInntektPeriode(inntektspost, opptjeningsnøkkel);
                });
            return List.copyOf(result);
        }
        return Collections.emptyList();
    }

    /**
     * Hent siste ytelse etter kapittel 8, 9 og 14 før skjæringstidspunkt for opptjening
     * @param behandling en Behandling
     * @return Ytelse hvis den finnes, ellers Optional.empty()
     */
    public Optional<Ytelse> hentSisteInfotrygdYtelseFørSkjæringstidspunktForOpptjening(Long behandlingId, AktørId aktørId) {
        var grunnlagOpt = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);
        if (!grunnlagOpt.isPresent()) {
            return Optional.empty();
        }
        Optional<Opptjening> opptjeningOpt = opptjeningRepository.finnOpptjening(behandlingId);
        if (!opptjeningOpt.isPresent()) {
            return Optional.empty();
        }
        var grunnlag = grunnlagOpt.get();
        LocalDate skjæringstidspunktForOpptjening = opptjeningOpt.get().getTom();
        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunktForOpptjening);

        if (filter.isEmpty()) {
            return Optional.empty();
        }

        return filter.getFiltrertYtelser().stream()
            .filter(y -> !y.getPeriode().getFomDato().isAfter(skjæringstidspunktForOpptjening))
            .filter(y -> Fagsystem.INFOTRYGD.equals(y.getKilde()))
            .max(Comparator.comparing(y -> y.getPeriode().getFomDato()));
    }

    /**
     * Hent siste ytelse etter kapittel 8, 9 og 14 før skjæringstidspunkt for opptjening
     * @param behandling en Behandling
     * @return liste med sammenhengende ytelser som gjelder før skjæringstidspunkt for opptjening
     */
    public List<Ytelse> hentSammenhengendeInfotrygdYtelserFørSkjæringstidspunktForOppjening(Long behandlingId, AktørId aktørId) {
        var grunnlagOpt = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);
        if (!grunnlagOpt.isPresent()) {
            return Collections.emptyList();
        }
        Optional<Opptjening> opptjeningOpt = opptjeningRepository.finnOpptjening(behandlingId);
        if (!opptjeningOpt.isPresent()) {
            return Collections.emptyList();
        }
        var grunnlag = grunnlagOpt.get();
        LocalDate skjæringstidspunktForOpptjening = opptjeningOpt.get().getTom();
        var filter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunktForOpptjening);

        if (filter.isEmptyFiltered()) {
            return Collections.emptyList();
        }
        return finnSammenhengendeInfotrygdYtelser(filter.getFiltrertYtelser(), skjæringstidspunktForOpptjening);
    }

    private List<Ytelse> finnSammenhengendeInfotrygdYtelser(Collection<Ytelse> ytelser, LocalDate skjæringstidspunktForOpptjening) {
        List<Ytelse> ytelserFørSkjæringstidspunkt = ytelser.stream()
            .filter(y -> !y.getPeriode().getFomDato().isAfter(skjæringstidspunktForOpptjening))
            .filter(y -> Fagsystem.INFOTRYGD.equals(y.getKilde()))
            .sorted(Comparator.comparing((Ytelse y) -> y.getPeriode().getFomDato()).reversed())
            .collect(Collectors.toList());

        if (ytelserFørSkjæringstidspunkt.isEmpty()) {
            return ytelserFørSkjæringstidspunkt;
        }

        List<Ytelse> sammenhengende = new ArrayList<>(ytelserFørSkjæringstidspunkt.subList(0, 1));

        for (int i = 0; i < ytelserFørSkjæringstidspunkt.size() - 1; i++) {
            if (erPeriodeneSammenhengende(ytelserFørSkjæringstidspunkt.get(i).getPeriode(), ytelserFørSkjæringstidspunkt.get(i + 1).getPeriode())) {
                Ytelse nesteSammenhengendeYtelse = ytelserFørSkjæringstidspunkt.get(i + 1);
                sammenhengende.add(nesteSammenhengendeYtelse);
            } else {
                return sammenhengende;
            }
        }
        return sammenhengende;

    }

    private boolean erPeriodeneSammenhengende(DatoIntervallEntitet periode1, DatoIntervallEntitet periode2) {
        return !periode1.getFomDato().isAfter(periode2.getTomDato().plusDays(1));
    }

    public List<OpptjeningAktivitetPeriode> hentRelevanteOpptjeningAktiveterForVilkårVurdering(BehandlingReferanse behandlingReferanse) {
        final List<OpptjeningsperiodeForSaksbehandling> perioder = opptjeningsperioderTjeneste
            .hentRelevanteOpptjeningAktiveterForVilkårVurdering(behandlingReferanse);

        return perioder.stream().map(this::mapTilPerioder).collect(Collectors.toList());
    }

    private OpptjeningAktivitetPeriode mapTilPerioder(OpptjeningsperiodeForSaksbehandling periode) {
        final OpptjeningAktivitetPeriode.Builder builder = OpptjeningAktivitetPeriode.Builder.ny();
        builder.medPeriode(periode.getPeriode())
            .medOpptjeningAktivitetType(periode.getOpptjeningAktivitetType())
            .medOrgnr(periode.getOrgnr())
            .medOpptjeningsnøkkel(periode.getOpptjeningsnøkkel())
            .medStillingsandel(periode.getStillingsprosent())
            .medVurderingsStatus(periode.getVurderingsStatus())
            .medBegrunnelse(periode.getBegrunnelse());
        return builder.build();
    }

}
