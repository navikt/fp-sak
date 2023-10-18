package no.nav.foreldrepenger.web.app.tjenester.formidling.tilkjentytelse;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.kontrakter.fpsak.tilkjentytelse.TilkjentYtelseDagytelseDto;
import no.nav.foreldrepenger.kontrakter.fpsak.tilkjentytelse.TilkjentYtelseEngangsstønadDto;

public final class TilkjentYtelseFormidlingDtoTjeneste {

    private TilkjentYtelseFormidlingDtoTjeneste() {
        // Skjuler default konstruktør
    }

    public static TilkjentYtelseDagytelseDto mapDagytelse(BeregningsresultatEntitet bgRes) {
        var perioder = bgRes.getBeregningsresultatPerioder()
            .stream()
            .map(TilkjentYtelseFormidlingDtoTjeneste::mapPeriode)
            .toList();
        return new TilkjentYtelseDagytelseDto(perioder);
    }

    private static TilkjentYtelseDagytelseDto.TilkjentYtelsePeriodeDto mapPeriode(BeregningsresultatPeriode resultatPeriode) {
        var andeler = resultatPeriode.getBeregningsresultatAndelList()
            .stream()
            .map(TilkjentYtelseFormidlingDtoTjeneste::mapAndel)
            .toList();
        return new TilkjentYtelseDagytelseDto.TilkjentYtelsePeriodeDto(resultatPeriode.getBeregningsresultatPeriodeFom(),
            resultatPeriode.getBeregningsresultatPeriodeTom(), resultatPeriode.getDagsats(), andeler);
    }

    private static TilkjentYtelseDagytelseDto.TilkjentYtelseAndelDto mapAndel(BeregningsresultatAndel andel) {
        Integer refusjon = andel.erBrukerMottaker() ? null : andel.getDagsats();
        Integer tilSøker = andel.erBrukerMottaker() ? andel.getDagsats() : null;
        var arbeidsgiverIdent = andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator).orElse(null);
        var referanse = andel.getArbeidsforholdRef().getReferanse();
        return new TilkjentYtelseDagytelseDto.TilkjentYtelseAndelDto(arbeidsgiverIdent, refusjon, tilSøker,
            mapAktivitetstatus(andel.getAktivitetStatus()), referanse, andel.getStillingsprosent());
    }

    private static TilkjentYtelseDagytelseDto.Aktivitetstatus mapAktivitetstatus(no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus aktivitetStatus) {
        return switch (aktivitetStatus) {
            case ARBEIDSAVKLARINGSPENGER -> TilkjentYtelseDagytelseDto.Aktivitetstatus.ARBEIDSAVKLARINGSPENGER;
            case ARBEIDSTAKER -> TilkjentYtelseDagytelseDto.Aktivitetstatus.ARBEIDSTAKER;
            case DAGPENGER -> TilkjentYtelseDagytelseDto.Aktivitetstatus.DAGPENGER;
            case FRILANSER -> TilkjentYtelseDagytelseDto.Aktivitetstatus.FRILANSER;
            case MILITÆR_ELLER_SIVIL -> TilkjentYtelseDagytelseDto.Aktivitetstatus.MILITÆR_ELLER_SIVIL;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> TilkjentYtelseDagytelseDto.Aktivitetstatus.SELVSTENDIG_NÆRINGSDRIVENDE;
            case BRUKERS_ANDEL -> TilkjentYtelseDagytelseDto.Aktivitetstatus.BRUKERS_ANDEL;
            case KUN_YTELSE -> TilkjentYtelseDagytelseDto.Aktivitetstatus.KUN_YTELSE;
            case TTLSTØTENDE_YTELSE -> TilkjentYtelseDagytelseDto.Aktivitetstatus.TTLSTØTENDE_YTELSE;
            case VENTELØNN_VARTPENGER -> TilkjentYtelseDagytelseDto.Aktivitetstatus.VENTELØNN_VARTPENGER;
            case UDEFINERT -> TilkjentYtelseDagytelseDto.Aktivitetstatus.UDEFINERT;
            case KOMBINERT_AT_SN, KOMBINERT_AT_FL_SN, // Burde ikke oppstå for aktivitetstatuser på andeler
                KOMBINERT_AT_FL, KOMBINERT_FL_SN -> throw new IllegalArgumentException("Ugyldig aktivitetstatus " + aktivitetStatus);
        };
    }

    public static TilkjentYtelseEngangsstønadDto mapEngangsstønad(LegacyESBeregning legacyESBeregning) {
        return new TilkjentYtelseEngangsstønadDto(legacyESBeregning.getBeregnetTilkjentYtelse());
    }
}
