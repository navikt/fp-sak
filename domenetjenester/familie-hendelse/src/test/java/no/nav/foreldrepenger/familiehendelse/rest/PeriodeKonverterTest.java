package no.nav.foreldrepenger.familiehendelse.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class PeriodeKonverterTest {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Test
    public void testPeriodeKonverter() {
        LocalDate iDag = LocalDate.now();
        List<PeriodeDto> ikkeOmsorgPerioder = new ArrayList<>();
        PeriodeDto periodeDto = new PeriodeDto();
        periodeDto.setPeriodeFom(iDag.minusMonths(3));
        periodeDto.setPeriodeTom(iDag.minusMonths(2));
        ikkeOmsorgPerioder.add(periodeDto);
        periodeDto = new PeriodeDto();
        periodeDto.setPeriodeFom(iDag.minusMonths(2));
        periodeDto.setPeriodeTom(iDag.minusMonths(1));
        ikkeOmsorgPerioder.add(periodeDto);

        String testString = DATE_FORMATTER.format(ikkeOmsorgPerioder.get(0).getPeriodeFom())+ "-"+DATE_FORMATTER.format(ikkeOmsorgPerioder.get(0).getPeriodeTom())+" og "+
            DATE_FORMATTER.format(ikkeOmsorgPerioder.get(1).getPeriodeFom())+ "-"+DATE_FORMATTER.format(ikkeOmsorgPerioder.get(1).getPeriodeTom());

        assertThat(PeriodeKonverter.konvertPerioderTilString(ikkeOmsorgPerioder)).isEqualTo(testString);
    }

}
