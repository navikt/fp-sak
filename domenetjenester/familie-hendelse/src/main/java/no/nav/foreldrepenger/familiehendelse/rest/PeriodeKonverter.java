package no.nav.foreldrepenger.familiehendelse.rest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.PeriodeUtenOmsorgEntitet;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class PeriodeKonverter {
    private static final String PERIODE_SEPARATOR = " , ";
    private static final String PERIODE_SEPARATOR_ENDE =  " og ";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private PeriodeKonverter() {
    }

    public static String konvertPerioderTilString(List<PeriodeDto> perioder) {
        if(perioder != null) {
            if(perioder.isEmpty()) {
                return null;
            }
            var result = new StringBuilder();
            var perioderList = perioder.stream().map(PeriodeKonverter::konvertPeriodeTilString).collect(Collectors.toList());
            var lastIndex = perioderList.size() - 1;
            result.append(lastIndex == 0 ? perioderList.get(lastIndex) :
                perioderList.subList(0, lastIndex).stream().collect(Collectors.joining(PERIODE_SEPARATOR)).concat(PERIODE_SEPARATOR_ENDE).concat(perioderList.get(lastIndex)));
            return result.toString();
        }
        return null;
    }

    private static String konvertPeriodeTilString(PeriodeDto periode) {
        return konvertDatoTilString(periode.getPeriodeFom(), periode.getPeriodeTom());
    }

    private static String konvertDatoTilString(LocalDate periodeFom, LocalDate periodeTom) {
        return periodeTom == null || periodeTom.equals(DatoIntervallEntitet.TIDENES_ENDE) ?
            DATE_FORMATTER.format(periodeFom).concat("-").concat("") : DATE_FORMATTER.format(periodeFom).concat("-").concat(DATE_FORMATTER.format(periodeTom));
    }

    public static List<PeriodeDto> mapUtenOmsorgperioder(List<PeriodeUtenOmsorgEntitet> periodeUtenOmsorgs) {
        List<PeriodeDto> result = new ArrayList<>();
        if(!Objects.isNull(periodeUtenOmsorgs) && !periodeUtenOmsorgs.isEmpty()) {
            result.addAll(periodeUtenOmsorgs.stream().map(PeriodeKonverter::mapUtenOmsorgperiode).collect(Collectors.toList()));
        }
        return result;
    }

    private static PeriodeDto mapUtenOmsorgperiode(PeriodeUtenOmsorgEntitet dto) {
        return mapPeriode(dto.getPeriode());
    }

    private static PeriodeDto mapPeriode(DatoIntervallEntitet intervallEntitet) {
        var periodeDto =  new PeriodeDto();
        periodeDto.setPeriodeFom(intervallEntitet.getFomDato());
        periodeDto.setPeriodeTom(DatoIntervallEntitet.TIDENES_ENDE.equals(intervallEntitet.getTomDato()) ? null : intervallEntitet.getTomDato());
        return periodeDto;
    }

    public static List<DatoIntervallEntitet> mapIkkeOmsorgsperioder(List<PeriodeDto> ikkeOmsorgPeriodeDtos, Boolean omsorg) {
        List<DatoIntervallEntitet> result = new ArrayList<>();
        if(Boolean.FALSE.equals(omsorg) && !Objects.isNull(ikkeOmsorgPeriodeDtos) && !ikkeOmsorgPeriodeDtos.isEmpty()) {
            result.addAll(ikkeOmsorgPeriodeDtos.stream().map(PeriodeKonverter::mapIkkeOmsorgperiodeDto).collect(Collectors.toList()));
        }
        return result;
    }

    private static DatoIntervallEntitet mapIkkeOmsorgperiodeDto(PeriodeDto dto) {
        return mapIkkeOmsorgperiode(dto.getPeriodeFom(), dto.getPeriodeTom());
    }

    private static DatoIntervallEntitet mapIkkeOmsorgperiode(LocalDate periodeFom, LocalDate periodeTom) {
        DatoIntervallEntitet datoIntervallEntitet;
        if(periodeTom != null) {
            datoIntervallEntitet = DatoIntervallEntitet.fraOgMedTilOgMed(periodeFom, periodeTom);
        }else {
            datoIntervallEntitet = DatoIntervallEntitet.fraOgMed(periodeFom);
        }
        return datoIntervallEntitet;
    }
}
