package no.nav.foreldrepenger.ytelse.beregning;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.SjekkForIngenAndelerOgAndelerUtenDagsats;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatAndelEndringModell;
import no.nav.foreldrepenger.ytelse.beregning.endringsdato.regelmodell.BeregningsresultatPeriodeEndringModell;

public class SjekkForIngenAndelerOgAndelerUtenDagsatsTest {

    private static final InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.namedRef("TEST-REF");
    private static final String ORGNR = KUNSTIG_ORG;

    private SjekkForIngenAndelerOgAndelerUtenDagsats sjekkForIngenAndelerOgAndelerUtenDagsats;
    private BeregningsresultatEntitet beregningsresultatFørstegangsbehandling;
    private BeregningsresultatEntitet beregningsresultatRevurdering;
    private LocalDate fom;
    private LocalDate tom;

    @BeforeEach
    void setUp() {
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
    public void endring_nyPeriode_uten_andel_uten_dagsats_hvor_gammelPeriode_er_null() {
        var nyPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of());
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, null);
        assertThat(endring).isTrue();
    }

    @Test
    public void endring_nyPeriode_med_andel_uten_dagsats_hvor_gammelPeriode_er_null() {
        var andel = opprettBeregningsresultatAndel(0);
        var nyPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel));
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, null);
        assertThat(endring).isTrue();
    }

    @Test
    public void ingen_endring_nyPeriode_med_andel_med_dagsats_hvor_gammelPeriode_er_null() {
        var andel = opprettBeregningsresultatAndel(1000);
        var nyPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel));
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, null);
        assertThat(endring).isFalse();
    }

    @Test
    public void endring_gammelPeriode_uten_andel_uten_dagsats_hvor_gammelPeriode_er_null() {
        var gammelPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of());
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(null, gammelPeriode);
        assertThat(endring).isTrue();
    }

    @Test
    public void endring_gammelPeriode_med_andel_uten_dagsats_hvor_gammelPeriode_er_null() {
        var andel = opprettBeregningsresultatAndel(0);
        var gammelPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel));
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(null, gammelPeriode);
        assertThat(endring).isTrue();
    }

    @Test
    public void ingen_endring_gammelPeriode_med_andel_med_dagsats_hvor_gammelPeriode_er_null() {
        var andel = opprettBeregningsresultatAndel(1000);
        var gammelPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel));
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(null, gammelPeriode);
        assertThat(endring).isFalse();
    }

    @Test
    public void endring_nyPeriode_uten_andel_uten_dagsats_og_gammelPeriode_uten_andel_uten_dagsats() {
        var nyPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of());
        var gammelPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of());
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isTrue();
    }

    @Test
    public void endring_nyPeriode_med_andel_uten_dagsats_og_gammelPeriode_uten_andel_uten_dagsats() {
        var andel = opprettBeregningsresultatAndel(0);
        var nyPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel));
        var gammelPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of());
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isTrue();
    }

    @Test
    public void ingen_endring_nyPeriode_med_andel_med_dagsats_og_gammelPeriode_uten_andel_uten_dagsats() {
        var andel = opprettBeregningsresultatAndel(1000);
        var nyPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel));
        var gammelPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of());
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isFalse();
    }

    @Test
    public void endring_nyPeriode_uten_andel_uten_dagsats_og_gammelPeriode_med_andel_uten_dagsats() {
        var nyPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of());
        var andel = opprettBeregningsresultatAndel(0);
        var gammelPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel));
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isTrue();
    }

    @Test
    public void ingen_endring_nyPeriode_uten_andel_uten_dagsats_og_gammelPeriode_med_andel_med_dagsats() {
        var nyPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of());
        var andel = opprettBeregningsresultatAndel(1000);
        var gammelPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel));
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isFalse();
    }

    @Test
    public void endring_nyPeriode_med_andel_uten_dagsats_og_gammelPeriode_med_andel_uten_dagsats() {
        var andel = opprettBeregningsresultatAndel(0);
        var nyPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel));
        var andel2 = opprettBeregningsresultatAndel(0);
        var gammelPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel2));
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isTrue();
    }

    @Test
    public void ingen_endring_nyPeriode_med_andel_med_dagsats_og_gammelPeriode_med_andel_uten_dagsats() {
        var andel = opprettBeregningsresultatAndel(1000);
        var nyPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel));
        var andel2 = opprettBeregningsresultatAndel(0);
        var gammelPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel2));
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isFalse();
    }

    @Test
    public void ingen_endring_nyPeriode_med_andel_uten_dagsats_og_gammelPeriode_med_andel_med_dagsats() {
        var andel = opprettBeregningsresultatAndel(0);
        var nyPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel));
        var andel2 = opprettBeregningsresultatAndel(1000);
        var gammelPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel2));
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isFalse();
    }

    @Test
    public void ingen_endring_nyPeriode_med_andel_med_dagsats_og_gammelPeriode_med_andel_med_dagsats() {
        var andel = opprettBeregningsresultatAndel(1000);
        var nyPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel));
        var andel2 = opprettBeregningsresultatAndel(1000);
        var gammelPeriode = opprettBeregningsresultatPeriode(fom, tom, List.of(andel2));
        boolean endring = sjekkForIngenAndelerOgAndelerUtenDagsats.sjekk(nyPeriode, gammelPeriode);
        assertThat(endring).isFalse();
    }

    private BeregningsresultatPeriodeEndringModell opprettBeregningsresultatPeriode(LocalDate fom, LocalDate tom, List<BeregningsresultatAndelEndringModell> andeler) {
        return new BeregningsresultatPeriodeEndringModell(fom, tom, andeler);
    }

    private BeregningsresultatAndelEndringModell opprettBeregningsresultatAndel(int dagsats) {
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORGNR);
        return new BeregningsresultatAndelEndringModell(AktivitetStatus.ARBEIDSTAKER, Inntektskategori.ARBEIDSTAKER, arbeidsgiver, InternArbeidsforholdRef.nullRef(), false, dagsats);
    }

}
