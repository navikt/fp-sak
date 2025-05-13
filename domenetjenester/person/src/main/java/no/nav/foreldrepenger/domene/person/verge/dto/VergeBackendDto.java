package no.nav.foreldrepenger.domene.person.verge.dto;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeType;

/**
 * Dto for verge som brukes av andre applikasjoner enn frontend - dvs formidling og tilbakekreving.
 * Legg merke til aktørId i stedet for fødselsnummer (som frontend bruker).
 * Ikke endre "aktoerId" uten expand/contract med konsumenter.
 */
public record VergeBackendDto(String aktoerId, String navn, String organisasjonsnummer,
                              LocalDate gyldigFom, LocalDate gyldigTom, VergeType vergeType) {
}
