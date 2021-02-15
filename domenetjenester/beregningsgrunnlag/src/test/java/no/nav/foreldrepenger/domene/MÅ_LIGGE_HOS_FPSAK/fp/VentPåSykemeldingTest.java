package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.fp;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.iay.modell.YtelseBuilder;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.RelatertYtelseTilstand;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VentPåSykemeldingTest {
    private static final LocalDate STP = LocalDate.of(2020,6,1);

    @Test
    public void skal_ikke_gi_frist_når_det_ikke_finnes_sykepenger() {
        // Arrange
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedPlusArbeidsdager(førStp(5), 14);
        YtelseBuilder ytelseBuilder = lagYtelse(RelatertYtelseType.FRISINN, periode, RelatertYtelseTilstand.LØPENDE);
        lagYtelseAnvist(ytelseBuilder, periode);

        // Act
        Optional<LocalDate> frist = VentPåSykemelding.utledVenteFrist(new YtelseFilter(Collections.singletonList(ytelseBuilder.build())), STP, STP);

        // Assert
        assertThat(frist).isEmpty();
    }

    @Test
    public void skal_ikke_gi_frist_når_det_ikke_finnes_løpende_sykepenger() {
        // Arrange
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedPlusArbeidsdager(førStp(5), 14);
        YtelseBuilder ytelseBuilder = lagYtelse(RelatertYtelseType.SYKEPENGER, periode, RelatertYtelseTilstand.AVSLUTTET);
        lagYtelseAnvist(ytelseBuilder, periode);

        // Act
        Optional<LocalDate> frist = VentPåSykemelding.utledVenteFrist(new YtelseFilter(Collections.singletonList(ytelseBuilder.build())), STP, STP);

        // Assert
        assertThat(frist).isEmpty();
    }

    @Test
    public void skal_ikke_gi_frist_når_det_er_løpende_sykepenger_og_sykemelding_er_motatt_på_stp() {
        // Arrange
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedPlusArbeidsdager(førStp(5), 14);
        YtelseBuilder ytelseBuilder = lagYtelse(RelatertYtelseType.SYKEPENGER, periode, RelatertYtelseTilstand.LØPENDE);
        lagYtelseAnvist(ytelseBuilder, periode);

        // Act
        Optional<LocalDate> frist = VentPåSykemelding.utledVenteFrist(new YtelseFilter(Collections.singletonList(ytelseBuilder.build())), STP, STP);

        // Assert
        assertThat(frist).isEmpty();
    }

    @Test
    public void skal_ikke_gi_frist_når_det_er_løpende_sykepenger_ikke_basert_på_dagpenger() {
        // Arrange
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedPlusArbeidsdager(førStp(20), 10);
        YtelseBuilder ytelseBuilder = lagYtelse(RelatertYtelseType.SYKEPENGER, periode, RelatertYtelseTilstand.LØPENDE);
        lagYtelseAnvist(ytelseBuilder, periode);
        lagYtelseGrunnlag(ytelseBuilder, Arbeidskategori.ARBEIDSTAKER);

        // Act
        Optional<LocalDate> frist = VentPåSykemelding.utledVenteFrist(new YtelseFilter(Collections.singletonList(ytelseBuilder.build())), STP, STP);

        // Assert
        assertThat(frist).isEmpty();
    }

    @Test
    public void skal_gi_frist_når_det_er_løpende_sykepenger_av_dagpenger_og_sykemelding_mangler_på_stp() {
        // Arrange
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedPlusArbeidsdager(førStp(20), 14);
        YtelseBuilder ytelseBuilder = lagYtelse(RelatertYtelseType.SYKEPENGER, periode, RelatertYtelseTilstand.LØPENDE);
        lagYtelseAnvist(ytelseBuilder, periode);
        lagYtelseGrunnlag(ytelseBuilder, Arbeidskategori.DAGPENGER);

        // Act
        Optional<LocalDate> frist = VentPåSykemelding.utledVenteFrist(new YtelseFilter(Collections.singletonList(ytelseBuilder.build())), STP, STP);

        // Assert
        assertThat(frist).isPresent();
    }

    @Test
    public void skal_ikke_bry_seg_om_andre_ytelser() {
        // Arrange
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedPlusArbeidsdager(førStp(20), 14);
        YtelseBuilder ytelseBuilderSP = lagYtelse(RelatertYtelseType.SYKEPENGER, periode, RelatertYtelseTilstand.LØPENDE);
        lagYtelseAnvist(ytelseBuilderSP, periode);
        lagYtelseGrunnlag(ytelseBuilderSP, Arbeidskategori.DAGPENGER);
        YtelseBuilder ytelseBuilderFRI = lagYtelse(RelatertYtelseType.FRISINN, periode, RelatertYtelseTilstand.LØPENDE);
        lagYtelseAnvist(ytelseBuilderFRI, periode);
        lagYtelseGrunnlag(ytelseBuilderFRI, Arbeidskategori.DAGPENGER);

        // Act
        Optional<LocalDate> frist = VentPåSykemelding.utledVenteFrist(new YtelseFilter(Arrays.asList(ytelseBuilderFRI.build(), ytelseBuilderSP.build())), STP, STP);

        // Assert
        assertThat(frist).isPresent();
    }

    @Test
    public void skal_ikke_vente_når_vi_behandler_over_2_uker_etter_stp() {
        // Arrange
        LocalDate skjæringstidspunkt = LocalDate.of(2020,5,1);
        LocalDate dagensDato = LocalDate.of(2020,5,31);

        // Act
        Optional<LocalDate> frist = kjørSakSomTriggerVentepunkt(skjæringstidspunkt, dagensDato);

        // Assert
        assertThat(frist).isEmpty();
    }

    @Test
    public void skal_gi_korrekt_frist_når_vi_behandler_under_2_uker_etter_stp() {
        // Arrange
        LocalDate skjæringstidspunkt = LocalDate.of(2020,5,1);
        LocalDate dagensDato = LocalDate.of(2020,5,14);

        // Act
        LocalDate frist = kjørSakSomTriggerVentepunkt(skjæringstidspunkt, dagensDato).orElseThrow();

        // Assert
        assertThat(frist).isEqualTo(dagensDato.plusDays(1));
    }

    @Test
    public void skal_gi_korrekt_frist_når_stp_er_etter_behandlingstidspunkt() {
        // Arrange
        LocalDate skjæringstidspunkt = LocalDate.of(2020,5,15);
        LocalDate dagensDato = LocalDate.of(2020,5,1);

        // Act
        LocalDate frist = kjørSakSomTriggerVentepunkt(skjæringstidspunkt, dagensDato).orElseThrow();

        // Assert
        assertThat(frist).isEqualTo(skjæringstidspunkt.plusDays(1));
    }


    private Optional<LocalDate> kjørSakSomTriggerVentepunkt(LocalDate skjæringstidspunkt, LocalDate dagensDato) {
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedPlusArbeidsdager(skjæringstidspunkt.minusDays(20), 14);
        YtelseBuilder ytelseBuilder = lagYtelse(RelatertYtelseType.SYKEPENGER, periode, RelatertYtelseTilstand.LØPENDE);
        lagYtelseAnvist(ytelseBuilder, periode);
        lagYtelseGrunnlag(ytelseBuilder, Arbeidskategori.DAGPENGER);
        return VentPåSykemelding.utledVenteFrist(new YtelseFilter(Collections.singletonList(ytelseBuilder.build())), skjæringstidspunkt, dagensDato);

    }

    private LocalDate førStp(int dager) {
        return STP.minusDays(dager);
    }

    private void lagYtelseAnvist(YtelseBuilder builder, DatoIntervallEntitet periode) {
        builder.medYtelseAnvist(builder.getAnvistBuilder().medAnvistPeriode(periode)
            .medBeløp(BigDecimal.ZERO)
            .medUtbetalingsgradProsent(BigDecimal.ZERO)
            .build());
    }

    private void lagYtelseGrunnlag(YtelseBuilder builder, Arbeidskategori arbeidskategori) {
        builder.medYtelseGrunnlag(builder.getGrunnlagBuilder()
            .medArbeidskategori(arbeidskategori)
            .medDekningsgradProsent(BigDecimal.ZERO)
            .medVedtaksDagsats(Beløp.ZERO)
            .medInntektsgrunnlagProsent(BigDecimal.ZERO)
            .build());
    }

    private YtelseBuilder lagYtelse(RelatertYtelseType type, DatoIntervallEntitet periode, RelatertYtelseTilstand status) {
        return YtelseBuilder.oppdatere(Optional.empty())
            .medPeriode(periode)
            .medYtelseType(type)
            .medStatus(status)
            .medKilde(Fagsystem.INFOTRYGD)
            .medSaksnummer(new Saksnummer("333"));
    }

}
