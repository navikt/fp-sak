package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;

public record SakHendelseDto(@NotNull FamilieHendelseType hendelseType, LocalDate hendelseDato, @NotNull Integer antallBarn, @NotNull boolean dødfødsel) {

}
