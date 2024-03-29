package no.nav.foreldrepenger.domene.arbeidsforhold;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.ArbeidsforholdHandlingType;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

class InntektsmeldingUtenArbeidsforholdTjenesteTest {
    private static final AktørId AKTØR_ID = new AktørId("9999999999999");
    private static final LocalDate STP = LocalDate.of(2022,1,1);

    @Test
    void skal_ikke_gi_utslag_ved_inntektsmelding_med_arbeidsforhold() {
        // Arrange
        var orgnr = "222222222";
        var ya = List.of(yrkesaktivitet(orgnr, dagerFørStp(90), dagerEtterStp(60), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD));
        var inntektsposter = lagInntektsposter(dagerFørStp(90), dagerEtterStp(60), 100);
        var inntekter = List.of(inntekt(orgnr, inntektsposter));
        var inntektsmeldinger = List.of(inntektsmelding(orgnr));

        // Act
        var resultat = utled(lagAggregat(ya, inntekter, inntektsmeldinger));

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_gi_utslag_hvis_det_mangler_arbeidsforhold_men_kreves_refusjon() {
        // Arrange
        var orgnr = "222222222";
        var inntektsmeldinger = List.of(inntektsmelding(orgnr, 5000));

        // Act
        var resultat = utled(lagAggregat(Collections.emptyList(), Collections.emptyList(), inntektsmeldinger));

        // Assert
        assertThat(resultat).hasSize(1);
        var arbeidsgivere = resultat.keySet();
        assertThat(arbeidsgivere).contains(Arbeidsgiver.virksomhet(orgnr));
    }

    @Test
    void skal_ikke_gi_utslag_hvis_det_kreves_refusjon_og_finnes_arbeidsforhold() {
        // Arrange
        var orgnr = "222222222";
        var ya = List.of(yrkesaktivitet(orgnr, dagerFørStp(90), dagerEtterStp(60), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD));
        var inntektsmeldinger = List.of(inntektsmelding(orgnr, 5000));

        // Act
        var resultat = utled(lagAggregat(ya, Collections.emptyList(), inntektsmeldinger));

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_ikke_gi_utslag_ved_inntektsmelding_uten_arbeid_uten_inntekt() {
        // Arrange
        var orgnrIM = "222222222";
        var orgnrArbeidInntekt = "333333333";
        var ya = List.of(
            yrkesaktivitet(orgnrArbeidInntekt, dagerFørStp(90), dagerEtterStp(60), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD));
        var inntektsposter = lagInntektsposter(dagerFørStp(90), dagerEtterStp(60), 100);
        var inntekter = List.of(inntekt(orgnrArbeidInntekt, inntektsposter));
        var inntektsmeldinger = List.of(inntektsmelding(orgnrIM));

        // Act
        var resultat = utled(lagAggregat(ya, inntekter, inntektsmeldinger));

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_gi_utslag_ved_inntektsmelding_uten_arbeid_med_inntekt() {
        // Arrange
        var orgnr = "222222222";
        List<YrkesaktivitetBuilder> ya = Collections.emptyList();
        var inntektsposter = lagInntektsposter(dagerFørStp(90), dagerEtterStp(60), 100);
        var inntekter = List.of(inntekt(orgnr, inntektsposter));
        var inntektsmeldinger = List.of(inntektsmelding(orgnr));

        // Act
        var resultat = utled(lagAggregat(ya, inntekter, inntektsmeldinger));

        // Assert
        assertThat(resultat).hasSize(1);
        var arbeidsgivere = resultat.keySet();
        assertThat(arbeidsgivere).contains(Arbeidsgiver.virksomhet(orgnr));
    }

    @Test
    void skal_ikke_gi_utslag_ved_inntektsmelding_uten_arbeid_med_gammel_inntekt() {
        // Arrange
        var orgnr = "222222222";
        List<YrkesaktivitetBuilder> ya = Collections.emptyList();
        var inntektsposter = lagInntektsposter(dagerFørStp(360), dagerFørStp(200), 100);
        var inntekter = List.of(inntekt(orgnr, inntektsposter));
        var inntektsmeldinger = List.of(inntektsmelding(orgnr));

        // Act
        var resultat = utled(lagAggregat(ya, inntekter, inntektsmeldinger));

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_gi_utslag_ved_oppgitt_fiske() {
        // Arrange
        var orgnr = "222222222";
        List<YrkesaktivitetBuilder> ya = Collections.emptyList();
        var inntektsposter = lagInntektsposter(dagerFørStp(360), dagerFørStp(200), 100);
        var inntekter = List.of(inntekt(orgnr, inntektsposter));
        var inntektsmeldinger = List.of(inntektsmelding(orgnr));

        // Act
        var resultat = utled(lagAggregat(ya, inntekter, inntektsmeldinger, lagFiske(), null));

        // Assert
        assertThat(resultat).hasSize(1);
        var arbeidsgivere = resultat.keySet();
        assertThat(arbeidsgivere).contains(Arbeidsgiver.virksomhet(orgnr));
    }

    @Test
    void skal_gi_utslag_ved_oppgitt_fiske_og_manuelt_opprettet_arbeidsforhold() {
        // Arrange
        var orgnr = "222222222";
        List<YrkesaktivitetBuilder> ya = Collections.emptyList();
        var inntektsposter = lagInntektsposter(dagerFørStp(360), dagerFørStp(200), 100);
        var inntekter = List.of(inntekt(orgnr, inntektsposter));
        var inntektsmeldinger = List.of(inntektsmelding(orgnr));
        var ref = inntektsmeldinger.get(0).getArbeidsforholdRef();
        var infoBuilder = ArbeidsforholdInformasjonBuilder.builder(empty());
        var manueltOpprettetArbeidsforhold = infoBuilder.getOverstyringBuilderFor(Arbeidsgiver.virksomhet(orgnr), ref)
            .medHandling(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING)
            .medBeskrivelse("BEskrivelse")
            .medAngittStillingsprosent(Stillingsprosent.HUNDRED)
            .leggTilOverstyrtPeriode(dagerFørStp(100), dagerEtterStp(100));
        infoBuilder.leggTil(manueltOpprettetArbeidsforhold);

        // Act
        var resultat = utled(lagAggregat(ya, inntekter, inntektsmeldinger, lagFiske(), infoBuilder.build()));

        // Assert
        assertThat(resultat).hasSize(1);
        var arbeidsgivere = resultat.keySet();
        assertThat(arbeidsgivere).contains(Arbeidsgiver.virksomhet(orgnr));
    }

    @Test
    void skal_ikke_gi_utslag_ved_motatt_inntektsmelding_på_frilansforhold() {
        // Arrange
        var frilansOrgnr = "222222222";
        var arbeidOrgnr = "333333333";
        var ya = Arrays.asList(yrkesaktivitet(frilansOrgnr, dagerFørStp(90), dagerEtterStp(60), ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER),
            yrkesaktivitet(arbeidOrgnr, dagerFørStp(90), dagerEtterStp(60), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD));
        var inntektsposter = lagInntektsposter(dagerFørStp(360), dagerFørStp(10), 100);
        var inntekter = Arrays.asList(inntekt(frilansOrgnr, inntektsposter), inntekt(arbeidOrgnr, inntektsposter));
        var inntektsmeldinger = Arrays.asList(inntektsmelding(frilansOrgnr), inntektsmelding(arbeidOrgnr));

        // Act
        var resultat = utled(lagAggregat(ya, inntekter, inntektsmeldinger));

        // Assert
        assertThat(resultat).isEmpty();
    }


    private Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> utled(InntektArbeidYtelseGrunnlag iay) {
        return InntektsmeldingUtenArbeidsforholdTjeneste.utledManglendeArbeidsforhold(iay.getInntektsmeldinger().map(InntektsmeldingAggregat::getAlleInntektsmeldinger).orElse(Collections.emptyList()), iay, AKTØR_ID, STP);
    }

    private OppgittOpptjeningBuilder lagFiske() {
        return OppgittOpptjeningBuilder.ny().leggTilEgenNæring(List.of(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(dagerFørStp(100)))
            .medVirksomhetType(VirksomhetType.FISKE)
            .medVirksomhet("999999999")));
    }

    private Map<YearMonth, Integer> lagInntektsposter(LocalDate fom, LocalDate tom, int beløp) {
        var slutt = YearMonth.of(tom.getYear(), tom.getMonth());
        var current = YearMonth.of(fom.getYear(), fom.getMonth());
        Map<YearMonth, Integer> inntektsposter = new HashMap<>();
        while (!current.isAfter(slutt)) {
            inntektsposter.put(current, beløp);
            current = current.plusMonths(1);
        }
        return inntektsposter;
    }

    private LocalDate dagerFørStp(int før) {
        return STP.minusDays(før);
    }

    private LocalDate dagerEtterStp(int etter) {
        return STP.plusDays(etter);
    }

    private InntektArbeidYtelseGrunnlag lagAggregat(List<YrkesaktivitetBuilder> yrkesaktiviteter,
                                                    List<InntektBuilder> inntekter,
                                                    List<Inntektsmelding> inntektsmeldinger) {
        return lagAggregat(yrkesaktiviteter, inntekter, inntektsmeldinger, null, null);
    }

    private InntektArbeidYtelseGrunnlag lagAggregat(List<YrkesaktivitetBuilder> yrkesaktiviteter,
                                                    List<InntektBuilder> inntekter,
                                                    List<Inntektsmelding> inntektsmeldinger,
                                                    OppgittOpptjeningBuilder oppgittOpptjeningBuilder,
                                                    ArbeidsforholdInformasjon arbeidsforholdInformasjon) {
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);

        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØR_ID);
        yrkesaktiviteter.forEach(aktørArbeidBuilder::leggTilYrkesaktivitet);
        builder.leggTilAktørArbeid(aktørArbeidBuilder);

        var aktørInntektBuilder = builder.getAktørInntektBuilder(AKTØR_ID);
        inntekter.forEach(aktørInntektBuilder::leggTilInntekt);
        builder.leggTilAktørInntekt(aktørInntektBuilder);


        var iayGrunnlagBuilder = InntektArbeidYtelseGrunnlagBuilder.nytt().medData(builder).medInntektsmeldinger(inntektsmeldinger);
        if (oppgittOpptjeningBuilder != null) {
            iayGrunnlagBuilder.medOppgittOpptjening(oppgittOpptjeningBuilder);
        }
        if (arbeidsforholdInformasjon != null) {
            iayGrunnlagBuilder.medInformasjon(arbeidsforholdInformasjon);
        }
        return iayGrunnlagBuilder.build();
    }


    private YrkesaktivitetBuilder yrkesaktivitet(String orgnr, LocalDate fom, LocalDate tom, ArbeidType arbeidType) {
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        return YrkesaktivitetBuilder.oppdatere(empty())
            .medArbeidType(arbeidType)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medProsentsats(BigDecimal.ZERO)
                .medPeriode(periode)
                .medSisteLønnsendringsdato(periode.getFomDato()))
            .leggTilAktivitetsAvtale(AktivitetsAvtaleBuilder.ny()
                .medProsentsats(BigDecimal.valueOf(100))
                .medPeriode(periode));
    }

    private InntektBuilder inntekt(String orgnr, Map<YearMonth, Integer> månedBeløpMap) {
        var inntektBuilder = InntektBuilder.oppdatere(empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_BEREGNING)
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr));
        månedBeløpMap.entrySet().stream().map(entry -> InntektspostBuilder.ny()
                .medBeløp(BigDecimal.valueOf(entry.getValue()))
                .medPeriode(entry.getKey().atDay(1), entry.getKey().atDay(entry.getKey().lengthOfMonth())))
            .toList()
            .forEach(inntektBuilder::leggTilInntektspost);
        return inntektBuilder;
    }

    private Inntektsmelding inntektsmelding(String orgnr) {
        return inntektsmelding(orgnr, null);
    }

    private Inntektsmelding inntektsmelding(String orgnr, Integer refusjonPrMnd) {
        var builder = InntektsmeldingBuilder.builder()
            .medArbeidsgiver(Arbeidsgiver.virksomhet(orgnr))
            .medBeløp(BigDecimal.valueOf(10000))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef());
        if (refusjonPrMnd != null) {
            builder.medRefusjon(BigDecimal.valueOf(refusjonPrMnd));
        }
        return builder.build();
    }


}
