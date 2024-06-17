package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import java.time.LocalDate;

public record Uføreperiode(LocalDate uforetidspunkt, LocalDate virkningsdato) {

    public Uføreperiode(HarUføreGrad dto) {
        this(dto.datoUfor(), dto.virkDato());
    }
}
