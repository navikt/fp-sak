package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;
import no.nav.vedtak.util.InputValideringRegex;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FaktaUttakPeriodeDto(@NotNull LocalDate fom,
                                   @NotNull LocalDate tom,
                                   @ValidKodeverk UttakPeriodeType uttakPeriodeType,
                                   @ValidKodeverk UtsettelseÅrsak utsettelseÅrsak,
                                   @ValidKodeverk OverføringÅrsak overføringÅrsak,
                                   @ValidKodeverk OppholdÅrsak oppholdÅrsak,
                                   @Min(0) @Max(100) @Digits(integer = 3, fraction = 2) BigDecimal arbeidstidsprosent,
                                   @Valid ArbeidsforholdDto arbeidsforhold,
                                   @Valid SamtidigUttaksprosent samtidigUttaksprosent,
                                   boolean flerbarnsdager,
                                   @ValidKodeverk MorsAktivitet morsAktivitet,
                                   @ValidKodeverk FordelingPeriodeKilde periodeKilde,
                                   @Size(max = 4000) @Pattern(regexp = InputValideringRegex.FRITEKST) String begrunnelse) {

    @Override
    public String toString() {
        return "FaktaUttakPeriodeDto{" + "fom=" + fom + ", tom=" + tom + ", uttakPeriodeType=" + uttakPeriodeType + ", utsettelseÅrsak="
            + utsettelseÅrsak + ", overføringÅrsak=" + overføringÅrsak + ", oppholdÅrsak=" + oppholdÅrsak + ", arbeidstidsprosent="
            + arbeidstidsprosent + ", arbeidsforhold=" + arbeidsforhold + ", samtidigUttaksprosent=" + samtidigUttaksprosent + ", flerbarnsdager="
            + flerbarnsdager + ", morsAktivitet=" + morsAktivitet + ", periodeKilde=" + periodeKilde + '}';
    }
}
