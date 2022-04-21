package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.fp.InntektsmeldingFilterYtelseImpl;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsmeldingInnsendingsårsak;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class VurderArbeidsforholdTjenesteTest {

    private static final LocalDate IDAG = LocalDate.now();
    private final LocalDateTime nåTid = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    private final LocalDate skjæringstidspunkt = IDAG.minusDays(30);

    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
    private final InntektsmeldingFilterYtelse inntektsmeldingFilterYtelse = new InntektsmeldingFilterYtelseImpl();
    private final InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste = new InntektsmeldingRegisterTjeneste(iayTjeneste,
            inntektsmeldingTjeneste, null,
            new UnitTestLookupInstanceImpl<>(inntektsmeldingFilterYtelse));
    private VurderArbeidsforholdTjeneste tjeneste;

    private IAYRepositoryProvider repositoryProvider;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        repositoryProvider = new IAYRepositoryProvider(entityManager);
        var påkrevdeInntektsmeldingerTjeneste = new PåkrevdeInntektsmeldingerTjeneste(inntektsmeldingArkivTjeneste,
                repositoryProvider.getSøknadRepository());
        tjeneste = new VurderArbeidsforholdTjeneste(påkrevdeInntektsmeldingerTjeneste);
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt() {
        final var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);

        final var behandling = scenario.lagre(repositoryProvider);

        final var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        final var arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        final var yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        final var virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123"));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);
        yrkesBuilder.medArbeidsgiver(virksomhet)
                .medArbeidsforholdId(internRef);
        final var avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
                .medProsentsats(BigDecimal.TEN);
        final var avtaleBuilder1 = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder).leggTilAktivitetsAvtale(avtaleBuilder1);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        sendNyInntektsmelding(behandling, virksomhet, ref, nåTid);

        var vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();

        avsluttBehandlingOgFagsak(behandling);

        opprettRevurderingsbehandling(behandling);

        sendInnInntektsmelding(behandling, virksomhet, null, nåTid.plusSeconds(1));

        vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(skjæringstidspunkt).build());
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_2() {
        final var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);

        final var behandling = scenario.lagre(repositoryProvider);

        final var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        final var arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        final var yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        final var virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet("123"));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);
        yrkesBuilder.medArbeidsgiver(virksomhet)
                .medArbeidsforholdId(internRef);
        final var avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
                .medProsentsats(BigDecimal.TEN);
        final var avtaleBuilder1 = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder)
                .leggTilAktivitetsAvtale(avtaleBuilder1);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        sendNyInntektsmelding(behandling, virksomhet, ref, nåTid);

        var vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();

        avsluttBehandlingOgFagsak(behandling);

        opprettRevurderingsbehandling(behandling);

        sendInnInntektsmelding(behandling, virksomhet, ref, nåTid);

        vurder = hentArbeidsforhold(behandling);
        assertThat(vurder).isEmpty();
    }

    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> hentArbeidsforhold(final Behandling behandling) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        var sakInntektsmeldinger = iayTjeneste.hentInntektsmeldinger(behandling.getFagsak().getSaksnummer());
        return tjeneste.vurder(lagRef(behandling), iayGrunnlag, sakInntektsmeldinger, true);
    }

    @Test
    public void skal_ikke_gi_aksjonspunkt_3() {
        final var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        final var behandling = scenario.lagre(repositoryProvider);

        final var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        final var arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        final var orgnummer = "123";
        final var virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet(orgnummer));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);

        final var yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
                Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(internRef, virksomhet), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesBuilder.medArbeidsgiver(virksomhet)
                .medArbeidsforholdId(internRef);
        final var avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
                .medProsentsats(BigDecimal.TEN);
        final var avtaleBuilder3 = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder3.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder).leggTilAktivitetsAvtale(avtaleBuilder3);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);
        var ref1 = EksternArbeidsforholdRef.ref("ref1");
        var internRef1 = builder.medNyInternArbeidsforholdRef(virksomhet, ref1);
        final var yrkesBuilder1 = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
                Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(internRef1, virksomhet), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesBuilder1.medArbeidsgiver(virksomhet)
                .medArbeidsforholdId(internRef1);
        final var avtaleBuilder1 = yrkesBuilder1.getAktivitetsAvtaleBuilder();
        avtaleBuilder1.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
                .medProsentsats(BigDecimal.TEN);
        final var avtaleBuilder2 = yrkesBuilder1.getAktivitetsAvtaleBuilder();
        avtaleBuilder2.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)));
        yrkesBuilder1.leggTilAktivitetsAvtale(avtaleBuilder1).leggTilAktivitetsAvtale(avtaleBuilder2);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder1);
        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        sendNyInntektsmelding(behandling, virksomhet, ref, nåTid);
        sendNyInntektsmelding(behandling, virksomhet, ref1, nåTid.plusSeconds(1));

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        var sakInntektsmeldinger = iayTjeneste.hentInntektsmeldinger(behandling.getFagsak().getSaksnummer());
        var vurder = tjeneste.vurder(lagRef(behandling), iayGrunnlag, sakInntektsmeldinger, false);
        assertThat(vurder).isEmpty();
    }

    @Test
    public void skal_gi_aksjonspunkt_for_fiske_uten_aktivt_arbeid_med_inntektsmelding() {
        final var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.FORELDREPENGER);
        final var behandling = scenario.lagre(repositoryProvider);

        final var builder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        final var arbeidBuilder = builder.getAktørArbeidBuilder(behandling.getAktørId());
        final var orgnummer = "123";
        final var virksomhet = Arbeidsgiver.virksomhet(opprettVirksomhet(orgnummer));
        var ref = EksternArbeidsforholdRef.ref("ref");
        var internRef = builder.medNyInternArbeidsforholdRef(virksomhet, ref);

        final var yrkesBuilder = arbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
                Opptjeningsnøkkel.forArbeidsforholdIdMedArbeidgiver(internRef, virksomhet), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesBuilder.medArbeidsgiver(virksomhet)
                .medArbeidsforholdId(internRef);
        final var avtaleBuilder = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMed(IDAG.minusYears(1)))
                .medProsentsats(BigDecimal.TEN);
        final var avtaleBuilder3 = yrkesBuilder.getAktivitetsAvtaleBuilder();
        avtaleBuilder3.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(12), skjæringstidspunkt.minusDays(10)));
        yrkesBuilder.leggTilAktivitetsAvtale(avtaleBuilder).leggTilAktivitetsAvtale(avtaleBuilder3);
        arbeidBuilder.leggTilYrkesaktivitet(yrkesBuilder);

        builder.leggTilAktørArbeid(arbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(),
                OppgittOpptjeningBuilder.ny().leggTilEgneNæringer(List.of(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
                        .medPeriode(DatoIntervallEntitet.fraOgMed(skjæringstidspunkt.minusMonths(1)))
                        .medVirksomhetType(VirksomhetType.FISKE))));

        sendNyInntektsmelding(behandling, virksomhet, ref, nåTid);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        var sakInntektsmeldinger = iayTjeneste.hentInntektsmeldinger(behandling.getFagsak().getSaksnummer());
        var vurder = tjeneste.vurder(lagRef(behandling), iayGrunnlag, sakInntektsmeldinger, false);
        assertThat(vurder).isNotEmpty();
    }

    private void sendNyInntektsmelding(Behandling behandling,
            Arbeidsgiver arbeidsgiver,
            EksternArbeidsforholdRef ref,
            LocalDateTime innsendingstidspunkt) {
        var mottattDokument = new MottattDokument.Builder()
                .medFagsakId(behandling.getFagsakId())
                .medBehandlingId(behandling.getId())
                .medJournalPostId(new JournalpostId("123"))
                .medElektroniskRegistrert(true)
                .medDokumentType(DokumentTypeId.INNTEKTSMELDING)
                .medMottattDato(IDAG).build();
        repositoryProvider.getMottatteDokumentRepository().lagre(mottattDokument);

        var inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(ref)
                .medBeløp(BigDecimal.TEN)
                .medStartDatoPermisjon(skjæringstidspunkt)
                .medInntektsmeldingaarsak(InntektsmeldingInnsendingsårsak.NY)
                .medInnsendingstidspunkt(innsendingstidspunkt)
                .medJournalpostId(mottattDokument.getJournalpostId());

        inntektsmeldingTjeneste.lagreInntektsmelding(behandling.getFagsak().getSaksnummer(), behandling.getId(), inntektsmeldingBuilder);
    }

    private void sendInnInntektsmelding(Behandling behandling, Arbeidsgiver arbeidsgiver, EksternArbeidsforholdRef ref,
            LocalDateTime innsendingstidspunkt) {
        var inntektsmeldingBuilder = InntektsmeldingBuilder.builder()
                .medArbeidsgiver(arbeidsgiver)
                .medArbeidsforholdId(ref)
                .medBeløp(BigDecimal.TEN)
                .medStartDatoPermisjon(skjæringstidspunkt)
                .medInntektsmeldingaarsak(InntektsmeldingInnsendingsårsak.ENDRING)
                .medInnsendingstidspunkt(innsendingstidspunkt)
                .medJournalpostId(new JournalpostId("123"));

        inntektsmeldingTjeneste.lagreInntektsmelding(behandling.getFagsak().getSaksnummer(), behandling.getId(), inntektsmeldingBuilder);
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        var lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        behandling.avsluttBehandling();
        repositoryProvider.getBehandlingRepository().lagre(behandling, lås);
        var fagsakRepository = repositoryProvider.getFagsakRepository();
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }

    private Behandling opprettRevurderingsbehandling(Behandling opprinneligBehandling) {
        var behandlingType = BehandlingType.REVURDERING;
        var revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
                .medOriginalBehandlingId(opprinneligBehandling.getId());
        var revurdering = Behandling.fraTidligereBehandling(opprinneligBehandling, behandlingType)
                .medBehandlingÅrsak(revurderingÅrsak).build();
        repositoryProvider.getBehandlingRepository().lagre(revurdering, repositoryProvider.getBehandlingRepository().taSkriveLås(revurdering));
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(opprinneligBehandling.getId(), revurdering.getId());
        return revurdering;
    }

    private String opprettVirksomhet(String orgnummer) {
        return orgnummer;
    }
}
