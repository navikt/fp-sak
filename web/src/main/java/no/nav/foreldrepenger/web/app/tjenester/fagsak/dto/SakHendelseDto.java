package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;

public record SakHendelseDto(FamilieHendelseType hendelseType, LocalDate hendelseDato, Integer antallBarn, boolean dødfødsel) {

}
