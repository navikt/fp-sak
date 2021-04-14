package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.TemaUnderkategori;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.OffentligYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.YtelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class InntektArbeidYtelseScenario {

    private InntektArbeidYtelseScenarioTestBuilder inntektArbeidYtelseScenarioTestBuilder;
    private OppgittOpptjeningBuilder oppgittOpptjeningBuilder;

    public InntektArbeidYtelseScenarioTestBuilder getInntektArbeidYtelseScenarioTestBuilder() {
        if (inntektArbeidYtelseScenarioTestBuilder == null) {
            inntektArbeidYtelseScenarioTestBuilder = getInntektArbeidYtelseScenarioTestBuilder(
                    InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER));
        }
        return inntektArbeidYtelseScenarioTestBuilder;
    }

    public InntektArbeidYtelseScenarioTestBuilder getInntektArbeidYtelseScenarioTestBuilder(
            InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder) {
        return InntektArbeidYtelseScenarioTestBuilder.ny(inntektArbeidYtelseAggregatBuilder);
    }

    void lagreVirksomhet() {
        var kladd = getInntektArbeidYtelseScenarioTestBuilder().getKladd();
        if (kladd != null) {
            var build = kladd.build();
            build.getAktørArbeid().stream()
                    .map(AktørArbeid::hentAlleYrkesaktiviteter)
                    .flatMap(java.util.Collection::stream)
                    .forEach(yr -> {
                        if (yr.getArbeidsgiver().getErVirksomhet()) {
                            var orgnr = yr.getArbeidsgiver().getOrgnr();
                            try {
                                var m = Yrkesaktivitet.class.getDeclaredMethod("setArbeidsgiver", Arbeidsgiver.class);
                                m.setAccessible(true);
                                m.invoke(yr, Arbeidsgiver.virksomhet(orgnr));
                            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                throw new IllegalArgumentException("Utvikler feil");
                            }
                        }
                    });
        }
    }

    void lagreOpptjening(IAYRepositoryProvider repositoryProvider, Behandling behandling) {
        var kladd = getInntektArbeidYtelseScenarioTestBuilder().getKladd();
        if (kladd != null) {
            repositoryProvider.getInntektArbeidYtelseTjeneste().lagreIayAggregat(behandling.getId(), kladd);
        }
    }

    public OppgittOpptjeningBuilder medOppgittOpptjening(OppgittOpptjeningBuilder oppgittOpptjeningBuilder) {
        this.oppgittOpptjeningBuilder = oppgittOpptjeningBuilder;
        return oppgittOpptjeningBuilder;
    }

    void lagreOppgittOpptjening(IAYRepositoryProvider repositoryProvider, Behandling behandling) {
        if (oppgittOpptjeningBuilder != null) {
            repositoryProvider.getInntektArbeidYtelseTjeneste().lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        }
    }

    public static class InntektArbeidYtelseScenarioTestBuilder {
        private InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder;

        private LocalDate permisjonFom = LocalDate.now().minusWeeks(9L);
        private LocalDate permisjonTom = LocalDate.now().minusWeeks(2L);
        private BigDecimal permisjonsprosent = BigDecimal.valueOf(100);
        private PermisjonsbeskrivelseType permisjonsbeskrivelseType = PermisjonsbeskrivelseType.UDEFINERT;

        private LocalDate aktivitetsAvtaleFom = LocalDate.now().minusYears(3L);
        private LocalDate aktivitetsAvtaleTom = LocalDate.now();
        private BigDecimal aktivitetsAvtaleProsentsats = BigDecimal.TEN;

        private String orgNr = KUNSTIG_ORG;
        private AktørId aktørId = AktørId.dummy();

        private ArbeidType yrkesaktivitetArbeidType = ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;
        private InternArbeidsforholdRef yrkesaktivitetArbeidsforholdId = InternArbeidsforholdRef.nyRef();

        private InntektsKilde inntektsKilde = null;

        private InntektspostType inntektspostType = InntektspostType.UDEFINERT;
        private BigDecimal inntektspostBeløp = BigDecimal.TEN;
        private LocalDate inntektspostFom = LocalDate.now().minusYears(3L);
        private LocalDate inntektspostTom = LocalDate.now();
        private YtelseType inntektspostYtelseType = OffentligYtelseType.UDEFINERT;

        private RelatertYtelseType ytelseType = null;
        private LocalDate iverksettelsesDato = LocalDate.now().minusYears(5L);
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

        public InntektArbeidYtelseScenarioTestBuilder medPermisjonFom(LocalDate permisjonFom) {
            this.permisjonFom = permisjonFom;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medPermisjonTom(LocalDate permisjonTom) {
            this.permisjonTom = permisjonTom;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medPermisjonProsent(BigDecimal permisjonsprosent) {
            this.permisjonsprosent = permisjonsprosent;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType permisjonsbeskrivelseType) {
            this.permisjonsbeskrivelseType = permisjonsbeskrivelseType;
            return this;
        }

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

        public InntektArbeidYtelseScenarioTestBuilder medOrgNr(String orgNr) {
            this.orgNr = orgNr;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medYrkesaktivitetArbeidType(ArbeidType yrkesaktivitetArbeidType) {
            this.yrkesaktivitetArbeidType = yrkesaktivitetArbeidType;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medInntektspostType(InntektspostType inntektspostType) {
            this.inntektspostType = inntektspostType;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medInntektspostBeløp(BigDecimal inntektspostBeløp) {
            this.inntektspostBeløp = inntektspostBeløp;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medInntektspostFom(LocalDate inntektspostFom) {
            this.inntektspostFom = inntektspostFom;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medInntektspostTom(LocalDate inntektspostTom) {
            this.inntektspostTom = inntektspostTom;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medInntektspostYtelseType(YtelseType ytelseType) {
            this.inntektspostYtelseType = ytelseType;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medInntektsKilde(InntektsKilde inntektsKilde) {
            this.inntektsKilde = inntektsKilde;
            return this;
        }

        // Ytelse (YtelseType må settes)
        public InntektArbeidYtelseScenarioTestBuilder medYtelseType(RelatertYtelseType ytelseType) {
            this.ytelseType = ytelseType;
            return this;
        }

        public InntektArbeidYtelseScenarioTestBuilder medIverksettelseDato(LocalDate dato) {
            this.iverksettelsesDato = dato;
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

        public YtelseBuilder buildRelaterteYtelserGrunnlag(RelatertYtelseType ytelseType) {
            return YtelseBuilder.oppdatere(Optional.empty())
                    .medKilde(ytelseKilde)
                    .medSaksnummer(saksnummer)
                    .medPeriode(
                            tomDato != null ? DatoIntervallEntitet.fraOgMedTilOgMed(iverksettelsesDato, tomDato)
                                    : DatoIntervallEntitet.fraOgMed(iverksettelsesDato))
                    .medStatus(relatertYtelseTilstand)
                    .medYtelseType(ytelseType)
                    .medBehandlingsTema(ytelseBehandlingstema);
        }

        public InntektArbeidYtelseAggregatBuilder buildInntektGrunnlag() {

            var aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder
                    .getAktørInntektBuilder(aktørId);
            final var opptjeningsnøkkel = new Opptjeningsnøkkel(yrkesaktivitetArbeidsforholdId, orgNr, aktørId.getId());
            var inntektBuilder = aktørInntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING, opptjeningsnøkkel);
            var inntektspostBuilder = inntektBuilder.getInntektspostBuilder();
            if (inntektsKilde != null) {
                var inntektspost = inntektspostBuilder
                        .medBeløp(inntektspostBeløp)
                        .medPeriode(inntektspostFom, inntektspostTom)
                        .medYtelse(inntektspostYtelseType)
                        .medInntektspostType(inntektspostType);

                inntektBuilder
                        .leggTilInntektspost(inntektspost)
                        .medInntektsKilde(inntektsKilde);

                var aktørInntekt = aktørInntektBuilder.leggTilInntekt(inntektBuilder);

                inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntekt);
            }
            return inntektArbeidYtelseAggregatBuilder;
        }

        public InntektArbeidYtelseAggregatBuilder build() {
            var aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder
                    .getAktørInntektBuilder(aktørId);
            final var opptjeningsnøkkel = new Opptjeningsnøkkel(yrkesaktivitetArbeidsforholdId, orgNr, aktørId.getId());
            var inntektBuilder = aktørInntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING, opptjeningsnøkkel);
            var inntektspostBuilder = inntektBuilder.getInntektspostBuilder();

            var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder
                    .getAktørArbeidBuilder(aktørId);
            var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel,
                    ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
            var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
            var permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();

            var aktørYtelseBuilder = inntektArbeidYtelseAggregatBuilder
                    .getAktørYtelseBuilder(aktørId);

            var permisjon = permisjonBuilder
                    .medProsentsats(permisjonsprosent)
                    .medPeriode(permisjonFom, permisjonTom)
                    .medPermisjonsbeskrivelseType(permisjonsbeskrivelseType)
                    .build();

            var aktivitetsAvtale = aktivitetsAvtaleBuilder
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(aktivitetsAvtaleFom, aktivitetsAvtaleTom))
                    .medProsentsats(aktivitetsAvtaleProsentsats)
                    .medSisteLønnsendringsdato(aktivitetsAvtaleFom);

            var ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                    .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(aktivitetsAvtaleFom, aktivitetsAvtaleTom));

            var yrkesaktivitet = yrkesaktivitetBuilder
                    .medArbeidType(yrkesaktivitetArbeidType)
                    .medArbeidsgiver(Arbeidsgiver.virksomhet(orgNr))
                    .medArbeidsforholdId(yrkesaktivitetArbeidsforholdId)
                    .tilbakestillAvtaler()
                    .leggTilAktivitetsAvtale(aktivitetsAvtale)
                    .leggTilAktivitetsAvtale(ansettelsesperiode)
                    .tilbakestillPermisjon()
                    .leggTilPermisjon(permisjon)
                    .build();

            var aktørArbeid = aktørArbeidBuilder
                    .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

            if (inntektsKilde != null) {
                var inntektspost = inntektspostBuilder
                        .medBeløp(inntektspostBeløp)
                        .medPeriode(inntektspostFom, inntektspostTom)
                        .medYtelse(inntektspostYtelseType)
                        .medInntektspostType(inntektspostType);

                inntektBuilder
                        .leggTilInntektspost(inntektspost)
                        .medArbeidsgiver(yrkesaktivitet.getArbeidsgiver())
                        .medInntektsKilde(inntektsKilde);

                var aktørInntekt = aktørInntektBuilder
                        .leggTilInntekt(inntektBuilder);

                inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntekt);
            }

            if (ytelseType != null) {
                aktørYtelseBuilder.leggTilYtelse(buildRelaterteYtelserGrunnlag(ytelseType));
                inntektArbeidYtelseAggregatBuilder.leggTilAktørYtelse(aktørYtelseBuilder);
            }

            inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeid);
            return inntektArbeidYtelseAggregatBuilder;
        }

        /**
         * Gir den rå buildern for å videre manipulere testdata. på samme måte som
         * entitene bygges på.
         *
         * @return buildern
         */
        public InntektArbeidYtelseAggregatBuilder getKladd() {
            return inntektArbeidYtelseAggregatBuilder;
        }

    }
}
