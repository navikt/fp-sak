package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class EndringIArbeidsforholdIdTest {


    private static final Arbeidsgiver ARBEIDSGIVER = Arbeidsgiver.virksomhet("987654321");
    private Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result;

    private InternArbeidsforholdRef refA = InternArbeidsforholdRef.namedRef("A");
    private InternArbeidsforholdRef refB = InternArbeidsforholdRef.namedRef("B");
    private InternArbeidsforholdRef refC = InternArbeidsforholdRef.namedRef("C");

    @Before
    public void setup() {
        result = new HashMap<>();
    }

    @Test
    public void ett_arbeidsforhold_har_ref_A_får_inntektsmelding_med_samme_ref() {
        // Arrange
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> yrkesaktiviteterPerArbeidsgiver = Map.of(ARBEIDSGIVER, Set.of(refA));
        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(yrkesaktiviteterPerArbeidsgiver);
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> eksisterendeIM = Map.of(ARBEIDSGIVER, Set.of(refA));
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmeldingMap = Map.of(ARBEIDSGIVER, Set.of(refA));
        Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmelding = nyInntektsmeldingMap.entrySet().iterator().next();

        // Act
        EndringIArbeidsforholdId.vurderMedÅrsak(result, nyInntektsmelding, eksisterendeIM, grunnlag, yrkesaktiviteterPerArbeidsgiver);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    public void ett_arbeidsforhold_har_ref_A_får_inntektsmelding_med_ref_B() {
        // Arrange
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> yrkesaktiviteterPerArbeidsgiver = Map.of(ARBEIDSGIVER, Set.of(refA));
        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(yrkesaktiviteterPerArbeidsgiver);
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> eksisterendeIM = Map.of(ARBEIDSGIVER, Set.of(refA));
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmeldingMap = Map.of(ARBEIDSGIVER,
            new HashSet<>(Set.of(refA, refB)));
        Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmelding = nyInntektsmeldingMap.entrySet().iterator().next();

        // Act
        EndringIArbeidsforholdId.vurderMedÅrsak(result, nyInntektsmelding, eksisterendeIM, grunnlag, yrkesaktiviteterPerArbeidsgiver);

        // Assert
        assertMap(refB);

    }

    @Test
    public void ett_arbeidsforhold_har_ref_A_får_inntektsmelding_uten_ref() {
        // Arrange
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> yrkesaktiviteterPerArbeidsgiver = Map.of(ARBEIDSGIVER, Set.of(refA));
        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(yrkesaktiviteterPerArbeidsgiver);
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> eksisterendeIM = Map.of(ARBEIDSGIVER, Set.of(refA));
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmeldingMap = Map.of(ARBEIDSGIVER,
            Set.of(InternArbeidsforholdRef.nullRef()));
        Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmelding = nyInntektsmeldingMap.entrySet().iterator().next();

        // Act
        EndringIArbeidsforholdId.vurderMedÅrsak(result, nyInntektsmelding, eksisterendeIM, grunnlag, yrkesaktiviteterPerArbeidsgiver);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    public void ett_arbeidsforhold_har_inntektsmelding_uten_ref_får_inntektsmelding_med_ref_A() {
        // Arrange
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> yrkesaktiviteterPerArbeidsgiver = Map.of(ARBEIDSGIVER, Set.of(refA));
        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(yrkesaktiviteterPerArbeidsgiver);
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> eksisterendeIM = Map.of(ARBEIDSGIVER, Set.of(InternArbeidsforholdRef.nullRef()));
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmeldingMap = Map.of(ARBEIDSGIVER, Set.of(refA));
        Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmelding = nyInntektsmeldingMap.entrySet().iterator().next();

        // Act
        EndringIArbeidsforholdId.vurderMedÅrsak(result, nyInntektsmelding, eksisterendeIM, grunnlag, yrkesaktiviteterPerArbeidsgiver);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    public void to_arbeidsforhold_AB_får_inntektsmelding_med_ref_B() {
        // Arrange
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> yrkesaktiviteterPerArbeidsgiver = Map.of(ARBEIDSGIVER,
            Set.of(refA, refB));
        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(yrkesaktiviteterPerArbeidsgiver);
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> eksisterendeIM = Map.of(ARBEIDSGIVER, Set.of(refA));
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmeldingMap = Map.of(ARBEIDSGIVER,
            new HashSet<>(Set.of(refA, refB)));
        Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmelding = nyInntektsmeldingMap.entrySet().iterator().next();

        // Act
        EndringIArbeidsforholdId.vurderMedÅrsak(result, nyInntektsmelding, eksisterendeIM, grunnlag, yrkesaktiviteterPerArbeidsgiver);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    public void to_arbeidsforhold_har_ref_A_får_inntektsmelding_uten_ref() {
        // Arrange
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> yrkesaktiviteterPerArbeidsgiver = Map.of(ARBEIDSGIVER,
            Set.of(refA, refB));
        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(yrkesaktiviteterPerArbeidsgiver);
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> eksisterendeIM = Map.of(ARBEIDSGIVER, Set.of(refA));
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmeldingMap = Map.of(ARBEIDSGIVER, Set.of(InternArbeidsforholdRef.nullRef()));
        Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmelding = nyInntektsmeldingMap.entrySet().iterator().next();

        // Act
        EndringIArbeidsforholdId.vurderMedÅrsak(result, nyInntektsmelding, eksisterendeIM, grunnlag, yrkesaktiviteterPerArbeidsgiver);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    public void to_arbeidsforhold_har_inntektsmelding_uten_ref_får_inntektsmelding_med_ref_A() {
        // Arrange
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> yrkesaktiviteterPerArbeidsgiver = Map.of(ARBEIDSGIVER,
            Set.of(refA, refB));
        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(yrkesaktiviteterPerArbeidsgiver);
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> eksisterendeIM = Map.of(ARBEIDSGIVER, Set.of(InternArbeidsforholdRef.nullRef()));
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmeldingMap = Map.of(ARBEIDSGIVER, Set.of(refA));
        Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmelding = nyInntektsmeldingMap.entrySet().iterator().next();

        // Act
        EndringIArbeidsforholdId.vurderMedÅrsak(result, nyInntektsmelding, eksisterendeIM, grunnlag, yrkesaktiviteterPerArbeidsgiver);

        // Assert
        assertMap(refB);

    }

    @Test
    public void to_arbeidsforhold_har_inntektsmeldinger_med_ref_får_inntektsmelding_med_ref_C() {
        // Arrange
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> yrkesaktiviteterPerArbeidsgiver = Map.of(ARBEIDSGIVER,
            Set.of(refA, refB));
        InntektArbeidYtelseGrunnlag grunnlag = lagGrunnlag(yrkesaktiviteterPerArbeidsgiver);
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> eksisterendeIM = Map.of(ARBEIDSGIVER,
            Set.of(refA, refB));
        Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmeldingMap = Map.of(ARBEIDSGIVER,
            new HashSet<>(Set.of(refA, refB, refC)));
        Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> nyInntektsmelding = nyInntektsmeldingMap.entrySet().iterator().next();

        // Act
        EndringIArbeidsforholdId.vurderMedÅrsak(result, nyInntektsmelding, eksisterendeIM, grunnlag, yrkesaktiviteterPerArbeidsgiver);

        // Assert
        assertMap(refC);

    }

    private InntektArbeidYtelseGrunnlag lagGrunnlag(Map<Arbeidsgiver, Set<InternArbeidsforholdRef>> yrkesaktiviteterPerArbeidsgiver) {
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder.oppdatere(Optional.empty());

        for (Map.Entry<Arbeidsgiver, Set<InternArbeidsforholdRef>> entry : yrkesaktiviteterPerArbeidsgiver.entrySet()) {
            Arbeidsgiver arbeidsgiver = entry.getKey();
            Set<InternArbeidsforholdRef> refs = entry.getValue().isEmpty() ? Set.of(InternArbeidsforholdRef.nullRef()) : entry.getValue();
            for (InternArbeidsforholdRef ref : refs) {
                aktørArbeidBuilder.leggTilYrkesaktivitet(YrkesaktivitetBuilder.oppdatere(Optional.empty())
                    .medArbeidsgiver(arbeidsgiver)
                    .medArbeidsforholdId(ref)
                );
            }
        }
        return InntektArbeidYtelseGrunnlagBuilder.nytt()
            .medData(InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER)
                .leggTilAktørArbeid(aktørArbeidBuilder))
            .build();
    }

    private void assertMap(InternArbeidsforholdRef ref) {
        assertThat(result).hasSize(1);
        assertThat(result).hasEntrySatisfying(ARBEIDSGIVER, årsaker ->
            assertThat(årsaker).hasOnlyOneElementSatisfying(arbeidsforholdMedÅrsak -> {
                assertThat(arbeidsforholdMedÅrsak.getRef()).isEqualTo(ref);
                assertThat(arbeidsforholdMedÅrsak.getÅrsaker()).containsOnly(AksjonspunktÅrsak.ENDRING_I_ARBEIDSFORHOLDS_ID);
            })
        );
    }

}
