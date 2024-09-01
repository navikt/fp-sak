package no.nav.foreldrepenger.domene.opptjening;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.typer.AktørId;

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
        var optional = opptjeningRepository.finnOpptjening(behandlingId);
        return optional
                .orElseThrow(() -> new IllegalStateException("Utvikler-feil: Mangler Opptjening for Behandling: " + behandlingId));
    }

    /** Hent alle inntekter for søker der det finnes arbeidsgiver */
    public List<OpptjeningInntektPeriode> hentRelevanteOpptjeningInntekterForVilkårVurdering(Long behandlingId, AktørId aktørId,
            LocalDate skjæringstidspunkt) {
        var grunnlagOpt = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);

        if (grunnlagOpt.isPresent()) {
            var grunnlag = grunnlagOpt.get();
            var filter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)).før(skjæringstidspunkt).filterPensjonsgivende();

            var result = filter.filter((inntekt, inntektspost) -> inntekt.getArbeidsgiver() != null)
                    .mapInntektspost((inntekt, inntektspost) -> {
                        var opptjeningsnøkkel = new Opptjeningsnøkkel(null, inntekt.getArbeidsgiver());
                        return new OpptjeningInntektPeriode(inntektspost, opptjeningsnøkkel);
                    });
            return List.copyOf(result);
        }
        return Collections.emptyList();
    }

    public List<OpptjeningAktivitetPeriode> hentRelevanteOpptjeningAktiveterForVilkårVurdering(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt stp) {
        var perioder = opptjeningsperioderTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(behandlingReferanse, stp);

        return perioder.stream().map(this::mapTilPerioder).toList();
    }

    private OpptjeningAktivitetPeriode mapTilPerioder(OpptjeningsperiodeForSaksbehandling periode) {
        var builder = OpptjeningAktivitetPeriode.Builder.ny();
        builder.medPeriode(periode.getPeriode())
                .medOpptjeningAktivitetType(periode.getOpptjeningAktivitetType())
                .medOrgnr(Optional.ofNullable(periode.getArbeidsgiver()).map(Arbeidsgiver::getOrgnr).orElse(null))
                .medOpptjeningsnøkkel(periode.getOpptjeningsnøkkel())
                .medStillingsandel(periode.getStillingsprosent())
                .medVurderingsStatus(periode.getVurderingsStatus())
                .medBegrunnelse(periode.getBegrunnelse());
        return builder.build();
    }

}
