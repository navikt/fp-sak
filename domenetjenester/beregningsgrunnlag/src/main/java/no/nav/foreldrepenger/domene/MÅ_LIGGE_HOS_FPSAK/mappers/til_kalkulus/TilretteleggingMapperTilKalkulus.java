package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus;

import static no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.til_kalkulus.IAYMapperTilKalkulus.mapArbeidsforholdRef;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulator.modell.svp.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulator.modell.svp.SvpTilretteleggingDto;
import no.nav.folketrygdloven.kalkulator.modell.svp.TilretteleggingArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulator.modell.svp.TilretteleggingMedUtbelingsgradDto;
import no.nav.folketrygdloven.kalkulator.modell.uttak.UttakArbeidType;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.PeriodeMedUtbetalingsgrad;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.TilretteleggingArbeidsforhold;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.TilretteleggingMedUtbelingsgrad;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class TilretteleggingMapperTilKalkulus {

    public static List<TilretteleggingMedUtbelingsgradDto> mapTilretteleggingerMedUtbetalingsgrad(List<TilretteleggingMedUtbelingsgrad> tilretteleggingMedUtbelingsgrad) {
        return tilretteleggingMedUtbelingsgrad.stream().
            map(TilretteleggingMapperTilKalkulus::mapTilretteleggingMedUtbetalingsgrad)
            .collect(Collectors.toList());
    }

    private static TilretteleggingMedUtbelingsgradDto mapTilretteleggingMedUtbetalingsgrad(TilretteleggingMedUtbelingsgrad tilretteleggingMedUtbelingsgrad) {
        return new TilretteleggingMedUtbelingsgradDto(
            mapTilretteleggingArbeidsforhold(tilretteleggingMedUtbelingsgrad.getTilretteleggingArbeidsforhold()),
            mapPerioderMedUtbetalingsgrad(tilretteleggingMedUtbelingsgrad.getPeriodeMedUtbetalingsgrad())
        );
    }

    private static List<PeriodeMedUtbetalingsgradDto> mapPerioderMedUtbetalingsgrad(List<PeriodeMedUtbetalingsgrad> periodeMedUtbetalingsgrad) {
        return periodeMedUtbetalingsgrad.stream()
            .map(TilretteleggingMapperTilKalkulus::mapPeriodeMedUtbetalingsgrad)
            .collect(Collectors.toList());
    }

    private static PeriodeMedUtbetalingsgradDto mapPeriodeMedUtbetalingsgrad(PeriodeMedUtbetalingsgrad periodeMedUtbetalingsgrad) {
        return new PeriodeMedUtbetalingsgradDto(mapDatoIntervall(periodeMedUtbetalingsgrad.getPeriode()), periodeMedUtbetalingsgrad.getUtbetalingsgrad());
    }

    private static Intervall mapDatoIntervall(DatoIntervallEntitet periode) {
        return periode.getTomDato() == null ? Intervall.fraOgMed(periode.getFomDato()) : Intervall.fraOgMedTilOgMed(periode.getFomDato(), periode.getTomDato());
    }

    private static TilretteleggingArbeidsforholdDto mapTilretteleggingArbeidsforhold(TilretteleggingArbeidsforhold tilretteleggingArbeidsforhold) {
        return new TilretteleggingArbeidsforholdDto(
            tilretteleggingArbeidsforhold.getArbeidsgiver().map(IAYMapperTilKalkulus::mapArbeidsgiver).orElse(null),
            mapArbeidsforholdRef(tilretteleggingArbeidsforhold.getInternArbeidsforholdRef()),
            mapUttakArbeidType(tilretteleggingArbeidsforhold.getUttakArbeidType())
        );
    }

    private static UttakArbeidType mapUttakArbeidType(no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType uttakArbeidType) {
        return UttakArbeidType.fraKode(uttakArbeidType.getKode());
    }

    public static List<SvpTilretteleggingDto> mapTilrettelegginger(List<SvpTilretteleggingEntitet> aktuelleArbeidsgivereMedTilrettelegginger) {
        return aktuelleArbeidsgivereMedTilrettelegginger.stream()
            .map(TilretteleggingMapperTilKalkulus::mapSvpTilrettelegging)
            .collect(Collectors.toList());
    }

    private static SvpTilretteleggingDto mapSvpTilrettelegging(SvpTilretteleggingEntitet svpTilretteleggingEntitet) {
        return new SvpTilretteleggingDto.Builder()
            .medArbeidsgiver(svpTilretteleggingEntitet.getArbeidsgiver().map((Arbeidsgiver arbeidsgiver) -> IAYMapperTilKalkulus.mapArbeidsgiver(arbeidsgiver)).orElse(null))
            .medInternArbeidsforholdRef(svpTilretteleggingEntitet.getInternArbeidsforholdRef().map(IAYMapperTilKalkulus::mapArbeidsforholdRef).orElse(null))
            .medSkalBrukes(svpTilretteleggingEntitet.getSkalBrukes())
            .medHarSøktDelvisTilrettelegging(harSøktDelvisTilrettelegging(svpTilretteleggingEntitet.getTilretteleggingFOMListe()))
            .build();
    }

    private static boolean harSøktDelvisTilrettelegging(List<TilretteleggingFOM> tilretteleggingFOMListe) {
        return tilretteleggingFOMListe.stream()
            .anyMatch(tilretteleggingFOM -> no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType.DELVIS_TILRETTELEGGING.equals(tilretteleggingFOM.getType()));
    }

}
