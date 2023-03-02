package no.nav.foreldrepenger.ytelse.beregning.regelmodell.uttakresultat;

import java.time.LocalDate;
import java.util.List;

public record UttakResultatPeriode(LocalDate fom, LocalDate tom, List<UttakAktivitet> uttakAktiviteter, boolean erOppholdsPeriode) {
}
