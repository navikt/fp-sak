package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import java.math.BigDecimal;
import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;

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
                                   @ValidKodeverk FordelingPeriodeKilde periodeKilde) {
}
