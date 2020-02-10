package no.nav.foreldrepenger.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class LeggTilResultatTest {

    private Map<Arbeidsgiver, Set<ArbeidsforholdMedÅrsak>> result;
    private ArbeidsforholdMedÅrsak a1;
    private ArbeidsforholdMedÅrsak a2;
    private InternArbeidsforholdRef ref1;
    private InternArbeidsforholdRef ref2;

    @Before
    public void setup(){
        ref1 = InternArbeidsforholdRef.nyRef();
        ref2 = InternArbeidsforholdRef.nyRef();
        a1 = new ArbeidsforholdMedÅrsak(ref1);
        a2 = new ArbeidsforholdMedÅrsak(ref2);
        result = new HashMap<>();
    }

    @Test
    public void skal_legge_til_årsak_på_allerede_eksisterende_arbeidsgiver_med_matchende_arbeidsforholdref(){
        // Arrange
        Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("1");
        Set<InternArbeidsforholdRef> refs = Set.of(ref1, ref2);
        result.put(virksomhet, Set.of(a1, a2));
        // Act
        LeggTilResultat.leggTil(result, AksjonspunktÅrsak.PERMISJON, virksomhet, refs);
        // Assert
        assertThat(result).hasEntrySatisfying(virksomhet, årsaker -> {
            assertThat(årsaker).hasSize(2);
            assertThat(årsaker).anySatisfy(årsak -> {
                assertThat(årsak.getRef()).isEqualTo(ref1);
                assertThat(årsak.getÅrsaker()).containsExactlyInAnyOrder(AksjonspunktÅrsak.PERMISJON);
            });
            assertThat(årsaker).anySatisfy(årsak -> {
                assertThat(årsak.getRef()).isEqualTo(ref2);
                assertThat(årsak.getÅrsaker()).containsExactlyInAnyOrder(AksjonspunktÅrsak.PERMISJON);
            });
        });
    }

    @Test
    public void skal_legge_til_årsak_på_arbeidsgiver_som_ikke_eksisterer_i_result_i_utgangspunktet(){
        // Arrange
        Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("1");
        Set<InternArbeidsforholdRef> refs = Set.of(ref1, ref2);
        // Act
        LeggTilResultat.leggTil(result, AksjonspunktÅrsak.ENDRING_I_ARBEIDSFORHOLDS_ID, virksomhet, refs);
        // Assert
        assertThat(result).hasEntrySatisfying(virksomhet, årsaker -> {
            assertThat(årsaker).hasSize(2);
            assertThat(årsaker).anySatisfy(årsak -> {
                assertThat(årsak.getRef()).isEqualTo(ref1);
                assertThat(årsak.getÅrsaker()).containsExactlyInAnyOrder(AksjonspunktÅrsak.ENDRING_I_ARBEIDSFORHOLDS_ID);
            });
            assertThat(årsaker).anySatisfy(årsak -> {
                assertThat(årsak.getRef()).isEqualTo(ref2);
                assertThat(årsak.getÅrsaker()).containsExactlyInAnyOrder(AksjonspunktÅrsak.ENDRING_I_ARBEIDSFORHOLDS_ID);
            });
        });
    }

    @Test
    public void skal_legge_til_arbeidsgiver_selv_når_ingen_arbeidsforholdref(){
        // Arrange
        Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("1");
        Set<InternArbeidsforholdRef> refs = Set.of();
        // Act
        LeggTilResultat.leggTil(result, AksjonspunktÅrsak.MANGLENDE_INNTEKTSMELDING, virksomhet, refs);
        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.containsKey(virksomhet)).isTrue();
        assertThat(result.get(virksomhet)).hasSize(0);
    }

    @Test
    public void skal_legge_til_to_årsaker_for_arbeidsgiver_med_to_arbeidsforholdref(){
        // Arrange
        Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("1");
        Set<InternArbeidsforholdRef> refs = Set.of(ref1, ref2);
        // Act
        LeggTilResultat.leggTil(result, AksjonspunktÅrsak.ENDRING_I_ARBEIDSFORHOLDS_ID, virksomhet, refs);
        LeggTilResultat.leggTil(result, AksjonspunktÅrsak.PERMISJON, virksomhet, refs);
        // Assert
        assertThat(result).hasEntrySatisfying(virksomhet, årsaker -> {
            assertThat(årsaker).hasSize(2);
            assertThat(årsaker).anySatisfy(årsak -> {
                assertThat(årsak.getRef()).isEqualTo(ref1);
                assertThat(årsak.getÅrsaker()).containsExactlyInAnyOrder(
                    AksjonspunktÅrsak.ENDRING_I_ARBEIDSFORHOLDS_ID, AksjonspunktÅrsak.PERMISJON);
            });
            assertThat(årsaker).anySatisfy(årsak -> {
                assertThat(årsak.getRef()).isEqualTo(ref2);
                assertThat(årsak.getÅrsaker()).containsExactlyInAnyOrder(
                    AksjonspunktÅrsak.ENDRING_I_ARBEIDSFORHOLDS_ID, AksjonspunktÅrsak.PERMISJON);
            });
        });
    }

}
