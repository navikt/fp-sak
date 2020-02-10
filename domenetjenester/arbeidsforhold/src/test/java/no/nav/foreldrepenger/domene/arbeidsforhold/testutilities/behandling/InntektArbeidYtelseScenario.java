package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.mockito.Mockito.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.TemaUnderkategori;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.PermisjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
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
import no.nav.vedtak.util.FPDateUtil;

public class InntektArbeidYtelseScenario {

    private InntektArbeidYtelseScenarioTestBuilder inntektArbeidYtelseScenarioTestBuilder;
    private OppgittOpptjeningBuilder oppgittOpptjeningBuilder;

    static VirksomhetRepository mockVirksomhetRepository() {
        return mock(VirksomhetRepository.class);
    }

    public InntektArbeidYtelseScenarioTestBuilder getInntektArbeidYtelseScenarioTestBuilder() {
        if (inntektArbeidYtelseScenarioTestBuilder == null) {
            inntektArbeidYtelseScenarioTestBuilder = getInntektArbeidYtelseScenarioTestBuilder(
                InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER));
        }
        return inntektArbeidYtelseScenarioTestBuilder;
    }

    public InntektArbeidYtelseScenarioTestBuilder getInntektArbeidYtelseScenarioTestBuilder(InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder) {
        return InntektArbeidYtelseScenarioTestBuilder.ny(inntektArbeidYtelseAggregatBuilder);
    }

    void lagreVirksomhet() {
        InntektArbeidYtelseAggregatBuilder kladd = getInntektArbeidYtelseScenarioTestBuilder().getKladd();
        if (kladd != null) {
            InntektArbeidYtelseAggregat build = kladd.build();
            build.getAktørArbeid().stream()
                .map(AktørArbeid::hentAlleYrkesaktiviteter)
                .flatMap(java.util.Collection::stream)
                .forEach(yr -> {
                    if (yr.getArbeidsgiver().getErVirksomhet()) {
                        String orgnr = yr.getArbeidsgiver().getOrgnr();
                        try {
                            Method m = Yrkesaktivitet.class.getDeclaredMethod("setArbeidsgiver", Arbeidsgiver.class);
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
        InntektArbeidYtelseAggregatBuilder kladd = getInntektArbeidYtelseScenarioTestBuilder().getKladd();
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
            VirksomhetRepository virksomhetRepository = repositoryProvider.getVirksomhetRepository();
            OppgittOpptjening oppgittOpptjening = oppgittOpptjeningBuilder.build();
            oppgittOpptjening.getEgenNæring().stream()
                .filter(egenNæring -> egenNæring.getOrgnr() != null)
                .map(OppgittEgenNæring::getOrgnr)
                .forEach(orgnr -> {
                    var virk = virksomhetRepository.hent(orgnr);
                    if (!virk.isPresent()) {
                        virksomhetRepository.lagre(new VirksomhetEntitet.Builder()
                            .medOrgnr(orgnr)
                            .medOrganisasjonstype(Organisasjonstype.erKunstig(orgnr) ? Organisasjonstype.KUNSTIG : Organisasjonstype.VIRKSOMHET)
                            .medNavn("Virksomhet-" + orgnr)
                            .build());
                    }
                });

            repositoryProvider.getInntektArbeidYtelseTjeneste().lagreOppgittOpptjening(behandling.getId(), oppgittOpptjeningBuilder);
        }
    }

    public static class InntektArbeidYtelseScenarioTestBuilder {
        private InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder;

        // Permisjon
        private LocalDate permisjonFom = FPDateUtil.iDag().minusWeeks(9L);
        private LocalDate permisjonTom = FPDateUtil.iDag().minusWeeks(2L);
        private BigDecimal permisjonsprosent = BigDecimal.valueOf(100);
        private PermisjonsbeskrivelseType permisjonsbeskrivelseType = PermisjonsbeskrivelseType.UDEFINERT;

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
        private InternArbeidsforholdRef yrkesaktivitetArbeidsforholdId = InternArbeidsforholdRef.nyRef();

        // Inntekt
        private InntektsKilde inntektsKilde = null;

        // Inntektspost
        private InntektspostType inntektspostType = InntektspostType.UDEFINERT;
        private BigDecimal inntektspostBeløp = BigDecimal.TEN;
        private LocalDate inntektspostFom = FPDateUtil.iDag().minusYears(3L);
        private LocalDate inntektspostTom = FPDateUtil.iDag();
        private YtelseType inntektspostYtelseType = OffentligYtelseType.UDEFINERT;

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

        // Permisjon
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

        // Inntektspost
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

        // Inntekt
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
                    tomDato != null ? DatoIntervallEntitet.fraOgMedTilOgMed(iverksettelsesDato, tomDato) : DatoIntervallEntitet.fraOgMed(iverksettelsesDato))
                .medStatus(relatertYtelseTilstand)
                .medYtelseType(ytelseType)
                .medBehandlingsTema(ytelseBehandlingstema);
        }

        public InntektArbeidYtelseAggregatBuilder buildInntektGrunnlag() {

            InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(aktørId);
            final Opptjeningsnøkkel opptjeningsnøkkel = new Opptjeningsnøkkel(yrkesaktivitetArbeidsforholdId, orgNr, aktørId.getId());
            InntektBuilder inntektBuilder = aktørInntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING, opptjeningsnøkkel);
            InntektspostBuilder inntektspostBuilder = inntektBuilder.getInntektspostBuilder();
            if (inntektsKilde != null) {
                InntektspostBuilder inntektspost = inntektspostBuilder
                    .medBeløp(inntektspostBeløp)
                    .medPeriode(inntektspostFom, inntektspostTom)
                    .medYtelse(inntektspostYtelseType)
                    .medInntektspostType(inntektspostType);

                inntektBuilder
                    .leggTilInntektspost(inntektspost)
                    .medInntektsKilde(inntektsKilde);

                InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntekt = aktørInntektBuilder
                    .leggTilInntekt(inntektBuilder);

                inntektArbeidYtelseAggregatBuilder.leggTilAktørInntekt(aktørInntekt);
            }
            return inntektArbeidYtelseAggregatBuilder;
        }

        public InntektArbeidYtelseAggregatBuilder build() {
            InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntektBuilder = inntektArbeidYtelseAggregatBuilder.getAktørInntektBuilder(aktørId);
            final Opptjeningsnøkkel opptjeningsnøkkel = new Opptjeningsnøkkel(yrkesaktivitetArbeidsforholdId, orgNr, aktørId.getId());
            InntektBuilder inntektBuilder = aktørInntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING, opptjeningsnøkkel);
            InntektspostBuilder inntektspostBuilder = inntektBuilder.getInntektspostBuilder();

            InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(aktørId);
            YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(opptjeningsnøkkel,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
            AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
            PermisjonBuilder permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();

            InntektArbeidYtelseAggregatBuilder.AktørYtelseBuilder aktørYtelseBuilder = inntektArbeidYtelseAggregatBuilder.getAktørYtelseBuilder(aktørId);

            Permisjon permisjon = permisjonBuilder
                .medProsentsats(permisjonsprosent)
                .medPeriode(permisjonFom, permisjonTom)
                .medPermisjonsbeskrivelseType(permisjonsbeskrivelseType)
                .build();

            AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(aktivitetsAvtaleFom, aktivitetsAvtaleTom))
                .medProsentsats(aktivitetsAvtaleProsentsats)
                .medAntallTimer(aktivitetsAvtaleAntallTimer)
                .medAntallTimerFulltid(aktivitetsAvtaleAntallTimerFulltid);

            AktivitetsAvtaleBuilder ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(aktivitetsAvtaleFom, aktivitetsAvtaleTom));

            Yrkesaktivitet yrkesaktivitet = yrkesaktivitetBuilder
                .medArbeidType(yrkesaktivitetArbeidType)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(orgNr))
                .medArbeidsforholdId(yrkesaktivitetArbeidsforholdId)
                .tilbakestillAvtaler()
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .leggTilAktivitetsAvtale(ansettelsesperiode)
                .tilbakestillPermisjon()
                .leggTilPermisjon(permisjon)
                .build();

            InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = aktørArbeidBuilder
                .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

            if (inntektsKilde != null) {
                InntektspostBuilder inntektspost = inntektspostBuilder
                    .medBeløp(inntektspostBeløp)
                    .medPeriode(inntektspostFom, inntektspostTom)
                    .medYtelse(inntektspostYtelseType)
                    .medInntektspostType(inntektspostType);

                inntektBuilder
                    .leggTilInntektspost(inntektspost)
                    .medArbeidsgiver(yrkesaktivitet.getArbeidsgiver())
                    .medInntektsKilde(inntektsKilde);

                InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntekt = aktørInntektBuilder
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
         * Gir den rå buildern for å videre manipulere testdata. på samme måte som entitene bygges på.
         *
         * @return buildern
         */
        public InntektArbeidYtelseAggregatBuilder getKladd() {
            return inntektArbeidYtelseAggregatBuilder;
        }

    }
}
