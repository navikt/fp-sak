package no.nav.foreldrepenger.domene.arbeidsforhold.fp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.abakus.ArbeidsforholdTjenesteMock;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.person.PersonIdentTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.PermisjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.SkatteOgAvgiftsregelType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

public class InntektsmeldingTjenesteImplTest {

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final EksternArbeidsforholdRef ARBEIDSFORHOLD_ID_EKSTERN = EksternArbeidsforholdRef.ref("1");
    private static final AktørId AKTØRID = AktørId.dummy();
    private static final LocalDate I_DAG = LocalDate.now();
    private static final LocalDate ARBEIDSFORHOLD_FRA = I_DAG.minusMonths(3);
    private static final LocalDate ARBEIDSFORHOLD_TIL = I_DAG.plusMonths(2);
    private static BigDecimal LØNNSPOST = BigDecimal.TEN;

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(I_DAG).build();
    private final AtomicLong journalpostIdInc = new AtomicLong(123);
    private IAYRepositoryProvider repositoryProvider = new IAYRepositoryProvider(repoRule.getEntityManager());
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private FagsakRepository fagsakRepository = new FagsakRepository(repoRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
    private PersonIdentTjeneste tpsTjeneste;
    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste;
    private Arbeidsgiver arbeidsgiver;
    private ArbeidsforholdTjenesteMock arbeidsforholdTjenesteMock;
    private Arbeidsgiver arbeidsgiver2;

    @Before
    public void setUp() throws Exception {
        var virksomhet1 = lagVirksomhet();
        var virksomhet2 = lagAndreVirksomhet();

        this.arbeidsgiver = Arbeidsgiver.virksomhet(virksomhet1.getOrgnr());
        tpsTjeneste = mock(PersonIdentTjeneste.class);

        var virksomhetTjeneste = mock(VirksomhetTjeneste.class);
        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(virksomhet1);
        arbeidsforholdTjenesteMock = new ArbeidsforholdTjenesteMock(false);
        var vurderArbeidsforholdTjeneste = mock(VurderArbeidsforholdTjeneste.class);
        arbeidsgiver2 = Arbeidsgiver.virksomhet(virksomhet2.getOrgnr());
        Set<InternArbeidsforholdRef> arbeidsforholdRefSet = new HashSet<>();
        arbeidsforholdRefSet.add(ARBEIDSFORHOLD_ID);
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> arbeidsgiverSetMap = new HashMap<>();
        arbeidsgiverSetMap.put(arbeidsgiver2, arbeidsforholdRefSet);
        when(vurderArbeidsforholdTjeneste.vurder(any(), any(), any(), Mockito.anyBoolean())).thenReturn(arbeidsgiverSetMap);
        var foreldrepengerFilter = new InntektsmeldingFilterYtelseImpl();

        this.inntektsmeldingArkivTjeneste = new InntektsmeldingRegisterTjeneste(iayTjeneste,
            inntektsmeldingTjeneste, arbeidsforholdTjenesteMock.getMock(), new UnitTestLookupInstanceImpl<>(foreldrepengerFilter));
    }

    @Test
    public void skal_vurdere_om_inntektsmeldinger_er_komplett() {
        var arbId1Intern = ARBEIDSFORHOLD_ID;
        var arbId1 = ARBEIDSFORHOLD_ID_EKSTERN;
        // Arrange
        final Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL),
            arbId1Intern, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);

        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        // Act+Assert
        assertThat(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(behandlingReferanse, false)).isNotEmpty();

        lagreInntektsmelding(I_DAG.minusDays(2), behandling, arbId1Intern, arbId1);

        // Act+Assert
        assertThat(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(behandlingReferanse, false)).isEmpty();
    }

    @Test
    public void skal_ta_hensyn_til_arbeidsforhold_med_inntekt() {
        // Arrange
        final Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL),
            ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);

        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        // Act+Assert
        assertThat(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, false)).isNotEmpty();
    }

    @Test
    public void skal_ikke_ta_hensyn_til_arbeidsforhold_uten_inntekt() {
        // Arrange
        final Behandling behandling = opprettBehandling();
        LØNNSPOST = BigDecimal.ZERO;
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL),
            ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);

        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        // Act+Assert
        assertThat(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, false)).isEmpty();
        LØNNSPOST = BigDecimal.TEN;
    }

    @Test
    public void skal_ikke_ta_hensyn_til_arbeidsforhold_ikke_gyldig_stp() throws Exception {
        // Arrange
        final Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_FRA.plusWeeks(1)),
            ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);

        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        // Act+Assert
        assertThat(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, false)).isEmpty();
    }

    private BehandlingReferanse lagReferanse(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunkt);
    }

    @Test
    public void skal_utelate_inntektsmeldinger_som_er_mottatt_i_førstegangsbehandlingen_ved_revurdering() {

        // Arrange
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL),
            ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);
        lagreInntektsmelding(I_DAG.minusDays(2), behandling, ARBEIDSFORHOLD_ID, ARBEIDSFORHOLD_ID_EKSTERN);
        avsluttBehandlingOgFagsak(behandling);

        Behandling revurdering = opprettRevurderingsbehandling(behandling);
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), revurdering.getId());

        BehandlingReferanse behandlingReferanse = lagReferanse(revurdering);

        // Act+Assert
        assertThat(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(behandlingReferanse, true)).isNotEmpty();

        lagreInntektsmelding(I_DAG, revurdering, ARBEIDSFORHOLD_ID, ARBEIDSFORHOLD_ID_EKSTERN);

        // Act+Assert
        assertThat(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(behandlingReferanse, true)).isEmpty();

        final List<Inntektsmelding> nyeInntektsmeldinger = inntektsmeldingTjeneste.hentInntektsmeldinger(behandlingReferanse, I_DAG);
        assertThat(nyeInntektsmeldinger).hasSize(1);
    }

    @Test
    public void skal_finne_inntektsmeldinger_etter_gjeldende_behandling() {
        var arbId1 = ARBEIDSFORHOLD_ID_EKSTERN;
        var arbId2 = EksternArbeidsforholdRef.ref("2");
        var arbId3 = EksternArbeidsforholdRef.ref("2");

        var arbId1Intern = ARBEIDSFORHOLD_ID;
        var arbId2Intern = InternArbeidsforholdRef.nyRef();
        var arbId3Intern = InternArbeidsforholdRef.nyRef();

        // Arrange
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL),
            ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);
        lagreInntektsmelding(I_DAG.minusDays(2), behandling, arbId1Intern, arbId1);
        lagreInntektsmelding(I_DAG.minusDays(3), behandling, arbId2Intern, arbId2);
        avsluttBehandlingOgFagsak(behandling);

        BehandlingReferanse ref = lagReferanse(behandling);
        List<Inntektsmelding> inntektsmeldingerFørGjeldendeVedtak = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, I_DAG);

        Behandling revurdering = opprettRevurderingsbehandling(behandling);
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(behandling.getId(), revurdering.getId());
        lagreInntektsmelding(I_DAG.plusWeeks(2), revurdering, arbId1Intern, arbId1);
        lagreInntektsmelding(I_DAG.plusWeeks(3), revurdering, arbId3Intern, arbId3);

        // Act+Assert
        BehandlingReferanse refRevurdering = lagReferanse(revurdering);
        List<Inntektsmelding> inntektsmeldingerEtterGjeldendeVedtak = inntektsmeldingTjeneste
            .hentAlleInntektsmeldingerMottattEtterGjeldendeVedtak(refRevurdering);
        assertThat(inntektsmeldingerEtterGjeldendeVedtak).hasSize(2);
        assertThat(erDisjonkteListerAvInntektsmeldinger(inntektsmeldingerFørGjeldendeVedtak, inntektsmeldingerEtterGjeldendeVedtak)).isTrue();
    }

    @Test
    public void skal_fjerne_inntektsmelding_når_arbeidsforhold_blir_inaktivt() {
        // Arrange
        Behandling behandling = opprettBehandling();
        LocalDate skjæringstidspunktet = I_DAG.minusDays(1);
        BehandlingReferanse ref = lagReferanse(behandling);

        // Act
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL),
            ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);
        lagreInntektsmelding(I_DAG.minusDays(2), behandling, ARBEIDSFORHOLD_ID, ARBEIDSFORHOLD_ID_EKSTERN);

        // Assert
        assertThat(inntektsmeldingTjeneste.hentInntektsmeldinger(ref, I_DAG)).hasSize(1);

        // Arrange: Arbeidsforholdet blir oppdater i AA-reg, blir inaktivt
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(AKTØRID);
        YrkesaktivitetBuilder yrkesaktivitetBuilderForType = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilderForType
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL), true);
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, skjæringstidspunktet.minusDays(1)));
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);

        // Act
        List<Inntektsmelding> inntektsmeldingerPåAktiveArbeidsforhold = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, I_DAG);

        // Assert
        assertThat(inntektsmeldingerPåAktiveArbeidsforhold).isEmpty();
    }

    @Test
    public void skal_ikke_fjerne_inntektsmelding_for_arbeidsforhold_som_tilkommer_etter_skjæringstidspunktet() {
        // Arrange
        Behandling behandling = opprettBehandling();
        LocalDate skjæringstidspunktet = I_DAG.minusDays(1);
        BehandlingReferanse ref = lagReferanse(behandling);

        // Act
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID,
            DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunktet.plusWeeks(1), ARBEIDSFORHOLD_TIL), ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
            BigDecimal.TEN);
        lagreInntektsmelding(I_DAG.minusDays(2), behandling, ARBEIDSFORHOLD_ID, ARBEIDSFORHOLD_ID_EKSTERN);

        // Assert
        assertThat(inntektsmeldingTjeneste.hentInntektsmeldinger(ref, I_DAG)).hasSize(1);

        // Arrange: Arbeidsforholdet blir oppdater i AA-reg, blir inaktivt
        InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(AKTØRID);
        YrkesaktivitetBuilder yrkesaktivitetBuilderForType = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilderForType
            .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL), true);
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, skjæringstidspunktet.minusDays(1)));
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);

        // Act
        List<Inntektsmelding> inntektsmeldingerPåAktiveArbeidsforhold = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, I_DAG);

        // Assert
        assertThat(inntektsmeldingerPåAktiveArbeidsforhold).hasSize(1);
    }

    @Test
    public void skal_ikke_fjerne_inntektsmelding_for_arbeidsforhold_som_ikke_har_vært_aktivt_i_opplysningsperioden() {
        // Arrange
        Behandling behandling = opprettBehandling();
        LocalDate skjæringstidspunktet = I_DAG.minusDays(1);
        BehandlingReferanse ref = lagReferanse(behandling);

        // Act
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID,
            DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunktet.plusWeeks(1), ARBEIDSFORHOLD_TIL), ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
            BigDecimal.TEN);
        lagreInntektsmelding(I_DAG.minusDays(2), behandling, ARBEIDSFORHOLD_ID, ARBEIDSFORHOLD_ID_EKSTERN, BigDecimal.TEN, arbeidsgiver2);

        // Assert
        assertThat(inntektsmeldingTjeneste.hentInntektsmeldinger(ref, I_DAG)).hasSize(1);

    }


    private boolean erDisjonkteListerAvInntektsmeldinger(List<Inntektsmelding> imsA, List<Inntektsmelding> imsB) {
        return Collections.disjoint(
            imsA.stream().map(Inntektsmelding::getJournalpostId).collect(Collectors.toList()),
            imsB.stream().map(Inntektsmelding::getJournalpostId).collect(Collectors.toList()));
    }

    private void lagreInntektsmelding(LocalDate mottattDato, Behandling behandling, InternArbeidsforholdRef arbeidsforholdIdIntern, EksternArbeidsforholdRef arbeidsforholdId) {
        lagreInntektsmelding(mottattDato, behandling, arbeidsforholdIdIntern, arbeidsforholdId, BigDecimal.TEN, arbeidsgiver);
    }

    private void lagreInntektsmelding(LocalDate mottattDato, Behandling behandling, InternArbeidsforholdRef arbeidsforholdIdIntern, EksternArbeidsforholdRef arbeidsforholdId, BigDecimal beløp, Arbeidsgiver arbeidsgiver) {
        JournalpostId journalPostId = new JournalpostId(journalpostIdInc.getAndIncrement());

        var inntektsmelding = InntektsmeldingBuilder.builder()
            .medStartDatoPermisjon(I_DAG)
            .medArbeidsgiver(arbeidsgiver)
            .medBeløp(beløp)
            .medNærRelasjon(false)
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(arbeidsforholdIdIntern)
            .medInnsendingstidspunkt(LocalDateTime.of(mottattDato, LocalTime.MIN))
            .medJournalpostId(journalPostId);

        inntektsmeldingTjeneste.lagreInntektsmelding(behandling.getFagsak().getSaksnummer(), behandling.getId(), inntektsmelding);

    }

    private void opprettOppgittOpptjening(Behandling behandling) {
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.minusMonths(2), I_DAG.plusMonths(1));
        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);
    }

    private void opprettInntektArbeidYtelseAggregatForYrkesaktivitet(Behandling behandling, AktørId aktørId,
                                                                     DatoIntervallEntitet periode,
                                                                     InternArbeidsforholdRef arbeidsforhold,
                                                                     ArbeidType type, BigDecimal prosentsats) {

        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);

        leggTilYrekesaktivitet(aktørArbeidBuilder, arbeidsforhold, type, prosentsats, periode, periode);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntektBuilder = builder.getAktørInntektBuilder(aktørId);
        leggTilInntekt(aktørInntektBuilder, periode);
        builder.leggTilAktørInntekt(aktørInntektBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder leggTilYrekesaktivitet(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder builder,
                                                                                         InternArbeidsforholdRef ref, ArbeidType type, BigDecimal prosentsats,
                                                                                         DatoIntervallEntitet periodeYA, DatoIntervallEntitet periodeAA) {
        YrkesaktivitetBuilder yrkesaktivitetBuilder = builder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(ref, arbeidsgiver.getIdentifikator(), null),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periodeAA, false);
        PermisjonBuilder permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();

        AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder
            .medPeriode(periodeAA)
            .medProsentsats(prosentsats)
            .medAntallTimer(BigDecimal.valueOf(20.4d))
            .medAntallTimerFulltid(BigDecimal.valueOf(10.2d))
            .medBeskrivelse("Ser greit ut");
        AktivitetsAvtaleBuilder ansettelsesPeriode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periodeYA, true);

        Permisjon permisjon = permisjonBuilder
            .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.UTDANNINGSPERMISJON)
            .medPeriode(periodeYA.getFomDato(), periodeYA.getTomDato())
            .medProsentsats(BigDecimal.valueOf(100))
            .build();

        yrkesaktivitetBuilder
            .medArbeidType(type)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(ARBEIDSFORHOLD_ID)
            .leggTilPermisjon(permisjon)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesPeriode);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = builder
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        return aktørArbeid;
    }

    private InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder leggTilInntekt(InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder builder,
                                                                                  DatoIntervallEntitet periodeInntekt) {
        InntektBuilder inntektBuilder = builder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING,
            new Opptjeningsnøkkel(InternArbeidsforholdRef.nullRef(), arbeidsgiver.getIdentifikator(), null));

        InntektspostBuilder inntektspostBuilder = inntektBuilder.getInntektspostBuilder()
            .medInntektspostType(InntektspostType.LØNN)
            .medBeløp(LØNNSPOST)
            .medSkatteOgAvgiftsregelType(SkatteOgAvgiftsregelType.UDEFINERT)
            .medPeriode(periodeInntekt.getFomDato(), periodeInntekt.getTomDato());

        inntektBuilder
            .medArbeidsgiver(arbeidsgiver)
            .leggTilInntektspost(inntektspostBuilder);

        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder aktørInntekt = builder
            .leggTilInntekt(inntektBuilder);

        return aktørInntekt;
    }

    private Virksomhet lagVirksomhet() {
        return new Virksomhet.Builder()
            .medOrgnr(KUNSTIG_ORG)
            .medNavn("Virksomheten")
            .medRegistrert(I_DAG.minusYears(2L))
            .medOppstart(I_DAG.minusYears(1L))
            .build();
    }

    private Virksomhet lagAndreVirksomhet() {
        return new Virksomhet.Builder()
            .medOrgnr("52")
            .medNavn("OrgA")
            .medRegistrert(I_DAG.minusYears(2L))
            .medOppstart(I_DAG.minusYears(1L))
            .build();
    }

    private Behandling opprettBehandling() {
        return opprettBehandling(opprettFagsak());
    }

    private Behandling opprettBehandling(Fagsak fagsak) {
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private Fagsak opprettFagsak() {
        final Personinfo personinfo = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AKTØRID)
            .medFødselsdato(I_DAG.minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12312"))
            .medForetrukketSpråk(Språkkode.NB)
            .build();
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo), RelasjonsRolleType.MORA, new Saksnummer("123"));
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, lås);
        FagsakRepository fagsakRepository = repositoryProvider.getFagsakRepository();
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }

    private Behandling opprettRevurderingsbehandling(Behandling opprinneligBehandling) {
        BehandlingType behandlingType = BehandlingType.REVURDERING;
        BehandlingÅrsak.Builder revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
            .medOriginalBehandlingId(opprinneligBehandling.getId());
        Behandling revurdering = Behandling.fraTidligereBehandling(opprinneligBehandling, behandlingType)
            .medBehandlingÅrsak(revurderingÅrsak).build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
        return revurdering;
    }
}
