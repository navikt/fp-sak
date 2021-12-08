package no.nav.foreldrepenger.domene.registerinnhenting.ufo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public record Uføreperiode(Integer uforegrad,
                           LocalDate uforetidspunkt,
                           LocalDate virkningsdato,
                           UføreType uføretype,
                           LocalDate uforetidspunktTom,
                           LocalDate ufgFom,
                           LocalDate ufgTom) {
    public Uføreperiode(UforeperiodeDto dto) {
        this(dto.uforegrad(),
            tilLocalDate(dto.uforetidspunkt()),
            tilLocalDate(dto.virk()),
            tilUføreType(dto.uforetype()),
            tilLocalDate(dto.uforetidspunktTom()),
            tilLocalDate(dto.ufgFom()),
            tilLocalDate(dto.ufgTom()));
    }

    private static LocalDate tilLocalDate(Date dato) {
        return dato == null ? null : LocalDateTime.ofInstant(dato.toInstant(), ZoneId.systemDefault()).toLocalDate();
    }
    private static UføreType tilUføreType(UforeTypeCtiDto dto) {
        if (dto == null || dto.code() == null) return UføreType.UDFEFINERT;
        return switch (dto.code()) {
            case UFORE -> UføreType.UFØRE;
            case UKJENT -> UføreType.UDFEFINERT;
            case UF_M_YRKE -> UføreType.UFØRE_MED_YRKESSKADE;
            case YRKE -> UføreType.YRKESSKADE;
            case VIRK_IKKE_UFOR -> UføreType.VIRKNINGSDATO_IKKE_UFØR;
        };
    }
}
