package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.TemaUnderkategori;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;

public class InntektArbeidYtelseScenario {

    private InntektArbeidYtelseScenarioTestBuilder inntektArbeidYtelseScenarioTestBuilder;

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
        var kladd = getInntektArbeidYtelseScenarioTestBuilder().getKladd();
        return Optional.ofNullable(kladd);
    }

    public static class InntektArbeidYtelseScenarioTestBuilder {
        private static final InternArbeidsforholdRef DEFAULT_REF = InternArbeidsforholdRef.namedRef("TEST-REF");

        private final InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder;

        // AktivitetsAvtale
        private final LocalDate aktivitetsAvtaleFom = LocalDate.now().minusYears(3L);
        private final LocalDate aktivitetsAvtaleTom = LocalDate.now();
        private final BigDecimal aktivitetsAvtaleProsentsats = BigDecimal.TEN;

        private final AktørId aktørId = AktørId.dummy();

        // Yrkesaktivitet
        private final ArbeidType yrkesaktivitetArbeidType = ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;
        private final InternArbeidsforholdRef yrkesaktivitetArbeidsforholdId = DEFAULT_REF;

        // RelaterteYtelser
        private final RelatertYtelseType ytelseType = null;
        private final LocalDate iverksettelsesDato = LocalDate.now().minusYears(5L);
        private final RelatertYtelseTilstand relatertYtelseTilstand = RelatertYtelseTilstand.AVSLUTTET;
        private final TemaUnderkategori ytelseBehandlingstema = TemaUnderkategori.FORELDREPENGER_SVANGERSKAPSPENGER;
        private final Saksnummer saksnummer = new Saksnummer("00001");
        private final Fagsystem ytelseKilde = Fagsystem.INFOTRYGD;

        private InntektArbeidYtelseScenarioTestBuilder(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder) {
            this.inntektArbeidYtelseAggregatBuilder = inntektArbeidYtelseAggregatBuilder;
        }

        public static InntektArbeidYtelseScenarioTestBuilder ny(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder) {
            return new InntektArbeidYtelseScenarioTestBuilder(inntektArbeidYtelseAggregatBuilder);
        }

        private YtelseBuilder buildRelaterteYtelserGrunnlag(RelatertYtelseType ytelseType) {
            return YtelseBuilder.oppdatere(Optional.empty())
                .medKilde(ytelseKilde)
                .medSaksnummer(saksnummer)
                .medPeriode(DatoIntervallEntitet.fraOgMed(iverksettelsesDato))
                .medStatus(relatertYtelseTilstand)
                .medYtelseType(ytelseType)
                .medBehandlingsTema(ytelseBehandlingstema);
        }

        public InntektArbeidYtelseAggregatBuilder build() {
            var opptjeningsnøkkel = new Opptjeningsnøkkel(yrkesaktivitetArbeidsforholdId, KUNSTIG_ORG, aktørId.getId());

            var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(aktørId);
            var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
            var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

            var aktørYtelseBuilder = inntektArbeidYtelseAggregatBuilder.getAktørYtelseBuilder(aktørId);

            var aktivitetsAvtale = aktivitetsAvtaleBuilder
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(aktivitetsAvtaleFom, aktivitetsAvtaleTom))
                .medProsentsats(aktivitetsAvtaleProsentsats)
                .medSisteLønnsendringsdato(aktivitetsAvtaleFom);

            var ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(aktivitetsAvtaleFom, aktivitetsAvtaleTom));

            @SuppressWarnings("unused") var yrkesaktivitet = yrkesaktivitetBuilder
                .medArbeidType(yrkesaktivitetArbeidType)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(KUNSTIG_ORG))
                .medArbeidsforholdId(yrkesaktivitetArbeidsforholdId)
                .tilbakestillAvtaler()
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .leggTilAktivitetsAvtale(ansettelsesperiode)
                .build();

            var aktørArbeid = aktørArbeidBuilder
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
