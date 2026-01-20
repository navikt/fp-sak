package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.PeriodeMedUtbetalingsgrad;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.TilretteleggingArbeidsforhold;
import no.nav.foreldrepenger.domene.arbeidsforhold.svp.TilretteleggingMedUtbelingsgrad;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.input.svangerskapspenger.AktivitetDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.input.svangerskapspenger.PeriodeMedUtbetalingsgradDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.input.svangerskapspenger.UtbetalingsgradPrAktivitetDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.Aktør;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.AktørIdPersonident;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.InternArbeidsforholdRefDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.Organisasjon;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.Periode;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.Utbetalingsgrad;

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
        return arbeidsforholdRef.getReferanse() == null ? null : new InternArbeidsforholdRefDto(arbeidsforholdRef.getReferanse());
    }


}
