package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public record Uføreperiode(LocalDate uforetidspunkt,
                           LocalDate virkningsdato,
                           LocalDate ufgFom,
                           LocalDate ufgTom) {
    public Uføreperiode(UforeperiodeDto dto) {
        this(tilLocalDate(dto.uforetidspunkt()), tilLocalDate(dto.virk()),
            tilLocalDate(dto.ufgFom()), tilLocalDate(dto.ufgTom()));
    }

    private static LocalDate tilLocalDate(Date dato) {
        return dato == null ? null : LocalDateTime.ofInstant(dato.toInstant(), ZoneId.systemDefault()).toLocalDate();
    }
}
