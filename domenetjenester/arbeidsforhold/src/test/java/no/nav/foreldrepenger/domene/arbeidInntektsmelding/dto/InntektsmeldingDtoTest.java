package no.nav.foreldrepenger.domene.arbeidInntektsmelding.dto;

import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

class InntektsmeldingDtoTest {

    @Test
    void skal_regne_ut_bortfalte_naturalytelser_fra_aktiveNaturalytelser() {
        var aktiveNaturalytelser = List.of(
            new NaturalYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2020, 3, 2), LocalDate.of(2020, 10, 2)), BigDecimal.valueOf(1),
                NaturalYtelseType.KOST_DAGER),
            new NaturalYtelse(DatoIntervallEntitet.fraOgMed(LocalDate.of(2024, 3, 2)), BigDecimal.valueOf(3), NaturalYtelseType.KOST_DAGER),
            new NaturalYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2021, 3, 2), LocalDate.of(2022, 5, 2)), BigDecimal.valueOf(4),
                NaturalYtelseType.BIL),
            new NaturalYtelse(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2022, 1, 2), LocalDate.of(2022, 12, 30)), BigDecimal.valueOf(2),
                NaturalYtelseType.KOST_DAGER));
        
        var res = InntektsmeldingDto.mapToBortfalteNaturalytelser(aktiveNaturalytelser);
        assertThat(res).hasSize(2);
        assertThat(res.get(NaturalYtelseType.KOST_DAGER)).hasSize(2);
        assertThat(res.get(NaturalYtelseType.KOST_DAGER).getFirst()).isEqualTo(
            new InntektsmeldingDto.BortfaltNaturalytelse(LocalDate.of(2020, 10, 3), LocalDate.of(2022, 1, 1), Beløp.fra(BigDecimal.ONE)));
        assertThat(res.get(NaturalYtelseType.KOST_DAGER).get(1)).isEqualTo(
            new InntektsmeldingDto.BortfaltNaturalytelse(LocalDate.of(2022, 12, 31), LocalDate.of(2024, 3, 1), Beløp.fra(BigDecimal.valueOf(2))));

        assertThat(res.get(NaturalYtelseType.BIL).getFirst()).isEqualTo(
            new InntektsmeldingDto.BortfaltNaturalytelse(LocalDate.of(2022, 5, 3), TIDENES_ENDE, Beløp.fra(BigDecimal.valueOf(4))));

    }
}
