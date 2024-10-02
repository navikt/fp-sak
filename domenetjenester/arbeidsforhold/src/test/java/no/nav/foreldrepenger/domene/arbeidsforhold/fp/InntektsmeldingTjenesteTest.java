package no.nav.foreldrepenger.domene.arbeidsforhold.fp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding.NAV_NO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.abakus.ArbeidsforholdTjenesteMock;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.fpinntektsmelding.FpInntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.SkatteOgAvgiftsregelType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@ExtendWith(JpaExtension.class)
class InntektsmeldingTjenesteTest {

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final EksternArbeidsforholdRef ARBEIDSFORHOLD_ID_EKSTERN = EksternArbeidsforholdRef.ref("1");
    private static final AktørId AKTØRID = AktørId.dummy();
    private static final LocalDate I_DAG = LocalDate.of(2022,1,27);
    private static final LocalDate ARBEIDSFORHOLD_FRA = I_DAG.minusMonths(3);
    private static final LocalDate ARBEIDSFORHOLD_TIL = I_DAG.plusMonths(2);
    private static BigDecimal LØNNSPOST = BigDecimal.TEN;

    private final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(I_DAG)
        .medKreverSammenhengendeUttak(false).build();
    private final AtomicLong journalpostIdInc = new AtomicLong(123);
    private IAYRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste, new FpInntektsmeldingTjeneste());
    private InntektsmeldingRegisterTjeneste inntektsmeldingArkivTjeneste;
    private Arbeidsgiver arbeidsgiver;
    private Arbeidsgiver arbeidsgiver2;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        fagsakRepository = new FagsakRepository(entityManager);
        repositoryProvider = new IAYRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();

        var virksomhet1 = lagVirksomhet();
        var virksomhet2 = lagAndreVirksomhet();

        this.arbeidsgiver = Arbeidsgiver.virksomhet(virksomhet1.getOrgnr());

        var virksomhetTjeneste = mock(VirksomhetTjeneste.class);
        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(virksomhet1);
        var arbeidsforholdTjenesteMock = new ArbeidsforholdTjenesteMock(false);
        arbeidsgiver2 = Arbeidsgiver.virksomhet(virksomhet2.getOrgnr());
        var foreldrepengerFilter = new InntektsmeldingFilterYtelseImpl();
        this.inntektsmeldingArkivTjeneste = new InntektsmeldingRegisterTjeneste(iayTjeneste,
                inntektsmeldingTjeneste, arbeidsforholdTjenesteMock.getMock(), new UnitTestLookupInstanceImpl<>(foreldrepengerFilter));
    }

    @Test
    void skal_vurdere_om_inntektsmeldinger_er_komplett() {
        // Arrange
        var behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID,
                DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL),
                ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);

        var behandlingReferanse = lagReferanse(behandling);

        // Act+Assert
        assertThat(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(behandlingReferanse, skjæringstidspunkt)).isNotEmpty();

        lagreInntektsmelding(I_DAG.minusDays(2), behandling, ARBEIDSFORHOLD_ID, ARBEIDSFORHOLD_ID_EKSTERN);

        // Act+Assert
        assertThat(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraAAreg(behandlingReferanse, skjæringstidspunkt)).isEmpty();
    }

    @Test
    void skal_ta_hensyn_til_arbeidsforhold_med_inntekt() {
        // Arrange
        var behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID,
                DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL),
                ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);

        var behandlingReferanse = lagReferanse(behandling);

        // Act+Assert
        assertThat(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, skjæringstidspunkt)).isNotEmpty();
    }

    @Test
    void skal_ikke_ta_hensyn_til_arbeidsforhold_uten_inntekt() {
        // Arrange
        var behandling = opprettBehandling();
        LØNNSPOST = BigDecimal.ZERO;
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID,
                DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA.minusMonths(3), ARBEIDSFORHOLD_TIL),
                ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);

        var behandlingReferanse = lagReferanse(behandling);

        // Act+Assert
        assertThat(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, skjæringstidspunkt)).isEmpty();
        LØNNSPOST = BigDecimal.TEN;
    }

    @Test
    void skal_ikke_ta_hensyn_til_arbeidsforhold_ikke_gyldig_stp() {
        // Arrange
        var behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID,
                DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_FRA.plusWeeks(1)),
                ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);

        var behandlingReferanse = lagReferanse(behandling);

        // Act+Assert
        assertThat(inntektsmeldingArkivTjeneste.utledManglendeInntektsmeldingerFraGrunnlag(behandlingReferanse, skjæringstidspunkt)).isEmpty();
    }

    private BehandlingReferanse lagReferanse(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

    @Test
    void skal_fjerne_inntektsmelding_når_arbeidsforhold_blir_inaktivt() {
        // Arrange
        var behandling = opprettBehandling();
        var skjæringstidspunktet = I_DAG.minusDays(1);
        var ref = lagReferanse(behandling);

        // Act
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID,
                DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL),
                ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);
        lagreInntektsmelding(I_DAG.minusDays(2), behandling, ARBEIDSFORHOLD_ID, ARBEIDSFORHOLD_ID_EKSTERN);

        // Assert
        assertThat(inntektsmeldingTjeneste.hentInntektsmeldinger(ref, I_DAG)).hasSize(1);

        // Arrange: Arbeidsforholdet blir oppdater i AA-reg, blir inaktivt
        var inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(AKTØRID);
        var yrkesaktivitetBuilderForType = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilderForType
                .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL), true);
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, skjæringstidspunktet.minusDays(1)));
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);

        // Act
        var inntektsmeldingerPåAktiveArbeidsforhold = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, I_DAG);

        // Assert
        assertThat(inntektsmeldingerPåAktiveArbeidsforhold).isEmpty();
    }

    @Test
    void skal_ikke_fjerne_inntektsmelding_for_arbeidsforhold_som_tilkommer_etter_skjæringstidspunktet() {
        // Arrange
        var behandling = opprettBehandling();
        var skjæringstidspunktet = I_DAG.minusDays(1);
        var ref = lagReferanse(behandling);

        // Act
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID,
                DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunktet.plusWeeks(1), ARBEIDSFORHOLD_TIL), ARBEIDSFORHOLD_ID,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
                BigDecimal.TEN);
        lagreInntektsmelding(I_DAG.minusDays(2), behandling, ARBEIDSFORHOLD_ID, ARBEIDSFORHOLD_ID_EKSTERN);

        // Assert
        assertThat(inntektsmeldingTjeneste.hentInntektsmeldinger(ref, I_DAG)).hasSize(1);

        // Arrange: Arbeidsforholdet blir oppdater i AA-reg, blir inaktivt
        var inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(AKTØRID);
        var yrkesaktivitetBuilderForType = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilderForType
                .getAktivitetsAvtaleBuilder(DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL), true);
        aktivitetsAvtaleBuilder.medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, skjæringstidspunktet.minusDays(1)));
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);

        // Act
        var inntektsmeldingerPåAktiveArbeidsforhold = inntektsmeldingTjeneste.hentInntektsmeldinger(ref, I_DAG);

        // Assert
        assertThat(inntektsmeldingerPåAktiveArbeidsforhold).hasSize(1);
    }

    @Test
    void skal_ikke_fjerne_inntektsmelding_for_arbeidsforhold_som_ikke_har_vært_aktivt_i_opplysningsperioden() {
        // Arrange
        var behandling = opprettBehandling();
        var skjæringstidspunktet = I_DAG.minusDays(1);
        var ref = lagReferanse(behandling);

        // Act
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID,
                DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunktet.plusWeeks(1), ARBEIDSFORHOLD_TIL), ARBEIDSFORHOLD_ID,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
                BigDecimal.TEN);
        lagreInntektsmelding(I_DAG.minusDays(2), behandling, ARBEIDSFORHOLD_ID, ARBEIDSFORHOLD_ID_EKSTERN, BigDecimal.TEN, arbeidsgiver2);

        // Assert
        assertThat(inntektsmeldingTjeneste.hentInntektsmeldinger(ref, I_DAG)).hasSize(1);

    }

    @Test
    void skal_ikke_ta_med_eldre_inntektsmelding_startdato() {
        // Arrange
        var behandling = opprettBehandling();
        var skjæringstidspunktet = I_DAG.minusDays(1);
        var ref = lagReferanse(behandling);

        // Act
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID,
            DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL),
            ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);

        lagreInntektsmelding(skjæringstidspunktet.minusMonths(1).minusWeeks(5), behandling, ARBEIDSFORHOLD_ID, ARBEIDSFORHOLD_ID_EKSTERN, BigDecimal.TEN, arbeidsgiver, skjæringstidspunktet.minusMonths(1),
            NAV_NO);
        assertThat(inntektsmeldingTjeneste.hentInntektsmeldinger(ref, skjæringstidspunktet)).isEmpty();

        lagreInntektsmelding(skjæringstidspunktet.minusWeeks(3), behandling, ARBEIDSFORHOLD_ID, ARBEIDSFORHOLD_ID_EKSTERN, BigDecimal.TEN, arbeidsgiver, skjæringstidspunktet,
            null);

        // Assert
        assertThat(inntektsmeldingTjeneste.hentInntektsmeldinger(ref, skjæringstidspunktet)).hasSize(1);

    }

    @Test
    void skal_ikke_ta_med_eldre_inntektsmelding() {
        // Arrange
        var behandling = opprettBehandling();
        var skjæringstidspunktet = I_DAG.minusDays(1);
        var ref = lagReferanse(behandling);

        // Act
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID,
            DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL),
            ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);

        lagreInntektsmelding(skjæringstidspunktet.minusMonths(2), behandling, ARBEIDSFORHOLD_ID, ARBEIDSFORHOLD_ID_EKSTERN, BigDecimal.TEN, arbeidsgiver2, skjæringstidspunktet.minusMonths(2),
            NAV_NO);
        var skalAksepteres = YearMonth.from(skjæringstidspunktet.minusMonths(2)).atEndOfMonth().plusDays(1);
        lagreInntektsmelding(skalAksepteres, behandling, ARBEIDSFORHOLD_ID, ARBEIDSFORHOLD_ID_EKSTERN, BigDecimal.TEN, arbeidsgiver, skjæringstidspunktet,
            NAV_NO);

        // Assert
        assertThat(inntektsmeldingTjeneste.hentInntektsmeldinger(ref, skjæringstidspunktet)).hasSize(1);

    }

    private boolean erDisjonkteListerAvInntektsmeldinger(List<Inntektsmelding> imsA, List<Inntektsmelding> imsB) {
        return Collections.disjoint(
                imsA.stream().map(Inntektsmelding::getJournalpostId).toList(),
                imsB.stream().map(Inntektsmelding::getJournalpostId).toList());
    }

    private void lagreInntektsmelding(LocalDate mottattDato, Behandling behandling, InternArbeidsforholdRef arbeidsforholdIdIntern,
            EksternArbeidsforholdRef arbeidsforholdId) {
        lagreInntektsmelding(mottattDato, behandling, arbeidsforholdIdIntern, arbeidsforholdId, BigDecimal.TEN, arbeidsgiver);
    }

    private void lagreInntektsmelding(LocalDate mottattDato, Behandling behandling, InternArbeidsforholdRef arbeidsforholdIdIntern,
                                      EksternArbeidsforholdRef arbeidsforholdId, BigDecimal beløp, Arbeidsgiver arbeidsgiver) {
        lagreInntektsmelding(mottattDato, behandling, arbeidsforholdIdIntern, arbeidsforholdId, beløp, arbeidsgiver, I_DAG, NAV_NO);
    }

    private void lagreInntektsmelding(LocalDate mottattDato, Behandling behandling, InternArbeidsforholdRef arbeidsforholdIdIntern,
                                      EksternArbeidsforholdRef arbeidsforholdId, BigDecimal beløp, Arbeidsgiver arbeidsgiver, LocalDate startdato,
                                      String kildeSystem) {
        var journalPostId = new JournalpostId(journalpostIdInc.getAndIncrement());

        var inntektsmelding = InntektsmeldingBuilder.builder()
            .medStartDatoPermisjon(startdato)
            .medArbeidsgiver(arbeidsgiver)
            .medBeløp(beløp)
            .medNærRelasjon(false)
            .medArbeidsforholdId(arbeidsforholdId)
            .medArbeidsforholdId(arbeidsforholdIdIntern)
            .medInnsendingstidspunkt(LocalDateTime.of(mottattDato, LocalTime.MIN))
            .medJournalpostId(journalPostId);
        if (kildeSystem != null) {
            inntektsmelding.medKildesystem(kildeSystem);
        }

        inntektsmeldingTjeneste.lagreInntektsmelding(inntektsmelding,behandling );

    }

    private void opprettOppgittOpptjening(Behandling behandling) {
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.minusMonths(2), I_DAG.plusMonths(1));
        var oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);
    }

    private void opprettInntektArbeidYtelseAggregatForYrkesaktivitet(Behandling behandling, AktørId aktørId,
            DatoIntervallEntitet periode,
            InternArbeidsforholdRef arbeidsforhold,
            ArbeidType type, BigDecimal prosentsats) {

        var builder = InntektArbeidYtelseAggregatBuilder
                .oppdatere(Optional.empty(), VersjonType.REGISTER);

        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);

        leggTilYrekesaktivitet(aktørArbeidBuilder, arbeidsforhold, type, prosentsats, periode, periode);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        var aktørInntektBuilder = builder.getAktørInntektBuilder(aktørId);
        leggTilInntekt(aktørInntektBuilder, periode);
        builder.leggTilAktørInntekt(aktørInntektBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder leggTilYrekesaktivitet(
            InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder builder,
            InternArbeidsforholdRef ref, ArbeidType type, BigDecimal prosentsats,
            DatoIntervallEntitet periodeYA, DatoIntervallEntitet periodeAA) {
        var yrkesaktivitetBuilder = builder.getYrkesaktivitetBuilderForNøkkelAvType(
                new Opptjeningsnøkkel(ref, arbeidsgiver.getIdentifikator(), null),
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periodeAA, false);
        var permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder
                .medPeriode(periodeAA)
                .medProsentsats(prosentsats)
                .medBeskrivelse("Ser greit ut");
        var ansettelsesPeriode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder(periodeYA, true);

        var permisjon = permisjonBuilder
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

        return builder
                .leggTilYrkesaktivitet(yrkesaktivitetBuilder);
    }

    private InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder leggTilInntekt(InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder builder,
            DatoIntervallEntitet periodeInntekt) {
        var inntektBuilder = builder.getInntektBuilder(InntektsKilde.INNTEKT_BEREGNING,
                new Opptjeningsnøkkel(InternArbeidsforholdRef.nullRef(), arbeidsgiver.getIdentifikator(), null));

        var inntektspostBuilder = inntektBuilder.getInntektspostBuilder()
                .medInntektspostType(InntektspostType.LØNN)
                .medBeløp(LØNNSPOST)
                .medSkatteOgAvgiftsregelType(SkatteOgAvgiftsregelType.UDEFINERT)
                .medPeriode(periodeInntekt.getFomDato(), periodeInntekt.getTomDato());

        inntektBuilder
                .medArbeidsgiver(arbeidsgiver)
                .leggTilInntektspost(inntektspostBuilder);

        return builder
                .leggTilInntekt(inntektBuilder);
    }

    private Virksomhet lagVirksomhet() {
        return new Virksomhet.Builder()
                .medOrgnr(KUNSTIG_ORG)
                .medNavn("Virksomheten")
                .medRegistrert(I_DAG.minusYears(2L))
                .build();
    }

    private Virksomhet lagAndreVirksomhet() {
        return new Virksomhet.Builder()
                .medOrgnr("52")
                .medNavn("OrgA")
                .medRegistrert(I_DAG.minusYears(2L))
                .build();
    }

    private Behandling opprettBehandling() {
        return opprettBehandling(opprettFagsak());
    }

    private Behandling opprettBehandling(Fagsak fagsak) {
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private Fagsak opprettFagsak() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AKTØRID), RelasjonsRolleType.MORA,
            new Saksnummer("9999"));
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET).buildFor(behandling);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, lås);
        var fagsakRepository = repositoryProvider.getFagsakRepository();
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }

    private Behandling opprettRevurderingsbehandling(Behandling opprinneligBehandling) {
        var behandlingType = BehandlingType.REVURDERING;
        var revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
                .medOriginalBehandlingId(opprinneligBehandling.getId());
        var revurdering = Behandling.fraTidligereBehandling(opprinneligBehandling, behandlingType)
                .medBehandlingÅrsak(revurderingÅrsak).build();
        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering));
        return revurdering;
    }
}
