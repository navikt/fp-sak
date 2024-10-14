package no.nav.foreldrepenger.behandlingslager.aktør.historikk;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import no.nav.vedtak.konfig.Tid;

public record Gyldighetsperiode(LocalDate fom, LocalDate tom) {

    public Gyldighetsperiode {
        Objects.requireNonNull(fom, "fom");
        Objects.requireNonNull(tom, "tom");
    }

    public static Gyldighetsperiode innenfor(LocalDate fom, LocalDate tom) {
        return new Gyldighetsperiode(Optional.ofNullable(fom).orElse(Tid.TIDENES_BEGYNNELSE), Optional.ofNullable(tom).orElse(Tid.TIDENES_ENDE));
    }

    public static Gyldighetsperiode fraDates(Date dateFom, Date dateTom) {
        var gyldigFra = dateFom == null ? null : LocalDateTime.ofInstant(dateFom.toInstant(), ZoneId.systemDefault()).toLocalDate();
        var gyldigTil = dateTom == null ? null : LocalDateTime.ofInstant(dateTom.toInstant(), ZoneId.systemDefault()).toLocalDate();
        return Gyldighetsperiode.innenfor(gyldigFra, gyldigTil);
    }

    public boolean overlapper(Gyldighetsperiode other) {
        var fomBeforeOrEqual = this.fom().isBefore(other.tom()) || this.fom().isEqual(other.tom());
        var tomAfterOrEqual = this.tom().isAfter(other.fom()) || this.tom().isEqual(other.fom());
        return fomBeforeOrEqual && tomAfterOrEqual;
    }

    // Returnerer periode med tom-dato satt til dagen før neste periode hvis finnes, ellers uendret
    public static Gyldighetsperiode justerForSenere(List<Gyldighetsperiode> tidsserie, Gyldighetsperiode periode) {
        if (tidsserie.stream().noneMatch(p -> p.fom().isAfter(periode.fom()) && p.fom().isBefore(periode.tom())))
            return periode;
        return tidsserie.stream()
            .map(Gyldighetsperiode::fom)
            .filter(d -> d.isAfter(periode.fom()))
            .min(Comparator.naturalOrder())
            .map(d -> Gyldighetsperiode.innenfor(periode.fom(), d.minusDays(1))).orElse(periode);
    }

}
