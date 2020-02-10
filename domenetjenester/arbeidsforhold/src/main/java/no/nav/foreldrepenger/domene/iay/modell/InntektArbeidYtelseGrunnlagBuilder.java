package no.nav.foreldrepenger.domene.iay.modell;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.vedtak.util.Tuple;

public class InntektArbeidYtelseGrunnlagBuilder {

    private InntektArbeidYtelseGrunnlag kladd;

    protected InntektArbeidYtelseGrunnlagBuilder(InntektArbeidYtelseGrunnlag kladd) {
        this.kladd = kladd;
    }

    public static InntektArbeidYtelseGrunnlagBuilder nytt() {
        return ny(UUID.randomUUID(), LocalDateTime.now());
    }

    /** Opprett ny versjon av grunnlag med angitt assignet grunnlagReferanse og opprettetTidspunkt. */
    public static InntektArbeidYtelseGrunnlagBuilder ny(UUID grunnlagReferanse, LocalDateTime opprettetTidspunkt) {
        return new InntektArbeidYtelseGrunnlagBuilder(new InntektArbeidYtelseGrunnlag(grunnlagReferanse, opprettetTidspunkt));
    }

    public static InntektArbeidYtelseGrunnlagBuilder oppdatere(InntektArbeidYtelseGrunnlag kladd) {
        return new InntektArbeidYtelseGrunnlagBuilder(new InntektArbeidYtelseGrunnlag(kladd));
    }

    public static InntektArbeidYtelseGrunnlagBuilder oppdatere(Optional<InntektArbeidYtelseGrunnlag> kladd) {
        return kladd.map(InntektArbeidYtelseGrunnlagBuilder::oppdatere).orElseGet(InntektArbeidYtelseGrunnlagBuilder::nytt);
    }

    // FIXME: Bør ikke være public, bryter encapsulation
    public InntektArbeidYtelseGrunnlag getKladd() {
        return kladd;
    }

    public InntektsmeldingAggregat getInntektsmeldinger() {
        final Optional<InntektsmeldingAggregat> inntektsmeldinger = kladd.getInntektsmeldinger();
        return inntektsmeldinger.map(InntektsmeldingAggregat::new).orElseGet(InntektsmeldingAggregat::new);
    }

    public void setInntektsmeldinger(InntektsmeldingAggregat inntektsmeldinger) {
        kladd.setInntektsmeldinger(inntektsmeldinger);
    }

    public ArbeidsforholdInformasjon getInformasjon() {
        var informasjon = kladd.getArbeidsforholdInformasjon();

        var informasjonEntitet = informasjon.orElseGet(() -> new ArbeidsforholdInformasjon());
        kladd.setInformasjon(informasjonEntitet);
        return informasjonEntitet;
    }

    public InntektArbeidYtelseGrunnlagBuilder medInformasjon(ArbeidsforholdInformasjon informasjon) {
        kladd.setInformasjon(informasjon);
        return this;
    }

    private void medSaksbehandlet(InntektArbeidYtelseAggregatBuilder builder) {
        if (builder != null) {
            kladd.setSaksbehandlet(builder.build());
        }
    }

    private void medRegister(InntektArbeidYtelseAggregatBuilder builder) {
        if (builder != null) {
            kladd.setRegister(builder.build());
        }
    }

    public InntektArbeidYtelseGrunnlagBuilder medOppgittOpptjening(OppgittOpptjeningBuilder builder) {
        if (builder != null) {
            if (kladd.getOppgittOpptjening().isPresent()) {
                throw new IllegalStateException("Utviklerfeil: Er ikke lov å endre oppgitt opptjening!");
            }
            kladd.setOppgittOpptjening(builder.build());
        }
        return this;
    }

    public InntektArbeidYtelseGrunnlag build() {
        var k = kladd;
        if (kladd.getArbeidsforholdInformasjon().isPresent()) {
            k.taHensynTilBetraktninger();
        }
        kladd = null; // må ikke finne på å gjenbruke buildere her, tar heller straffen i en NPE ved første feilkall
        return k;
    }

    public InntektArbeidYtelseGrunnlagBuilder medData(InntektArbeidYtelseAggregatBuilder builder) {
        VersjonType versjon = builder.getVersjon();

        if (versjon == VersjonType.REGISTER) {
            medRegister(builder);
        } else if (versjon == VersjonType.SAKSBEHANDLET) {
            medSaksbehandlet(builder);
        }
        return this;
    }

    public void ryddOppErstattedeArbeidsforhold(AktørId søker,
                                         List<Tuple<Arbeidsgiver, Tuple<InternArbeidsforholdRef, InternArbeidsforholdRef>>> erstattArbeidsforhold) {
        final Optional<InntektArbeidYtelseAggregat> registerFørVersjon = kladd.getRegisterVersjon();
        for (Tuple<Arbeidsgiver, Tuple<InternArbeidsforholdRef, InternArbeidsforholdRef>> tuple : erstattArbeidsforhold) {
            if (registerFørVersjon.isPresent()) {
                // TODO: Vurder konsekvensen av dette.
                final InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(registerFørVersjon, VersjonType.REGISTER);
                builder.oppdaterArbeidsforholdReferanseEtterErstatting(søker, tuple.getElement1(), tuple.getElement2().getElement1(),
                    tuple.getElement2().getElement2());
                medData(builder);
            }
        }
    }

    public Optional<ArbeidsforholdInformasjon> getArbeidsforholdInformasjon() {
        return kladd.getArbeidsforholdInformasjon();
    }

    protected void fjernSaksbehandlet() {
        kladd.fjernSaksbehandlet();
    }

    public InntektArbeidYtelseGrunnlagBuilder medErAktivtGrunnlag(boolean erAktivt) {
        kladd.setAktivt(erAktivt);
        return this;
    }

    public InntektArbeidYtelseGrunnlagBuilder medInntektsmeldinger(Collection<Inntektsmelding> inntektsmeldinger) {
        setInntektsmeldinger(new InntektsmeldingAggregat(inntektsmeldinger));
        return this;
    }

    public InntektArbeidYtelseGrunnlagBuilder medInntektsmeldinger(Inntektsmelding... inntektsmeldinger) {
        return medInntektsmeldinger(Arrays.asList(inntektsmeldinger));
    }
}
