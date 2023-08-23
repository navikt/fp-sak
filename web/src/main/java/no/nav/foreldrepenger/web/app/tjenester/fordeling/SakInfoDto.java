package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import no.nav.foreldrepenger.kontrakter.fordel.SaksnummerDto;

import java.time.LocalDate;

public record SakInfoDto(SaksnummerDto saksnummer, FagsakYtelseTypeDto ytelseType, LocalDate opprettetDato, FagsakStatusDto status, FamiliehendelseInfoDto familiehendelseInfoDto, LocalDate førsteUttaksdato) {
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

    public record FamiliehendelseInfoDto(LocalDate familiehendelseDato, FamilieHendelseTypeDto familihendelseType) {}
}

