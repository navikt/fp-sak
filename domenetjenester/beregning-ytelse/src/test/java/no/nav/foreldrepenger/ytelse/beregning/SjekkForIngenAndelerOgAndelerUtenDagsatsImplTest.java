package no.nav.foreldrepenger.ytelse.beregning;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Java6Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class SjekkForIngenAndelerOgAndelerUtenDagsatsImplTest {

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final String ORGNR = KUNSTIG_ORG;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private SjekkForIngenAndelerOgAndelerUtenDagsats sjekkForIngenAndelerOgAndelerUtenDagsats;
    private BeregningsresultatEntitet beregningsresultatFørstegangsbehandling;
    private BeregningsresultatEntitet beregningsresultatRevurdering;
    private LocalDate fom;
    private LocalDate tom;

    @Before
    public void oppsett(){
        sjekkForIngenAndelerOgAndelerUtenDagsats = new SjekkForIngenAndelerOgAndelerUtenDagsats();
        beregningsresultatFørstegangsbehandling = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        beregningsresultatRevurdering = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        fom = LocalDate.now();
        tom = LocalDate.now().plusWeeks(1);
    }

    @Test
    public void endring_nyPeriode_uten_andel_uten_dagsats_hvor_gammelPeriode_er_null(){
        BeregningsresultatPeriode nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, null);
        assertThat(endring).isTrue();
    }

    @Test
    public void endring_nyPeriode_med_andel_uten_dagsats_hvor_gammelPeriode_er_null(){
        BeregningsresultatPeriode nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
        opprettBeregningsresultatAndel(nyPeriode, 0);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, null);
        assertThat(endring).isTrue();
    }

    @Test
    public void ingen_endring_nyPeriode_med_andel_med_dagsats_hvor_gammelPeriode_er_null(){
        BeregningsresultatPeriode nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
        opprettBeregningsresultatAndel(nyPeriode, 1000);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, null);
        assertThat(endring).isFalse();
    }

    @Test
    public void endring_gammelPeriode_uten_andel_uten_dagsats_hvor_gammelPeriode_er_null(){
        BeregningsresultatPeriode gammelPeriode = opprettBeregningsresultatPeriode(beregningsresultatFørstegangsbehandling, fom, tom);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(null, gammelPeriode);
        assertThat(endring).isTrue();
    }

    @Test
    public void endring_gammelPeriode_med_andel_uten_dagsats_hvor_gammelPeriode_er_null(){
        BeregningsresultatPeriode gammelPeriode = opprettBeregningsresultatPeriode(beregningsresultatFørstegangsbehandling, fom, tom);
        opprettBeregningsresultatAndel(gammelPeriode, 0);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(null, gammelPeriode);
        assertThat(endring).isTrue();
    }

    @Test
    public void ingen_endring_gammelPeriode_med_andel_med_dagsats_hvor_gammelPeriode_er_null(){
        BeregningsresultatPeriode gammelPeriode = opprettBeregningsresultatPeriode(beregningsresultatFørstegangsbehandling, fom, tom);
        opprettBeregningsresultatAndel(gammelPeriode, 1000);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(null, gammelPeriode);
        assertThat(endring).isFalse();
    }

    @Test
    public void endring_nyPeriode_uten_andel_uten_dagsats_og_gammelPeriode_uten_andel_uten_dagsats(){
        BeregningsresultatPeriode nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
        BeregningsresultatPeriode gammelPeriode = opprettBeregningsresultatPeriode(beregningsresultatFørstegangsbehandling, fom, tom);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isTrue();
    }

    @Test
    public void endring_nyPeriode_med_andel_uten_dagsats_og_gammelPeriode_uten_andel_uten_dagsats(){
        BeregningsresultatPeriode nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
        opprettBeregningsresultatAndel(nyPeriode, 0);
        BeregningsresultatPeriode gammelPeriode = opprettBeregningsresultatPeriode(beregningsresultatFørstegangsbehandling, fom, tom);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isTrue();
    }

    @Test
    public void ingen_endring_nyPeriode_med_andel_med_dagsats_og_gammelPeriode_uten_andel_uten_dagsats(){
        BeregningsresultatPeriode nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
        opprettBeregningsresultatAndel(nyPeriode, 1000);
        BeregningsresultatPeriode gammelPeriode = opprettBeregningsresultatPeriode(beregningsresultatFørstegangsbehandling, fom, tom);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isFalse();
    }

    @Test
    public void endring_nyPeriode_uten_andel_uten_dagsats_og_gammelPeriode_med_andel_uten_dagsats(){
        BeregningsresultatPeriode nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
        BeregningsresultatPeriode gammelPeriode = opprettBeregningsresultatPeriode(beregningsresultatFørstegangsbehandling, fom, tom);
        opprettBeregningsresultatAndel(gammelPeriode, 0);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isTrue();
    }

    @Test
    public void ingen_endring_nyPeriode_uten_andel_uten_dagsats_og_gammelPeriode_med_andel_med_dagsats(){
        BeregningsresultatPeriode nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
        BeregningsresultatPeriode gammelPeriode = opprettBeregningsresultatPeriode(beregningsresultatFørstegangsbehandling, fom, tom);
        opprettBeregningsresultatAndel(gammelPeriode, 1000);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isFalse();
    }

    @Test
    public void endring_nyPeriode_med_andel_uten_dagsats_og_gammelPeriode_med_andel_uten_dagsats(){
        BeregningsresultatPeriode nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
        opprettBeregningsresultatAndel(nyPeriode, 0);
        BeregningsresultatPeriode gammelPeriode = opprettBeregningsresultatPeriode(beregningsresultatFørstegangsbehandling, fom, tom);
        opprettBeregningsresultatAndel(gammelPeriode, 0);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isTrue();
    }

    @Test
    public void ingen_endring_nyPeriode_med_andel_med_dagsats_og_gammelPeriode_med_andel_uten_dagsats(){
        BeregningsresultatPeriode nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
        opprettBeregningsresultatAndel(nyPeriode, 1000);
        BeregningsresultatPeriode gammelPeriode = opprettBeregningsresultatPeriode(beregningsresultatFørstegangsbehandling, fom, tom);
        opprettBeregningsresultatAndel(gammelPeriode, 0);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isFalse();
    }

    @Test
    public void ingen_endring_nyPeriode_med_andel_uten_dagsats_og_gammelPeriode_med_andel_med_dagsats(){
        BeregningsresultatPeriode nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
        opprettBeregningsresultatAndel(nyPeriode, 0);
        BeregningsresultatPeriode gammelPeriode = opprettBeregningsresultatPeriode(beregningsresultatFørstegangsbehandling, fom, tom);
        opprettBeregningsresultatAndel(gammelPeriode, 1000);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isFalse();
    }

    @Test
    public void ingen_endring_nyPeriode_med_andel_med_dagsats_og_gammelPeriode_med_andel_med_dagsats(){
        BeregningsresultatPeriode nyPeriode = opprettBeregningsresultatPeriode(beregningsresultatRevurdering, fom, tom);
        opprettBeregningsresultatAndel(nyPeriode, 1000);
        BeregningsresultatPeriode gammelPeriode = opprettBeregningsresultatPeriode(beregningsresultatFørstegangsbehandling, fom, tom);
        opprettBeregningsresultatAndel(gammelPeriode, 1000);
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isFalse();
    }

    private BeregningsresultatPeriode opprettBeregningsresultatPeriode(BeregningsresultatEntitet beregningsresultat, LocalDate fom, LocalDate tom){
        return BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultat);
    }

    private void opprettBeregningsresultatAndel(BeregningsresultatPeriode beregningsresultatPeriode, int dagsats) {
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        BeregningsresultatAndel.builder()
            .medBrukerErMottaker(false)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdRef(ARBEIDSFORHOLD_ID)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .medDagsats(dagsats)
            .medDagsatsFraBg(dagsats)
            .build(beregningsresultatPeriode);
    }

}
