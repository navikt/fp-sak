package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import static no.nav.foreldrepenger.domene.mappers.til_kalkulus.IAYMapperTilKalkulus.mapArbeidsforholdRef;

import java.util.List;

import no.nav.folketrygdloven.kalkulator.modell.svp.AktivitetDto;
import no.nav.folketrygdloven.kalkulator.modell.svp.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulator.modell.svp.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.PeriodeMedUtbetalingsgrad;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.TilretteleggingArbeidsforhold;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.TilretteleggingMedUtbelingsgrad;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class TilretteleggingMapperTilKalkulus {

    private TilretteleggingMapperTilKalkulus() {
    }

    public static List<UtbetalingsgradPrAktivitetDto> mapTilretteleggingerMedUtbetalingsgrad(List<TilretteleggingMedUtbelingsgrad> tilretteleggingMedUtbelingsgrad) {
        return tilretteleggingMedUtbelingsgrad.stream().
            map(TilretteleggingMapperTilKalkulus::mapTilretteleggingMedUtbetalingsgrad)
            .toList();
    }

    private static UtbetalingsgradPrAktivitetDto mapTilretteleggingMedUtbetalingsgrad(TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad) {
        return new UtbetalingsgradPrAktivitetDto(
            mapTilretteleggingArbeidsforhold(tilretteleggingMedUtbelingsgrad.getTilretteleggingArbeidsforhold()),
            mapPerioderMedUtbetalingsgrad(tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad())
        );
    }

    private static List<PeriodeMedUtbetalingsgradDto> mapPerioderMedUtbetalingsgrad(List<PeriodeMedUtbetalingsgrad> periodeMedUtbetalingsgrad) {
        return periodeMedUtbetalingsgrad.stream()
            .map(TilretteleggingMapperTilKalkulus::mapPeriodeMedUtbetalingsgrad)
            .toList();
    }

    private static PeriodeMedUtbetalingsgradDto mapPeriodeMedUtbetalingsgrad(PeriodeMedUtbetalingsgrad periodeMedUtbetalingsgrad) {
        return new PeriodeMedUtbetalingsgradDto(mapDatoIntervall(periodeMedUtbetalingsgrad.getPeriode()), periodeMedUtbetalingsgrad.getUtbetalingsgrad());
    }

    private static Intervall mapDatoIntervall(DatoIntervallEntitet periode) {
        return periode.getTomDato() == null ? Intervall.fraOgMed(periode.getFomDato()) : Intervall.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato());
    }

    private static AktivitetDto mapTilretteleggingArbeidsforhold(TilretteleggingArbeidsforhold tilretteleggingArbeidsforhold) {
        return new AktivitetDto(
            tilretteleggingArbeidsforhold.getArbeidsgiver().map(IAYMapperTilKalkulus::mapArbeidsgiver).orElse(null),
            mapArbeidsforholdRef(tilretteleggingArbeidsforhold.getInternArbeidsforholdRef()),
            KodeverkTilKalkulusMapper.mapUttakArbeidType(tilretteleggingArbeidsforhold.getUttakArbeidType())
        );
    }
}
