package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.foreldrepenger.domene.arbeidsforhold.ArbeidsforholdWrapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.VurderArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.ArbeidsforholdAdministrasjonTjeneste.UtledArbeidsforholdParametere;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.iay.modell.Permisjon;
import no.nav.foreldrepenger.domene.iay.modell.PermisjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class ArbeidsforholdAdministrasjonTjenesteTest {

    private static final String ORG1 = KUNSTIG_ORG;
    private static final String ORG2 = "52";

    private final LocalDate I_DAG = LocalDate.now();
    private final LocalDate ARBEIDSFORHOLD_FRA = I_DAG.minusMonths(3);
    private final LocalDate ARBEIDSFORHOLD_TIL = I_DAG.plusMonths(2);
    private final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef();
    private final EksternArbeidsforholdRef EKSTERN_ARBEIDSFORHOLD_ID = EksternArbeidsforholdRef.ref("123");
    private final AktørId AKTØRID = AktørId.dummy();

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
    private ArbeidsforholdAdministrasjonTjeneste arbeidsforholdTjeneste;

    private Arbeidsgiver arbeidsgiver;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);

        Virksomhet virksomhet1 = lagVirksomhet();
        Virksomhet virksomhet2 = lagAndreVirksomheten();

        arbeidsgiver = Arbeidsgiver.virksomhet(virksomhet1.getOrgnr());

        VurderArbeidsforholdTjeneste vurderArbeidsforholdTjeneste = mock(VurderArbeidsforholdTjeneste.class);

        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(virksomhet2.getOrgnr());
        Set<InternArbeidsforholdRef> arbeidsforholdRefSet = new HashSet<>();
        arbeidsforholdRefSet.add(ARBEIDSFORHOLD_ID);
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> arbeidsgiverSetMap = new HashMap<>();
        arbeidsgiverSetMap.put(arbeidsgiver, arbeidsforholdRefSet);
        when(vurderArbeidsforholdTjeneste.vurder(any(), any(), any(), Mockito.anyBoolean())).thenReturn(arbeidsgiverSetMap);

        arbeidsforholdTjeneste = new ArbeidsforholdAdministrasjonTjeneste(vurderArbeidsforholdTjeneste, inntektsmeldingTjeneste, iayTjeneste);
    }

    @Test
    public void skal_utlede_arbeidsforholdwrapper() {
        // Arrange
        LocalDate mottattDato = I_DAG.minusDays(2);
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL), ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);
        lagreInntektsmelding(mottattDato, behandling, ARBEIDSFORHOLD_ID, EKSTERN_ARBEIDSFORHOLD_ID);

        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();

        assertThat(arbeidsforhold.getMottattDatoInntektsmelding()).isEqualTo(mottattDato);
        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(false);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(ARBEIDSFORHOLD_FRA);
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(ARBEIDSFORHOLD_TIL);
    }


    @Test
    public void skal_utlede_tomm_arbeidsforholdwrapper_kun_im_etter_overstyring_ikke_bruk_TFP_2107() {
        // Arrange
        LocalDate mottattDato = I_DAG.minusDays(2);
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        lagreInntektsmelding(mottattDato, behandling, ARBEIDSFORHOLD_ID, EKSTERN_ARBEIDSFORHOLD_ID);

        // Act
        ArbeidsforholdInformasjonBuilder informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandling.getId());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ARBEIDSFORHOLD_ID);
        overstyringBuilder.medHandling(ArbeidsforholdHandlingType.IKKE_BRUK);
        informasjonBuilder.leggTil(overstyringBuilder);
        arbeidsforholdTjeneste.lagre(behandling.getId(), behandling.getAktørId(), informasjonBuilder);

        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(0);
    }

    @Test
    public void skal_utlede_arbeidsforholdwrapper_im_uten_ya_ans() {
        // Arrange
        LocalDate mottattDato = I_DAG.minusDays(2);
        Behandling behandling = opprettBehandling();
        opprettInntektArbeidYtelseAggregatForYrkesaktivitetUtenAns(AKTØRID, ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, behandling);
        lagreInntektsmelding(mottattDato, behandling, ARBEIDSFORHOLD_ID, EKSTERN_ARBEIDSFORHOLD_ID);

        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();

        assertThat(arbeidsforhold.getMottattDatoInntektsmelding()).isEqualTo(mottattDato);
        assertThat(arbeidsforhold.getKilde()).isEqualTo(ArbeidsforholdKilde.INNTEKTSMELDING);
        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(false);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(LocalDate.now()); // null-verdi
    }

    @Test
    public void skal_utlede_to_arbeidsforholdwrapper() {
        // Arrange
        LocalDate mottattDato = I_DAG.minusDays(2);
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL), ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);
        lagreInntektsmelding(mottattDato, behandling, null, null);

        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();

        assertThat(arbeidsforhold.getMottattDatoInntektsmelding()).isEqualTo(mottattDato);
        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(false);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(ARBEIDSFORHOLD_FRA);
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(ARBEIDSFORHOLD_TIL);
    }

    private Set<ArbeidsforholdWrapper> hentArbeidsforholdFerdigUtledet(Behandling behandling) {
        BehandlingReferanse ref = lagRef(behandling);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        return arbeidsforholdTjeneste.hentArbeidsforholdFerdigUtledet(ref, iayGrunnlag, null, new UtledArbeidsforholdParametere(true));
    }

    @Test
    public void skal_utlede_to_arbeidsforholdwrapper_max_prosent() {
        // Arrange
        LocalDate mottattDato = I_DAG.minusDays(2);

        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForMultiYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ONE, behandling);
        lagreInntektsmelding(mottattDato, behandling, null, null);

        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();

        assertThat(arbeidsforhold.getMottattDatoInntektsmelding()).isEqualTo(mottattDato);
        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(false);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(ARBEIDSFORHOLD_FRA);
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(ARBEIDSFORHOLD_TIL);
        assertThat(arbeidsforhold.getStillingsprosent()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    public void skal_utlede_to_arbeidsforholdwrapper_uten_im() {
        // Arrange
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForMultiYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ONE, behandling);

        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();

        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(false);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(ARBEIDSFORHOLD_FRA);
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(ARBEIDSFORHOLD_TIL);
        assertThat(arbeidsforhold.getKilde()).isEqualTo(ArbeidsforholdKilde.AAREGISTERET);
        assertThat(arbeidsforhold.getStillingsprosent()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    public void skal_utlede_arbeidsforholdwrapper_etter_overstyring() {
        // Arrange
        LocalDate mottattDato = I_DAG.minusDays(2);
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL), ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);
        lagreInntektsmelding(mottattDato, behandling, ARBEIDSFORHOLD_ID, EKSTERN_ARBEIDSFORHOLD_ID);

        // Act
        ArbeidsforholdInformasjonBuilder informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandling.getId());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ARBEIDSFORHOLD_ID);
        overstyringBuilder.medHandling(ArbeidsforholdHandlingType.BRUK);
        informasjonBuilder.leggTil(overstyringBuilder);
        arbeidsforholdTjeneste.lagre(behandling.getId(), behandling.getAktørId(), informasjonBuilder);
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();
        assertThat(arbeidsforhold.getMottattDatoInntektsmelding()).isEqualTo(mottattDato);
        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(false);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(ARBEIDSFORHOLD_FRA);
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(ARBEIDSFORHOLD_TIL);
    }

    @Test
    public void skal_utlede_arbeidsforholdwrapper_etter_overstyring_uten_arbeidsforhold() {
        // Arrange
        LocalDate mottattDato = I_DAG.minusDays(2);
        Behandling behandling = opprettBehandling();

        // Act
        ArbeidsforholdInformasjonBuilder informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandling.getId());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ARBEIDSFORHOLD_ID);
        overstyringBuilder.medHandling(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER);
        overstyringBuilder.medAngittStillingsprosent(Stillingsprosent.HUNDRED);
        overstyringBuilder.leggTilOverstyrtPeriode(mottattDato.minusYears(1L), Tid.TIDENES_ENDE);
        informasjonBuilder.leggTil(overstyringBuilder);
        arbeidsforholdTjeneste.lagre(behandling.getId(), behandling.getAktørId(), informasjonBuilder);
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();
        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getLagtTilAvSaksbehandler()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(true);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(mottattDato.minusYears(1L));
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(Tid.TIDENES_ENDE);
    }

    @Test
    public void test_hentArbeidsforholdFerdigUtledet_med_aksjonspunkt() {
        // Arrange
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForMultiYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.ONE, behandling);
        opprettAksjonspunkt(behandling, LocalDateTime.now());

        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();

        assertThat(arbeidsforhold.getBrukArbeidsforholdet()).isEqualTo(true);
        assertThat(arbeidsforhold.getFortsettBehandlingUtenInntektsmelding()).isEqualTo(false);
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(ARBEIDSFORHOLD_FRA);
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(ARBEIDSFORHOLD_TIL);
        assertThat(arbeidsforhold.getKilde()).isEqualTo(ArbeidsforholdKilde.AAREGISTERET);
        assertThat(arbeidsforhold.getStillingsprosent()).isEqualTo(BigDecimal.ONE);
    }

    @Test
    public void skal_bruke_datoer_for_yrkesaktivitet_som_kommer_etter_skjæringstidspunktet_uten_inntektsmelding() {
        // Arrange
        Behandling behandling = opprettBehandling();
        DatoIntervallEntitet periodeFør = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.minusYears(1), I_DAG.minusDays(1));
        DatoIntervallEntitet periodeEtter = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.plusDays(1), I_DAG.plusYears(1));
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØRID);
        leggTilYrkesaktivitet(aktørArbeidBuilder, InternArbeidsforholdRef.nyRef(), ArbeidType.FORENKLET_OPPGJØRSORDNING, BigDecimal.valueOf(100), periodeFør, periodeFør);
        leggTilYrkesaktivitet(aktørArbeidBuilder, InternArbeidsforholdRef.nyRef(), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.valueOf(100), periodeEtter, periodeEtter);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);
        // Assert
        assertThat(wrapperList).hasSize(1);
        assertThat(wrapperList.iterator().next().getFomDato()).isEqualTo(I_DAG.plusDays(1));
        assertThat(wrapperList.iterator().next().getTomDato()).isEqualTo(I_DAG.plusYears(1));
    }

    @Test
    public void skal_bruke_datoer_for_yrkesaktivitet_som_overlapper_skjæringstidspunktet_uten_inntektsmelding() {
        // Arrange
        Behandling behandling = opprettBehandling();
        DatoIntervallEntitet periodeFør = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.minusYears(1), I_DAG);
        DatoIntervallEntitet periodeEtter = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.plusDays(1), I_DAG.plusYears(1));
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder .oppdatere(Optional.empty(), VersjonType.REGISTER);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØRID);

        leggTilYrkesaktivitet(aktørArbeidBuilder, InternArbeidsforholdRef.nyRef(), ArbeidType.FORENKLET_OPPGJØRSORDNING, BigDecimal.valueOf(100), periodeFør, periodeFør);
        leggTilYrkesaktivitet(aktørArbeidBuilder, InternArbeidsforholdRef.nyRef(), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.valueOf(100), periodeEtter, periodeEtter);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        assertThat(wrapperList.iterator().next().getFomDato()).isEqualTo(I_DAG.minusYears(1));
        assertThat(wrapperList.iterator().next().getTomDato()).isEqualTo(I_DAG);
    }

    @Test
    public void skal_bruke_datoer_for_yrkesaktivitet_som_kommer_etter_skjæringstidspunktet_med_inntektsmelding() {
        // Arrange
        Behandling behandling = opprettBehandling();
        lagreInntektsmelding(I_DAG.minusDays(3), behandling, ARBEIDSFORHOLD_ID, EKSTERN_ARBEIDSFORHOLD_ID);
        DatoIntervallEntitet periodeFør = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.minusYears(1), I_DAG.minusDays(1));
        DatoIntervallEntitet periodeEtter = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.plusDays(1), I_DAG.plusYears(1));
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØRID);
        leggTilYrkesaktivitet(aktørArbeidBuilder, InternArbeidsforholdRef.nyRef(), ArbeidType.FORENKLET_OPPGJØRSORDNING, BigDecimal.valueOf(100), periodeFør, periodeFør);
        leggTilYrkesaktivitet(aktørArbeidBuilder, InternArbeidsforholdRef.nyRef(), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.valueOf(100), periodeEtter, periodeEtter);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);
        // Assert
        assertThat(wrapperList).hasSize(1);
        assertThat(wrapperList.iterator().next().getFomDato()).isEqualTo(I_DAG.plusDays(1));
        assertThat(wrapperList.iterator().next().getTomDato()).isEqualTo(I_DAG.plusYears(1));
    }

    @Test
    public void skal_bruke_datoer_for_yrkesaktivitet_som_overlapper_skjæringstidspunktet_med_inntektsmelding() {
        // Arrange
        Behandling behandling = opprettBehandling();
        lagreInntektsmelding(I_DAG.minusDays(3), behandling, ARBEIDSFORHOLD_ID, EKSTERN_ARBEIDSFORHOLD_ID);
        DatoIntervallEntitet periodeFør = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.minusYears(1), I_DAG);
        DatoIntervallEntitet periodeEtter = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.plusDays(1), I_DAG.plusYears(1));
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØRID);
        leggTilYrkesaktivitet(aktørArbeidBuilder, InternArbeidsforholdRef.nyRef(), ArbeidType.FORENKLET_OPPGJØRSORDNING, BigDecimal.valueOf(100), periodeFør, periodeFør);
        leggTilYrkesaktivitet(aktørArbeidBuilder, InternArbeidsforholdRef.nyRef(), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.valueOf(100), periodeEtter, periodeEtter);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
        // Act
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);
        // Assert
        assertThat(wrapperList).hasSize(1);
        assertThat(wrapperList.iterator().next().getFomDato()).isEqualTo(I_DAG.minusYears(1));
        assertThat(wrapperList.iterator().next().getTomDato()).isEqualTo(I_DAG);
    }

    @Test
    public void skal_utlede_dato_fra_ansettelsesperiode_når_overstyring_uten_overstyrte_perioder_og_im(){
        // Arrange
        LocalDate mottattDato = I_DAG.minusDays(2);
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL), ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);
        lagreInntektsmelding(mottattDato, behandling, ARBEIDSFORHOLD_ID, EKSTERN_ARBEIDSFORHOLD_ID);

        // Act
        ArbeidsforholdInformasjonBuilder informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandling.getId());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ARBEIDSFORHOLD_ID);
        overstyringBuilder.medHandling(ArbeidsforholdHandlingType.BRUK);
        BekreftetPermisjon bekreftetPermisjon = new BekreftetPermisjon(I_DAG.minusWeeks(5), I_DAG.minusWeeks(1), BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON);
        overstyringBuilder.medBekreftetPermisjon(bekreftetPermisjon);
        informasjonBuilder.leggTil(overstyringBuilder);
        arbeidsforholdTjeneste.lagre(behandling.getId(), behandling.getAktørId(), informasjonBuilder);
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(ARBEIDSFORHOLD_FRA);
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(ARBEIDSFORHOLD_TIL);
    }

    @Test
    public void skal_utlede_dato_fra_overstyrt_periode_når_overstyring_med_overstyrte_perioder_og_im(){
        // Arrange
        LocalDate overstyrtPeriodeFom = ARBEIDSFORHOLD_FRA.minusDays(2);
        LocalDate overstyrtPeriodeTom = Tid.TIDENES_ENDE;
        LocalDate mottattDato = I_DAG.minusDays(2);
        Behandling behandling = opprettBehandling();
        opprettOppgittOpptjening(behandling);
        opprettInntektArbeidYtelseAggregatForYrkesaktivitet(behandling, AKTØRID, DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL), ARBEIDSFORHOLD_ID, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN);
        lagreInntektsmelding(mottattDato, behandling, ARBEIDSFORHOLD_ID, EKSTERN_ARBEIDSFORHOLD_ID);

        // Act
        ArbeidsforholdInformasjonBuilder informasjonBuilder = arbeidsforholdTjeneste.opprettBuilderFor(behandling.getId());
        ArbeidsforholdOverstyringBuilder overstyringBuilder = informasjonBuilder.getOverstyringBuilderFor(arbeidsgiver, ARBEIDSFORHOLD_ID);
        overstyringBuilder.medHandling(ArbeidsforholdHandlingType.BRUK);
        BekreftetPermisjon bekreftetPermisjon = new BekreftetPermisjon(I_DAG.minusWeeks(5), I_DAG.minusWeeks(1), BekreftetPermisjonStatus.IKKE_BRUK_PERMISJON);
        overstyringBuilder.medBekreftetPermisjon(bekreftetPermisjon);
        overstyringBuilder.leggTilOverstyrtPeriode(overstyrtPeriodeFom, overstyrtPeriodeTom);
        informasjonBuilder.leggTil(overstyringBuilder);
        arbeidsforholdTjeneste.lagre(behandling.getId(), behandling.getAktørId(), informasjonBuilder);
        Set<ArbeidsforholdWrapper> wrapperList = hentArbeidsforholdFerdigUtledet(behandling);

        // Assert
        assertThat(wrapperList).hasSize(1);
        ArbeidsforholdWrapper arbeidsforhold = wrapperList.iterator().next();
        assertThat(arbeidsforhold.getFomDato()).isEqualTo(overstyrtPeriodeFom);
        assertThat(arbeidsforhold.getTomDato()).isEqualTo(overstyrtPeriodeTom);
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(I_DAG).build();
        return BehandlingReferanse.fra(behandling, skjæringstidspunkt);
    }

    private void lagreInntektsmelding(LocalDate mottattDato, Behandling behandling, InternArbeidsforholdRef arbeidsforholdId, EksternArbeidsforholdRef eksternArbeidsforholdRef) {
        JournalpostId journalPostId = new JournalpostId("123");
        var inntektsmelding = InntektsmeldingBuilder.builder()
        .medStartDatoPermisjon(I_DAG)
        .medArbeidsgiver(arbeidsgiver)
        .medBeløp(BigDecimal.TEN)
        .medNærRelasjon(false)
        .medArbeidsforholdId(arbeidsforholdId)
        .medArbeidsforholdId(eksternArbeidsforholdRef)
        .medMottattDato(mottattDato)
        .medInnsendingstidspunkt(LocalDateTime.now()).medJournalpostId(journalPostId);

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

        leggTilYrkesaktivitet(aktørArbeidBuilder, arbeidsforhold, type, prosentsats, periode, periode);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private void opprettInntektArbeidYtelseAggregatForYrkesaktivitetUtenAns(AktørId aktørId, InternArbeidsforholdRef arbeidsforhold,
                                                                            ArbeidType type, Behandling behandling) {

        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);

        leggTilYrkesaktivitetUtenAnsperiode(aktørArbeidBuilder, arbeidsforhold, type);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }


    private void opprettInntektArbeidYtelseAggregatForMultiYrkesaktivitet(AktørId aktørId, InternArbeidsforholdRef arbeidsforhold,
                                                                          ArbeidType type, BigDecimal prosentsats,
                                                                          Behandling behandling) {
        DatoIntervallEntitet periodeFørst = DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA.minusMonths(3), ARBEIDSFORHOLD_FRA.minusMonths(1));
        DatoIntervallEntitet periodeFørstAA = DatoIntervallEntitet.fraOgMedTilOgMed(I_DAG.minusMonths(6), Tid.TIDENES_ENDE);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, ARBEIDSFORHOLD_TIL);
        DatoIntervallEntitet periodeAA = DatoIntervallEntitet.fraOgMedTilOgMed(ARBEIDSFORHOLD_FRA, Tid.TIDENES_ENDE);

        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.REGISTER);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);

        leggTilYrkesaktivitet(aktørArbeidBuilder, arbeidsforhold, type, BigDecimal.TEN, periodeFørst, periodeFørstAA);
        leggTilYrkesaktivitet(aktørArbeidBuilder, arbeidsforhold, type, prosentsats, periode, periodeAA);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);

        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder leggTilYrkesaktivitet(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder builder,
                                                                                         InternArbeidsforholdRef ref, ArbeidType type, BigDecimal prosentsats,
                                                                                         DatoIntervallEntitet periodeYA, DatoIntervallEntitet periodeAA) {
        YrkesaktivitetBuilder yrkesaktivitetBuilder = builder.getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(ref, arbeidsgiver.getIdentifikator(), null),
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

        return builder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
    }

    private InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder leggTilYrkesaktivitetUtenAnsperiode(InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder builder,
                                                                                                       InternArbeidsforholdRef arbeidsforhold, ArbeidType type) {
        YrkesaktivitetBuilder yrkesaktivitetBuilder = builder.getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(arbeidsforhold, arbeidsgiver),
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        yrkesaktivitetBuilder
            .medArbeidType(type)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(ARBEIDSFORHOLD_ID);

        return builder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
    }

    private Virksomhet lagVirksomhet() {
        return new Virksomhet.Builder()
        .medOrgnr(ORG1)
        .medNavn("Virksomheten")
        .medRegistrert(I_DAG.minusYears(2L))
        .medOppstart(I_DAG.minusYears(1L))
        .build();
    }

    private Virksomhet lagAndreVirksomheten() {
        return new Virksomhet.Builder()
        .medOrgnr(ORG2)
        .medNavn("OrgA")
        .medRegistrert(I_DAG.minusYears(2L))
        .medOppstart(I_DAG.minusYears(1L))
        .build();
    }

    private Behandling opprettBehandling() {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AKTØRID));
        fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    private Aksjonspunkt opprettAksjonspunkt(Behandling behandling,
                                             LocalDateTime frist) {

        Aksjonspunkt aksjonspunkt = AksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);
        AksjonspunktTestSupport.setFrist(aksjonspunkt, frist, Venteårsak.UDEFINERT);
        return aksjonspunkt;
    }
}
