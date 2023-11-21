package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.kontrakter.fordel.SaksnummerDto;

public record SakInfoDto(@NotNull @Valid SaksnummerDto saksnummer,
                         @NotNull @Valid FagsakYtelseTypeDto ytelseType,
                         @NotNull LocalDate opprettetDato,
                         @NotNull @Valid FagsakStatusDto status,
                         @Valid FamiliehendelseInfoDto familiehendelseInfoDto,
                         LocalDate førsteUttaksdato) {
    public record FamiliehendelseInfoDto(LocalDate familiehendelseDato,
                                         @Valid FamilieHendelseTypeDto familihendelseType) {
    }
    public enum FagsakYtelseTypeDto {
        ENGANGSTØNAD,
        FORELDREPENGER,
        SVANGERSKAPSPENGER
    }

    public enum FagsakStatusDto {
        UNDER_BEHANDLING,
        LØPENDE,
        AVSLUTTET
    }

    public enum FamilieHendelseTypeDto {
        FØDSEL,
        TERMIN,
        ADOPSJON,
        OMSORG
    }
}

