package no.nav.foreldrepenger.domene.iay.modell;

import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class OpptjeningsnøkkelTest {

    @Test
    void skal_gi_treff_når_både_arbeidsforholdId_og_orgnummer_er_like() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var orgnummer = "orgnummer";
        var nøkkel1 = new Opptjeningsnøkkel(arbeidsforholdId, orgnummer, null);
        var nøkkel2 = new Opptjeningsnøkkel(arbeidsforholdId, orgnummer, null);

        Assertions.assertThat(nøkkel1.matcher(nøkkel2)).isTrue();
        Assertions.assertThat(nøkkel2.matcher(nøkkel1)).isTrue();
    }

    @Test
    void skal_ikke_treff_når_arbeidsforholdId_er_like_men_ikke_orgnummer() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var orgnummer = "orgnummer";
        var orgnummer2 = "orgnummer2";
        var nøkkel1 = new Opptjeningsnøkkel(arbeidsforholdId, orgnummer, null);
        var nøkkel2 = new Opptjeningsnøkkel(arbeidsforholdId, orgnummer2, null);

        Assertions.assertThat(nøkkel1.matcher(nøkkel2)).isFalse();
        Assertions.assertThat(nøkkel2.matcher(nøkkel1)).isFalse();
    }

    @Test
    void skal_gi_treff_når_arbeidsforholdId_er_null_på_en_av_nøklene_og_orgnummer_er_like() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var orgnummer = "orgnummer";
        var nøkkel1 = new Opptjeningsnøkkel(null, orgnummer, null);
        var nøkkel2 = new Opptjeningsnøkkel(arbeidsforholdId, orgnummer, null);

        Assertions.assertThat(nøkkel1.matcher(nøkkel2)).isTrue();
        Assertions.assertThat(nøkkel2.matcher(nøkkel1)).isTrue();
    }

    @Test
    void skal_gi_ikke_gi_treff_når_arbeidsforholdId_er_null_på_en_av_nøklene_og_orgnummer_er_ulike() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        var orgnummer = "orgnummer";
        var orgnummer2 = "orgnummer2";
        var nøkkel1 = new Opptjeningsnøkkel(null, orgnummer, null);
        var nøkkel2 = new Opptjeningsnøkkel(arbeidsforholdId, orgnummer2, null);

        Assertions.assertThat(nøkkel1.matcher(nøkkel2)).isFalse();
        Assertions.assertThat(nøkkel2.matcher(nøkkel1)).isFalse();
    }

    @Test
    void skal_gi_treff_når_aktør_id_er_like() {
        var aktørId = "123";
        var nøkkel1 = new Opptjeningsnøkkel(null, null, aktørId);
        var nøkkel2 = new Opptjeningsnøkkel(null, null, aktørId);

        Assertions.assertThat(nøkkel1.matcher(nøkkel2)).isTrue();
        Assertions.assertThat(nøkkel2.matcher(nøkkel1)).isTrue();
    }

    @Test
    void skal_ikke_gi_treff_når_aktør_id_er_ulike() {
        var aktørId = "123";
        var aktørId2 = "1234";
        var nøkkel1 = new Opptjeningsnøkkel(null, null, aktørId);
        var nøkkel2 = new Opptjeningsnøkkel(null, null, aktørId2);

        Assertions.assertThat(nøkkel1.matcher(nøkkel2)).isFalse();
        Assertions.assertThat(nøkkel2.matcher(nøkkel1)).isFalse();
    }
}
