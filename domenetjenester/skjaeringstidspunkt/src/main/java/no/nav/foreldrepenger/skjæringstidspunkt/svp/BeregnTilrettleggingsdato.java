package no.nav.foreldrepenger.skjæringstidspunkt.svp;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingFOM;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.TilretteleggingType;

class BeregnTilrettleggingsdato {

    static LocalDate beregnFraTilrettelegging(SvpTilretteleggingEntitet tilrettelegging) {
        var jordmorsdato = tilrettelegging.getBehovForTilretteleggingFom();
        var helTilrettelegging = tilrettelegging.getTilretteleggingFOMListe().stream()
            .filter(tl -> tl.getType().equals(TilretteleggingType.HEL_TILRETTELEGGING))
            .map(TilretteleggingFOM::getFomDato)
            .min(LocalDate::compareTo);
        var delvisTilrettelegging = tilrettelegging.getTilretteleggingFOMListe().stream()
            .filter(tl -> tl.getType().equals(TilretteleggingType.DELVIS_TILRETTELEGGING))
            .map(TilretteleggingFOM::getFomDato)
            .min(LocalDate::compareTo);
        var slutteArbeid = tilrettelegging.getTilretteleggingFOMListe().stream()
            .filter(tl -> tl.getType().equals(TilretteleggingType.INGEN_TILRETTELEGGING))
            .map(TilretteleggingFOM::getFomDato)
            .min(LocalDate::compareTo);
        return beregn(jordmorsdato, helTilrettelegging, delvisTilrettelegging, slutteArbeid);
    }

    static LocalDate beregn(LocalDate jordmorsdato, Optional<LocalDate> helTilrettelegging, Optional<LocalDate> delvisTilrettelegging, Optional<LocalDate> slutteArbeid) {

        if (helTilrettelegging.isPresent() && delvisTilrettelegging.isPresent() && slutteArbeid.isPresent()) {
            if (helTilrettelegging.get().isBefore(slutteArbeid.get())
                && helTilrettelegging.get().isBefore(delvisTilrettelegging.get())
                && !helTilrettelegging.get().isEqual(jordmorsdato)) {
                return jordmorsdato;
            }
            return ikkeFørJordmorsdato(jordmorsdato, tidligsteDato(delvisTilrettelegging.get(), slutteArbeid.get()));
        }

        if (helTilrettelegging.isEmpty() && delvisTilrettelegging.isPresent() && slutteArbeid.isPresent()) {
            if (slutteArbeid.get().isBefore(delvisTilrettelegging.get())) {
                return ikkeFørJordmorsdato(jordmorsdato, slutteArbeid.get());
            }
            return jordmorsdato;
        }

        if (helTilrettelegging.isEmpty() && delvisTilrettelegging.isPresent()) {
            return jordmorsdato;
        }

        if (helTilrettelegging.isEmpty() && slutteArbeid.isPresent()) {
            return ikkeFørJordmorsdato(jordmorsdato, slutteArbeid.get());
        }

        if (helTilrettelegging.isPresent() && slutteArbeid.isPresent()) {
            if (helTilrettelegging.get().isBefore(slutteArbeid.get()) && !helTilrettelegging.get().isEqual(jordmorsdato)) {
                return jordmorsdato;
            }
            return ikkeFørJordmorsdato(jordmorsdato, slutteArbeid.get());
        }

        if (helTilrettelegging.isPresent() && delvisTilrettelegging.isPresent()) {
            if (helTilrettelegging.get().isBefore(delvisTilrettelegging.get()) && !helTilrettelegging.get().isEqual(jordmorsdato)) {
                return jordmorsdato;
            }
            return ikkeFørJordmorsdato(jordmorsdato, delvisTilrettelegging.get());
        }

        return jordmorsdato;
    }

    private static LocalDate tidligsteDato(LocalDate dato1, LocalDate dato2) {
        if (dato1.isBefore(dato2)) {
            return dato1;
        }
        return dato2;
    }

    private static LocalDate ikkeFørJordmorsdato(LocalDate jordmorsdato, LocalDate dato) {
        if (dato.isBefore(jordmorsdato)) {
            return jordmorsdato;
        }
        return dato;
    }
}
