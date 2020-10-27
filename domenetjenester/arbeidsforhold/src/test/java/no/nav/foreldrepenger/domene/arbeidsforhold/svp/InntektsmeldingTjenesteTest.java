package no.nav.foreldrepenger.domene.arbeidsforhold.svp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.abakus.ArbeidsforholdTjenesteMock;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.InntektsmeldingRegisterTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
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
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class InntektsmeldingTjenesteTest extends EntityManagerAwareTest {

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final EksternArbeidsforholdRef ARBEIDSFORHOLD_ID_EKSTERN = EksternArbeidsforholdRef.ref("1");
    private static final AktørId AKTØRID = AktørId.dummy();
    private static final LocalDate I_DAG = LocalDate.now();
    private static final LocalDate ARBEIDSFORHOLD_FRA = I_DAG.minusMonths(3);
    private static final LocalDate ARBEIDSFORHOLD_TIL = I_DAG.plusMonths(2);
    private static final BigDecimal LØNNSPOST = BigDecimal.TEN;


    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);

    private Arbeidsgiver arbeidsgiver;

    private final Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(I_DAG).build();
    private final AtomicLong journalpostIdInc = new AtomicLong(123);
    private final ArbeidsforholdTjenesteMock arbeidsforholdTjenesteMock = new ArbeidsforholdTjenesteMock(false);

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);

        var virksomhet1 = lagVirksomhet();
        var virksomhet2 = lagAndreVirksomhet();

        this.arbeidsgiver = Arbeidsgiver.virksomhet(virksomhet1.getOrgnr());

        var virksomhetTjeneste = mock(VirksomhetTjeneste.class);
        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(virksomhet1);
        var vurderArbeidsforholdTjeneste = mock(VurderArbeidsforholdTjeneste.class);
        var arbeidsgiver2 = Arbeidsgiver.virksomhet(virksomhet2.getOrgnr());
        Set<InternArbeidsforholdRef> arbeidsforholdRefSet = new HashSet<>();
        arbeidsforholdRefSet.add(ARBEIDSFORHOLD_ID);
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> arbeidsgiverSetMap = new HashMap<>();
        arbeidsgiverSetMap.put(arbeidsgiver2, arbeidsforholdRefSet);
        when(vurderArbeidsforholdTjeneste.vurder(any(), any(), any(), Mockito.anyBoolean())).thenReturn(arbeidsgiverSetMap);

    }

    private BehandlingReferanse lagReferanse(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, skjæringstidspunkt);
    }

    @Test
    public void skal_ikke_ta_med_arbeidsforhold_det_ikke_er_søkt_for_når_manglende_im_utledes_for_svp() throws Exception {
        // Arrange
        var arbId1Intern = ARBEIDSFORHOLD_ID;
        var arbId1 = ARBEIDSFORHOLD_ID_EKSTERN;

        InternArbeidsforholdRef ARBEIDSFORHOLD_ID_2 = InternArbeidsforholdRef.ref("a6ea6724-868f-11e9-bc42-526af7764f64");

        var arbId2Intern = ARBEIDSFORHOLD_ID_2;

        var virksomhet1 = lagVirksomhet();
        var virksomhet2 = lagAndreVirksomhet();

        this.arbeidsgiver = Arbeidsgiver.virksomhet(virksomhet1.getOrgnr());

        var virksomhetTjeneste = mock(VirksomhetTjeneste.class);
        when(virksomhetTjeneste.hentOrganisasjon(any())).thenReturn(virksomhet1);

        var vurderArbeidsforholdTjeneste = mock(VurderArbeidsforholdTjeneste.class);
        var arbeidsgiver2 = Arbeidsgiver.virksomhet(virksomhet2.getOrgnr());
        Set<InternArbeidsforholdRef> arbeidsforholdRefSet = new HashSet<>();
        arbeidsforholdRefSet.add(ARBEIDSFORHOLD_ID);
        Set<InternArbeidsforholdRef> arbeidsforholdRefSet2 = new HashSet<>();
        arbeidsforholdRefSet2.add(ARBEIDSFORHOLD_ID_2);
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> arbeidsgiverSetMap = new HashMap<>();
        arbeidsgiverSetMap.put(arbeidsgiver2, arbeidsforholdRefSet);
        arbeidsgiverSetMap.put(arbeidsgiver, arbeidsforholdRefSet2);
        when(vurderArbeidsforholdTjeneste.vurder(any(), any(), any(), Mockito.anyBoolean())).thenReturn(arbeidsgiverSetMap);

        final Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL),
            arbId1Intern, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL),
            arbId2Intern, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);

        BehandlingReferanse behandlingReferanse = lagReferanse(behandling);

        SvangerskapspengerRepository svangerskapspengerRepositoryMock = mock(SvangerskapspengerRepository.class);
        SvpGrunnlagEntitet svpGrunnlag = byggSvpGrunnlag(behandling, virksomhet1.getOrgnr());
        when(svangerskapspengerRepositoryMock.hentGrunnlag(behandling.getId())).thenReturn(Optional.ofNullable(svpGrunnlag));

        var svpengerFilter = new InntektsmeldingFilterYtelseImpl(svangerskapspengerRepositoryMock);

        var inntektsmeldingArkivTjenesteSvp = new InntektsmeldingRegisterTjeneste(iayTjeneste,
            inntektsmeldingTjeneste, arbeidsforholdTjenesteMock.getMock(), new UnitTestLookupInstanceImpl<>(svpengerFilter));
        // Act+Assert
        assertThat(inntektsmeldingArkivTjenesteSvp.utledManglendeInntektsmeldingerFraAAreg(behandlingReferanse, false)).isNotEmpty();

        lagreInntektsmelding(I_DAG.minusDays(2), behandling, arbId1Intern, arbId1);

        // Act+Assert
        assertThat(inntektsmeldingArkivTjenesteSvp.utledManglendeInntektsmeldingerFraAAreg(behandlingReferanse, false)).isEmpty();
    }

    private SvpGrunnlagEntitet byggSvpGrunnlag(Behandling behandling, String arbeidsgiverOrgnr) {
        SvpTilretteleggingEntitet tilrettelegging = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(LocalDate.now())
            .medIngenTilrettelegging(LocalDate.now())
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(arbeidsgiverOrgnr))
            .medMottattTidspunkt(LocalDateTime.now())
            .medKopiertFraTidligereBehandling(false)
            .build();
        SvpGrunnlagEntitet svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medBehandlingId(behandling.getId())
            .medOpprinneligeTilrettelegginger(List.of(tilrettelegging))
            .build();
        return svpGrunnlag;
    }

    private void lagreInntektsmelding(LocalDate mottattDato, Behandling behandling, InternArbeidsforholdRef arbeidsforholdIdIntern, EksternArbeidsforholdRef arbeidsforholdId) {
        lagreInntektsmelding(mottattDato, behandling, arbeidsforholdIdIntern, arbeidsforholdId, BigDecimal.TEN);
    }

    private void lagreInntektsmelding(LocalDate mottattDato, Behandling behandling, InternArbeidsforholdRef arbeidsforholdIdIntern, EksternArbeidsforholdRef arbeidsforholdId, BigDecimal beløp) {
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
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AKTØRID), RelasjonsRolleType.MORA, new Saksnummer("123"));
        fagsakRepository.opprettNy(fagsak);
        return fagsak;
    }
}
