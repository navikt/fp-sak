package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.fp.InntektsmeldingFilterYtelseImpl;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

public class VurderArbeidsforholdTjenesteImplTest {

    private static final LocalDate IDAG = LocalDate.now();
    private LocalDateTime nåTid = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    private volatile int nåTidTeller;

    private final LocalDate skjæringstidspunkt = IDAG.minusDays(30);

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private IAYRepositoryProvider repositoryProvider = new IAYRepositoryProvider(repositoryRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
    private InntektsmeldingFilterYtelse inntektsmeldingFilterYtelse = new InntektsmeldingFilterYtelseImpl();
    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste = new InntektsmeldingRegisterTjeneste(iayTjeneste, inntektsmeldingTjeneste, null, new UnitTestLookupInstanceImpl<>(inntektsmeldingFilterYtelse));
    private PåkrevdeInntektsmeldingerTjeneste påkrevdeInntektsmeldingerTjeneste = new PåkrevdeInntektsmeldingerTjeneste(inntektsmeldingArkivTjeneste, repositoryProvider.getSøknadRepository());
    private VurderArbeidsforholdTjeneste tjeneste = new VurderArbeidsforholdTjeneste(påkrevdeInntektsmeldingerTjeneste);

    @Before
    public void setup(){
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt() {
        final var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);

        final Behandling behandling = scenario.lagre(repositoryProvider);

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        final InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        final YrkesaktivitetBuilder yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123"));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);
        yrkesBuilder.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef);
        final AktivitetsAvtaleBuilder avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        final AktivitetsAvtaleBuilder avtaleBuilder1 = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder).leggTilAktivitetsAvtale(avtaleBuilder1);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        sendNyInntektsmelding(behandling, virksomhet, ref);

        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();

        avsluttBehandlingOgFagsak(behandling);

        @SuppressWarnings("unused")
        var revurdering = opprettRevurderingsbehandling(behandling);

        sendInnInntektsmelding(behandling, virksomhet, null);

        vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunkt);
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_2() {
        final var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);

        final Behandling behandling = scenario.lagre(repositoryProvider);

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        final InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        final YrkesaktivitetBuilder yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123"));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);
        yrkesBuilder.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef);
        final AktivitetsAvtaleBuilder avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        final AktivitetsAvtaleBuilder avtaleBuilder1 = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder)
            .leggTilAktivitetsAvtale(avtaleBuilder1);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        sendNyInntektsmelding(behandling, virksomhet, ref);

        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();

        avsluttBehandlingOgFagsak(behandling);

        @SuppressWarnings("unused")
        var revurdering = opprettRevurderingsbehandling(behandling);

        sendInnInntektsmelding(behandling, virksomhet, ref);

        vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> hentArbeidsforhold(final Behandling behandling) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        var sakInntektsmeldinger = iayTjeneste.hentInntektsmeldinger(behandling.getFagsak().getSaksnummer());
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder = tjeneste.vurder(lagRef(behandling), iayGrunnlag, sakInntektsmeldinger, true);
        return vurder;
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_3() {
        final var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        final Behandling behandling = scenario.lagre(repositoryProvider);

        final InntektArbeidYtelseAggregatBuilder builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        final InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        final String orgnummer = "123";
        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet(orgnummer));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);

        final YrkesaktivitetBuilder yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(internRef, virksomhet), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesBuilder.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef);
        final AktivitetsAvtaleBuilder avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        final AktivitetsAvtaleBuilder avtaleBuilder3 = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder3.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder).leggTilAktivitetsAvtale(avtaleBuilder3);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        var ref1 = EksternArbeidsforholdRef.ref("ref1");
        var internRef1 = builder.medNyInternArbeidsforholdRef(virksomhet, ref1);
        final YrkesaktivitetBuilder yrkesBuilder1 = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(internRef1, virksomhet), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesBuilder1.medArbeidsgiver(virksomhet)
            .medArbeidsforholdId(internRef1);
        final AktivitetsAvtaleBuilder avtaleBuilder1 = yrkesBuilder1.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
            .medProsentsats(BigDecimal.TEN);
        final AktivitetsAvtaleBuilder avtaleBuilder2 = yrkesBuilder1.getAktivitetsAvtaleBuilder();
        avtaleBuilder2.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder1.leggTilAktivitetsAvtale(avtaleBuilder1).leggTilAktivitetsAvtale(avtaleBuilder2);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder1);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        sendNyInntektsmelding(behandling, virksomhet, ref);
        sendNyInntektsmelding(behandling, virksomhet, ref1);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        var sakInntektsmeldinger = iayTjeneste.hentInntektsmeldinger(behandling.getFagsak().getSaksnummer());
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> vurder = tjeneste.vurder(lagRef(behandling), iayGrunnlag, sakInntektsmeldinger, false);
        assertThat(vurder).isEmpty();
    }

    private void sendNyInntektsmelding(Behandling behandling, Arbeidsgiver arbeidsgiver,  EksternArbeidsforholdRef ref) {
        MottattDokument mottattDokument = new MottattDokument.Builder()
            .medFagsakId(behandling.getFagsakId())
            .medBehandlingId(behandling.getId())
            .medJournalPostId(new JournalpostId("123"))
            .medElektroniskRegistrert(true)
            .medDokumentType(DokumentTypeId.INNTEKTSMELDING)
            .medMottattDato(IDAG).build();
        repositoryProvider.getMottatteDokumentRepository().lagre(mottattDokument);

        InntektsmeldingBuilder inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
        .medArbeidsgiver(arbeidsgiver)
        .medArbeidsforholdId(ref)
        .medBeløp(BigDecimal.TEN)
        .medStartDatoPermisjon(skjæringstidspunkt)
        .medInntektsmeldingaarsak(InntektsmeldingInnsendingsårsak.NY)
        .medInnsendingstidspunkt(nyTid()).medJournalpostId(mottattDokument.getJournalpostId());

        inntektsmeldingTjeneste.lagreInntektsmelding(behandling.getFagsak().getSaksnummer(), behandling.getId(), inntektsmeldingBuilder);
    }

    private LocalDateTime nyTid() {
        return nåTid.plusSeconds((nåTidTeller++));
    }

    private void sendInnInntektsmelding(Behandling behandling, Arbeidsgiver arbeidsgiver, EksternArbeidsforholdRef ref) {
        var inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
        .medArbeidsgiver(arbeidsgiver)
        .medArbeidsforholdId(ref)
        .medBeløp(BigDecimal.TEN)
        .medStartDatoPermisjon(skjæringstidspunkt)
        .medInntektsmeldingaarsak(InntektsmeldingInnsendingsårsak.ENDRING)
        .medInnsendingstidspunkt(nyTid()).medJournalpostId(new JournalpostId("123"));

        inntektsmeldingTjeneste.lagreInntektsmelding(behandling.getFagsak().getSaksnummer(), behandling.getId(), inntektsmeldingBuilder);
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        BehandlingLås lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
        FagsakRepository fagsakRepository = repositoryProvider.getFagsakRepository();
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }

    private Behandling opprettRevurderingsbehandling(Behandling opprinneligBehandling) {
        BehandlingType behandlingType = BehandlingType.REVURDERING;
        BehandlingÅrsak.Builder revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
            .medOriginalBehandlingId(opprinneligBehandling.getId());
        Behandling revurdering = Behandling.fraTidligereBehandling(opprinneligBehandling, behandlingType)
            .medBehandlingÅrsak(revurderingÅrsak).build();
        repositoryProvider.getBehandlingRepository().lagre(revurdering, repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering));
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(opprinneligBehandling.getId(), revurdering.getId());
        return revurdering;
    }

    private String opprettVirksomhet(String orgnummer) {
        return orgnummer;
    }
}
