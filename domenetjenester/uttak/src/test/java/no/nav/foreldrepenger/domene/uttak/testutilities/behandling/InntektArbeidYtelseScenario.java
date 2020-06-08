package no.nav.foreldrepenger.domene.uttak.testutilities.behandling;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.TemaUnderkategori;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.util.FPDateUtil;

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
        InntektArbeidYtelseAggregatBuilder kladd = getInntektArbeidYtelseScenarioTestBuilder().getKladd();
        return Optional.ofNullable(kladd);
    }

    public static class InntektArbeidYtelseScenarioTestBuilder {
        private static final InternArbeidsforholdRef DEFAULT_REF = InternArbeidsforholdRef.namedRef("TEST-REF");

        private InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder;

        // AktivitetsAvtale
        private LocalDate aktivitetsAvtaleFom = FPDateUtil.iDag().minusYears(3L);
        private LocalDate aktivitetsAvtaleTom = FPDateUtil.iDag();
        private BigDecimal aktivitetsAvtaleProsentsats = BigDecimal.TEN;
        private BigDecimal aktivitetsAvtaleAntallTimer = BigDecimal.valueOf(20.4d);
        private BigDecimal aktivitetsAvtaleAntallTimerFulltid = BigDecimal.valueOf(10.2d);

        // Virksomhet
        private String orgNr = KUNSTIG_ORG;
        private AktørId aktørId = AktørId.dummy();

        // Yrkesaktivitet
        private ArbeidType yrkesaktivitetArbeidType = ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;
        private InternArbeidsforholdRef yrkesaktivitetArbeidsforholdId = DEFAULT_REF;

        // RelaterteYtelser
        private RelatertYtelseType ytelseType = null;
        private LocalDate iverksettelsesDato = FPDateUtil.iDag().minusYears(5L);
        private RelatertYtelseTilstand relatertYtelseTilstand = RelatertYtelseTilstand.AVSLUTTET;
        private TemaUnderkategori ytelseBehandlingstema = TemaUnderkategori.FORELDREPENGER_SVANGERSKAPSPENGER;
        private LocalDate tomDato;
        private Saksnummer saksnummer = new Saksnummer("00001");
        private Fagsystem ytelseKilde = Fagsystem.INFOTRYGD;

        private InntektArbeidYtelseScenarioTestBuilder(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder) {
            this.inntektArbeidYtelseAggregatBuilder = inntektArbeidYtelseAggregatBuilder;
        }

        public static InntektArbeidYtelseScenarioTestBuilder ny(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder) {
            return new InntektArbeidYtelseScenarioTestBuilder(inntektArbeidYtelseAggregatBuilder);
        }

        public InntektArbeidYtelseScenarioTestBuilder medAktørId(AktørId aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        // AktivitetsAvtale
        public InntektArbeidYtelseScenarioTestBuilder medAktivitetsAvtaleFom(LocalDate aktivitetsAvtaleFom) {
            this.aktivitetsAvtaleFom = aktivitetsAvtaleFom;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medAktivitetsAvtaleTom(LocalDate aktivitetsAvtaleTom) {
            this.aktivitetsAvtaleTom = aktivitetsAvtaleTom;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medAktivitetsAvtaleProsentsats(BigDecimal aktivitetsAvtaleProsentsats) {
            this.aktivitetsAvtaleProsentsats = aktivitetsAvtaleProsentsats;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medAktivitetsAvtaleAntallTimer(BigDecimal aktivitetsAvtaleAntallTimer) {
            this.aktivitetsAvtaleAntallTimer = aktivitetsAvtaleAntallTimer;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medAktivitetsAvtaleAntallTimerFulltid(BigDecimal aktivitetsAvtaleAntallTimerFulltid) {
            this.aktivitetsAvtaleAntallTimerFulltid = aktivitetsAvtaleAntallTimerFulltid;
            return this;
        }

        // Virksomhet
        public InntektArbeidYtelseScenarioTestBuilder medOrgNr(String orgNr) {
            this.orgNr = orgNr;
            return this;
        }

        // Yrkesaktivitet
        public InntektArbeidYtelseScenarioTestBuilder medYrkesaktivitetArbeidType(ArbeidType yrkesaktivitetArbeidType) {
            this.yrkesaktivitetArbeidType = yrkesaktivitetArbeidType;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medYrkesaktivitetArbeidsforholdId(InternArbeidsforholdRef arbeidsforholdId) {
            this.yrkesaktivitetArbeidsforholdId = arbeidsforholdId;
            return this;
        }

        // Ytelse (YtelseType må settes)
        public InntektArbeidYtelseScenarioTestBuilder medYtelseType(RelatertYtelseType ytelseType) {
            this.ytelseType = ytelseType;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medYtelseTomDato(LocalDate tomDato) {
            this.tomDato = tomDato;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medYtelseKilde(Fagsystem ytelseKilde) {
            this.ytelseKilde = ytelseKilde;
            return this;
        }

        private YtelseBuilder buildRelaterteYtelserGrunnlag(RelatertYtelseType ytelseType) {
            return YtelseBuilder.oppdatere(Optional.empty())
                .medKilde(ytelseKilde)
                .medSaksnummer(saksnummer)
                .medPeriode(
                    tomDato != null ? DatoIntervallEntitet.fraOgMedTilOgMed(iverksettelsesDato, tomDato) : DatoIntervallEntitet.fraOgMed(iverksettelsesDato))
                .medStatus(relatertYtelseTilstand)
                .medYtelseType(ytelseType)
                .medBehandlingsTema(ytelseBehandlingstema);
        }

        public InntektArbeidYtelseAggregatBuilder build() {
            final Opptjeningsnøkkel opptjeningsnøkkel = new Opptjeningsnøkkel(yrkesaktivitetArbeidsforholdId, orgNr, aktørId.getId());

            InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(aktørId);
            YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
            AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

            InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder aktørYtelseBuilder = inntektArbeidYtelseAggregatBuilder.getAktørYtelseBuilder(aktørId);

            AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(aktivitetsAvtaleFom, aktivitetsAvtaleTom))
                .medProsentsats(aktivitetsAvtaleProsentsats)
                .medAntallTimer(aktivitetsAvtaleAntallTimer)
                .medAntallTimerFulltid(aktivitetsAvtaleAntallTimerFulltid);

            AktivitetsAvtaleBuilder ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(aktivitetsAvtaleFom, aktivitetsAvtaleTom));

            @SuppressWarnings("unused")
            Yrkesaktivitet yrkesaktivitet = yrkesaktivitetBuilder
                .medArbeidType(yrkesaktivitetArbeidType)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(orgNr))
                .medArbeidsforholdId(yrkesaktivitetArbeidsforholdId)
                .tilbakestillAvtaler()
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .leggTilAktivitetsAvtale(ansettelsesperiode)
                .build();

            InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = aktørArbeidBuilder
                .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

            if (ytelseType != null) {
                aktørYtelseBuilder.leggTilYtelse(buildRelaterteYtelserGrunnlag(ytelseType));
                inntektArbeidYtelseAggregatBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
            }

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
