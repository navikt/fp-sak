package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.fakta;

import java.math.BigDecimal;
import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.SamtidigUttaksprosent;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;

public record FaktaUttakPeriodeDto(LocalDate fom, LocalDate tom, UttakPeriodeType uttakPeriodeType, UtsettelseÅrsak utsettelseÅrsak,
                                   OverføringÅrsak overføringÅrsak, OppholdÅrsak oppholdÅrsak, BigDecimal arbeidstidsprosent,
                                   ArbeidsforholdDto arbeidsforhold, SamtidigUttaksprosent samtidigUttaksprosent, boolean flerbarnsdager,
                                   MorsAktivitet morsAktivitet, FordelingPeriodeKilde periodeKilde) {
}
