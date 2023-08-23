package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;

import java.time.LocalDate;

public record SakHendelseDto(FamilieHendelseType hendelseType, LocalDate hendelseDato, Integer antallBarn, boolean dødfødsel) {

}
