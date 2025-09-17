package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import java.util.List;

import no.nav.folketrygdloven.kalkulus.beregning.v1.AktivitetDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedUtbetalingsgradDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.UtbetalingsgradPrAktivitetDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.felles.v1.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.PeriodeMedUtbetalingsgrad;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.TilretteleggingArbeidsforhold;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.TilretteleggingMedUtbelingsgrad;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;

public class MapTilrettelegginger {

    private MapTilrettelegginger() {
    }

    public static List<UtbetalingsgradPrAktivitetDto> mapTilretteleggingerMedUtbetalingsgrad(List<TilretteleggingMedUtbelingsgrad> tilretteleggingMedUtbelingsgrad) {
        return tilretteleggingMedUtbelingsgrad.stream().
            map(MapTilrettelegginger::mapTilretteleggingMedUtbetalingsgrad)
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
            .map(MapTilrettelegginger::mapPeriodeMedUtbetalingsgrad)
            .toList();
    }

    private static PeriodeMedUtbetalingsgradDto mapPeriodeMedUtbetalingsgrad(PeriodeMedUtbetalingsgrad periodeMedUtbetalingsgrad) {
        return new PeriodeMedUtbetalingsgradDto(mapDatoIntervall(periodeMedUtbetalingsgrad.getPeriode()), Utbetalingsgrad.fra(periodeMedUtbetalingsgrad.getUtbetalingsgrad()));
    }

    private static Periode mapDatoIntervall(DatoIntervallEntitet periode) {
        return new Periode(periode.getFomDato(), periode.getTomDato());
    }

    private static AktivitetDto mapTilretteleggingArbeidsforhold(TilretteleggingArbeidsforhold tilretteleggingArbeidsforhold) {
        return new AktivitetDto(
            mapTilAktør(tilretteleggingArbeidsforhold.getArbeidsgiver().orElse(null)),
            mapReferanse(tilretteleggingArbeidsforhold.getInternArbeidsforholdRef()),
            KodeverkTilKalkulusMapper.mapUttakArbeidType(tilretteleggingArbeidsforhold.getUttakArbeidType())
        );
    }

    private static Aktør mapTilAktør(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        return arbeidsgiver.getErVirksomhet() ? new Organisasjon(arbeidsgiver.getOrgnr()) : new AktørIdPersonident(arbeidsgiver.getAktørId().getId());
    }

    private static InternArbeidsforholdRefDto mapReferanse(InternArbeidsforholdRef arbeidsforholdRef) {
        return arbeidsforholdRef.getReferanse() == null ? null : new no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto(arbeidsforholdRef.getReferanse());
    }


}
