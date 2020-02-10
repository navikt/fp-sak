package no.nav.foreldrepenger.domene.iay.modell;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class OpptjeningsnøkkelTest {


    @Test
    public void skal_gi_treff_når_både_arbeidsforholdId_og_orgnummer_er_like() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        String orgnummer = "orgnummer";
        Opptjeningsnøkkel nøkkel1 = new Opptjeningsnøkkel(arbeidsforholdId, orgnummer, null);
        Opptjeningsnøkkel nøkkel2 = new Opptjeningsnøkkel(arbeidsforholdId, orgnummer, null);

        Assertions.assertThat(nøkkel1.matcher(nøkkel2)).isTrue();
        Assertions.assertThat(nøkkel2.matcher(nøkkel1)).isTrue();
    }

    @Test
    public void skal_ikke_treff_når_arbeidsforholdId_er_like_men_ikke_orgnummer() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        String orgnummer = "orgnummer";
        String orgnummer2 = "orgnummer2";
        Opptjeningsnøkkel nøkkel1 = new Opptjeningsnøkkel(arbeidsforholdId, orgnummer, null);
        Opptjeningsnøkkel nøkkel2 = new Opptjeningsnøkkel(arbeidsforholdId, orgnummer2, null);

        Assertions.assertThat(nøkkel1.matcher(nøkkel2)).isFalse();
        Assertions.assertThat(nøkkel2.matcher(nøkkel1)).isFalse();
    }

    @Test
    public void skal_gi_treff_når_arbeidsforholdId_er_null_på_en_av_nøklene_og_orgnummer_er_like() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        String orgnummer = "orgnummer";
        Opptjeningsnøkkel nøkkel1 = new Opptjeningsnøkkel(null, orgnummer, null);
        Opptjeningsnøkkel nøkkel2 = new Opptjeningsnøkkel(arbeidsforholdId, orgnummer, null);

        Assertions.assertThat(nøkkel1.matcher(nøkkel2)).isTrue();
        Assertions.assertThat(nøkkel2.matcher(nøkkel1)).isTrue();
    }

    @Test
    public void skal_gi_ikke_gi_treff_når_arbeidsforholdId_er_null_på_en_av_nøklene_og_orgnummer_er_ulike() {
        var arbeidsforholdId = InternArbeidsforholdRef.nyRef();
        String orgnummer = "orgnummer";
        String orgnummer2 = "orgnummer2";
        Opptjeningsnøkkel nøkkel1 = new Opptjeningsnøkkel(null, orgnummer, null);
        Opptjeningsnøkkel nøkkel2 = new Opptjeningsnøkkel(arbeidsforholdId, orgnummer2, null);

        Assertions.assertThat(nøkkel1.matcher(nøkkel2)).isFalse();
        Assertions.assertThat(nøkkel2.matcher(nøkkel1)).isFalse();
    }

    @Test
    public void skal_gi_treff_når_aktør_id_er_like() {
        String aktørId = "123";
        Opptjeningsnøkkel nøkkel1 = new Opptjeningsnøkkel(null, null, aktørId);
        Opptjeningsnøkkel nøkkel2 = new Opptjeningsnøkkel(null, null, aktørId);

        Assertions.assertThat(nøkkel1.matcher(nøkkel2)).isTrue();
        Assertions.assertThat(nøkkel2.matcher(nøkkel1)).isTrue();
    }

    @Test
    public void skal_ikke_gi_treff_når_aktør_id_er_ulike() {
        String aktørId = "123";
        String aktørId2 = "1234";
        Opptjeningsnøkkel nøkkel1 = new Opptjeningsnøkkel(null, null, aktørId);
        Opptjeningsnøkkel nøkkel2 = new Opptjeningsnøkkel(null, null, aktørId2);

        Assertions.assertThat(nøkkel1.matcher(nøkkel2)).isFalse();
        Assertions.assertThat(nøkkel2.matcher(nøkkel1)).isFalse();
    }
}
