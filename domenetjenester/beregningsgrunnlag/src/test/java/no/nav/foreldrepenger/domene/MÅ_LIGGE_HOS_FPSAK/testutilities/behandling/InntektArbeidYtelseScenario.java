package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.testutilities.behandling;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;


public class InntektArbeidYtelseScenario {

    private InntektArbeidYtelseScenarioTestBuilder inntektArbeidYtelseScenarioTestBuilder;
    private OppgittOpptjeningBuilder oppgittOpptjeningBuilder;

    public InntektArbeidYtelseAggregatBuilder medDefaultInntektArbeidYtelse() {
        inntektArbeidYtelseScenarioTestBuilder = getInntektArbeidYtelseScenarioTestBuilder();
        inntektArbeidYtelseScenarioTestBuilder.build();
        return inntektArbeidYtelseScenarioTestBuilder.inntektArbeidYtelseAggregatBuilder;
    }

    public InntektArbeidYtelseScenarioTestBuilder getInntektArbeidYtelseScenarioTestBuilder() {
        if (inntektArbeidYtelseScenarioTestBuilder == null) {
            inntektArbeidYtelseScenarioTestBuilder = getInntektArbeidYtelseScenarioTestBuilder(
                InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER));
        }
        return inntektArbeidYtelseScenarioTestBuilder;
    }

    private InntektArbeidYtelseScenarioTestBuilder getInntektArbeidYtelseScenarioTestBuilder(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder) {
        return InntektArbeidYtelseScenarioTestBuilder.ny(inntektArbeidYtelseAggregatBuilder);
    }

    Optional<InntektArbeidYtelseAggregatBuilder> initInntektArbeidYtelseAggregatBuilder() {
        InntektArbeidYtelseAggregatBuilder kladd = getInntektArbeidYtelseScenarioTestBuilder().getKladd();
        return Optional.ofNullable(kladd);
    }

    public OppgittOpptjeningBuilder medOppgittOpptjening(OppgittOpptjeningBuilder oppgittOpptjeningBuilder) {
        this.oppgittOpptjeningBuilder = oppgittOpptjeningBuilder;
        return oppgittOpptjeningBuilder;
    }

    Optional<OppgittOpptjeningBuilder> initOppgittOpptjeningBuilder() {
        return Optional.ofNullable(oppgittOpptjeningBuilder);
    }

    public static class InntektArbeidYtelseScenarioTestBuilder {
        private static final InternArbeidsforholdRef DEFAULT_REF = InternArbeidsforholdRef.namedRef("TEST-REF");

        private InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder;

        // Virksomhet
        private String orgNr = KUNSTIG_ORG;
        private AktørId aktørId = AktørId.dummy();

        // Yrkesaktivitet
        private ArbeidType yrkesaktivitetArbeidType = ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;
        private InternArbeidsforholdRef yrkesaktivitetArbeidsforholdId = DEFAULT_REF;

        private InntektArbeidYtelseScenarioTestBuilder(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder) {
            this.inntektArbeidYtelseAggregatBuilder = inntektArbeidYtelseAggregatBuilder;
        }

        public static InntektArbeidYtelseScenarioTestBuilder ny(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder) {
            return new InntektArbeidYtelseScenarioTestBuilder(inntektArbeidYtelseAggregatBuilder);
        }


        public InntektArbeidYtelseAggregatBuilder build() {
            final Opptjeningsnøkkel opptjeningsnøkkel = new Opptjeningsnøkkel(yrkesaktivitetArbeidsforholdId, orgNr, aktørId.getId());

            InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(aktørId);
            YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

            @SuppressWarnings("unused")
            Yrkesaktivitet yrkesaktivitet = yrkesaktivitetBuilder
                .medArbeidType(yrkesaktivitetArbeidType)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(orgNr))
                .medArbeidsforholdId(yrkesaktivitetArbeidsforholdId)
                .build();

            InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = aktørArbeidBuilder
                .leggTilYrkesaktivitet(yrkesaktivitetBuilder);


            inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeid);
            return inntektArbeidYtelseAggregatBuilder;
        }

        /**
         * Gir den rå buildern for å videre manipulere testdata. på samme måte som entitene bygges på.
         *
         * @return buildern
         */
        public InntektArbeidYtelseAggregatBuilder getKladd() {
            return inntektArbeidYtelseAggregatBuilder;
        }

    }
}
