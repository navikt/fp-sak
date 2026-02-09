package no.nav.foreldrepenger.web.app.tjenester.fpoversikt;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.Inntektskategori;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TilkjentYtelseDtoTjenesteTest {

    @Mock
    private BeregningsresultatRepository beregningsresultatRepository;

    @Mock
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    private TilkjentYtelseDtoTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new TilkjentYtelseDtoTjeneste(beregningsresultatRepository, arbeidsgiverTjeneste);
    }

    @Test
    void skal_returnere_tom_liste_når_beregningsresultat_ikke_finnes() {
        var ref = BehandlingReferanse.fra(ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked());
        when(beregningsresultatRepository.hentBeregningsresultatAggregat(any())).thenReturn(Optional.empty());

        var resultat = tjeneste.lagDtoForTilkjentYtelse(ref);

        assertThat(resultat.utbetalingsperioder()).isEmpty();
    }

    @Test
    void skal_mappe_tilkjent_ytelse_med_en_periode_og_en_andel() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("999999999");
        var ref = BehandlingReferanse.fra(ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked());

        var beregningsresultatEntitet = BeregningsresultatEntitet.builder()
            .medRegelInput("")
            .medRegelSporing("")
            .build();

        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(30);
        var periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultatEntitet);

        BeregningsresultatAndel.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medDagsats(1500)
            .medDagsatsFraBg(1500)
            .medBrukerErMottaker(true)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .build(periode);

        var beregningsresultat = BehandlingBeregningsresultatBuilder.ny()
            .medBgBeregningsresultatFP(beregningsresultatEntitet)
            .build(ref.behandlingId());

        when(beregningsresultatRepository.hentBeregningsresultatAggregat(any())).thenReturn(Optional.of(beregningsresultat));
        when(arbeidsgiverTjeneste.hent(arbeidsgiver)).thenReturn(new ArbeidsgiverOpplysninger("999999999", "Test Bedrift AS"));

        var resultat = tjeneste.lagDtoForTilkjentYtelse(ref).utbetalingsperioder();

        assertThat(resultat).hasSize(1);
        var dto = resultat.getFirst();
        assertThat(dto.fom()).isEqualTo(fom);
        assertThat(dto.tom()).isEqualTo(tom);
        assertThat(dto.andeler()).hasSize(1);

        var andel = dto.andeler().getFirst();
        assertThat(andel.arbeidsgiverIdent()).isEqualTo("999999999");
        assertThat(andel.arbeidsgivernavn()).isEqualTo("Test Bedrift AS");
        assertThat(andel.dagsats()).isEqualTo(1500);
        assertThat(andel.tilBruker()).isTrue();
        assertThat(andel.utbetalingsgrad()).isEqualTo(100.0);
    }

    @Test
    void skal_mappe_tilkjent_ytelse_med_flere_perioder() {
        var arbeidsgiver = Arbeidsgiver.virksomhet("888888888");
        var ref = BehandlingReferanse.fra(ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked());

        var beregningsresultatEntitet = BeregningsresultatEntitet.builder()
            .medRegelInput("")
            .medRegelSporing("")
            .build();

        var fom1 = LocalDate.now();
        var tom1 = LocalDate.now().plusDays(14);
        var periode1 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom1, tom1)
            .build(beregningsresultatEntitet);

        BeregningsresultatAndel.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medDagsats(1000)
            .medDagsatsFraBg(1000)
            .medBrukerErMottaker(false)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(80))
            .build(periode1);

        var fom2 = LocalDate.now().plusDays(15);
        var tom2 = LocalDate.now().plusDays(30);
        var periode2 = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom2, tom2)
            .build(beregningsresultatEntitet);

        BeregningsresultatAndel.builder()
            .medArbeidsgiver(arbeidsgiver)
            .medDagsats(1200)
            .medDagsatsFraBg(1200)
            .medBrukerErMottaker(false)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .build(periode2);

        var beregningsresultat = BehandlingBeregningsresultatBuilder.ny()
            .medBgBeregningsresultatFP(beregningsresultatEntitet)
            .build(ref.behandlingId());

        when(beregningsresultatRepository.hentBeregningsresultatAggregat(any())).thenReturn(Optional.of(beregningsresultat));
        when(arbeidsgiverTjeneste.hent(arbeidsgiver)).thenReturn(new ArbeidsgiverOpplysninger("888888888", "Annen Bedrift"));

        var resultat = tjeneste.lagDtoForTilkjentYtelse(ref).utbetalingsperioder();

        assertThat(resultat).hasSize(2);
        assertThat(resultat.get(0).fom()).isEqualTo(fom1);
        assertThat(resultat.get(0).tom()).isEqualTo(tom1);
        assertThat(resultat.get(0).andeler().getFirst().dagsats()).isEqualTo(1000);
        assertThat(resultat.get(0).andeler().getFirst().utbetalingsgrad()).isEqualTo(80.0);
        assertThat(resultat.get(0).andeler().getFirst().tilBruker()).isFalse();

        assertThat(resultat.get(1).fom()).isEqualTo(fom2);
        assertThat(resultat.get(1).tom()).isEqualTo(tom2);
        assertThat(resultat.get(1).andeler().getFirst().dagsats()).isEqualTo(1200);
        assertThat(resultat.get(1).andeler().getFirst().utbetalingsgrad()).isEqualTo(100.0);
    }

    @Test
    void skal_mappe_periode_med_flere_andeler() {
        var arbeidsgiver1 = Arbeidsgiver.virksomhet("111111111");
        var arbeidsgiver2 = Arbeidsgiver.virksomhet("222222222");
        var ref = BehandlingReferanse.fra(ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked());

        var beregningsresultatEntitet = BeregningsresultatEntitet.builder()
            .medRegelInput("")
            .medRegelSporing("")
            .build();

        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(20);
        var periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultatEntitet);

        BeregningsresultatAndel.builder()
            .medArbeidsgiver(arbeidsgiver1)
            .medDagsats(800)
            .medDagsatsFraBg(800)
            .medBrukerErMottaker(false)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(50))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .build(periode);

        BeregningsresultatAndel.builder()
            .medArbeidsgiver(arbeidsgiver2)
            .medDagsats(700)
            .medDagsatsFraBg(700)
            .medBrukerErMottaker(false)
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .medInntektskategori(Inntektskategori.ARBEIDSTAKER)
            .medStillingsprosent(BigDecimal.valueOf(50))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .build(periode);

        var beregningsresultat = BehandlingBeregningsresultatBuilder.ny()
            .medBgBeregningsresultatFP(beregningsresultatEntitet)
            .build(ref.behandlingId());

        when(beregningsresultatRepository.hentBeregningsresultatAggregat(any())).thenReturn(Optional.of(beregningsresultat));
        when(arbeidsgiverTjeneste.hent(arbeidsgiver1)).thenReturn(new ArbeidsgiverOpplysninger("111111111", "Bedrift En"));
        when(arbeidsgiverTjeneste.hent(arbeidsgiver2)).thenReturn(new ArbeidsgiverOpplysninger("222222222", "Bedrift To"));

        var resultat = tjeneste.lagDtoForTilkjentYtelse(ref).utbetalingsperioder();

        assertThat(resultat).hasSize(1);
        var dto = resultat.getFirst();
        assertThat(dto.andeler()).hasSize(2);

        var andel1 = dto.andeler().get(0);
        assertThat(andel1.arbeidsgiverIdent()).isEqualTo("111111111");
        assertThat(andel1.arbeidsgivernavn()).isEqualTo("Bedrift En");
        assertThat(andel1.dagsats()).isEqualTo(800);

        var andel2 = dto.andeler().get(1);
        assertThat(andel2.arbeidsgiverIdent()).isEqualTo("222222222");
        assertThat(andel2.arbeidsgivernavn()).isEqualTo("Bedrift To");
        assertThat(andel2.dagsats()).isEqualTo(700);
    }

    @Test
    void skal_håndtere_andel_uten_arbeidsgiver() {
        var ref = BehandlingReferanse.fra(ScenarioMorSøkerForeldrepenger.forFødsel().lagMocked());

        var beregningsresultatEntitet = BeregningsresultatEntitet.builder()
            .medRegelInput("")
            .medRegelSporing("")
            .build();

        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var periode = BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(fom, tom)
            .build(beregningsresultatEntitet);

        BeregningsresultatAndel.builder()
            .medDagsats(500)
            .medDagsatsFraBg(500)
            .medBrukerErMottaker(true)
            .medAktivitetStatus(AktivitetStatus.FRILANSER)
            .medInntektskategori(Inntektskategori.FRILANSER)
            .medStillingsprosent(BigDecimal.valueOf(100))
            .medUtbetalingsgrad(BigDecimal.valueOf(100))
            .build(periode);

        var beregningsresultat = BehandlingBeregningsresultatBuilder.ny()
            .medBgBeregningsresultatFP(beregningsresultatEntitet)
            .build(ref.behandlingId());

        when(beregningsresultatRepository.hentBeregningsresultatAggregat(any())).thenReturn(Optional.of(beregningsresultat));

        var resultat = tjeneste.lagDtoForTilkjentYtelse(ref);

        assertThat(resultat.utbetalingsperioder()).hasSize(1);
        var andel = resultat.utbetalingsperioder().getFirst().andeler().getFirst();
        assertThat(andel.arbeidsgiverIdent()).isNull();
        assertThat(andel.arbeidsgivernavn()).isNull();
        assertThat(andel.dagsats()).isEqualTo(500);
        assertThat(andel.tilBruker()).isTrue();
    }
}
