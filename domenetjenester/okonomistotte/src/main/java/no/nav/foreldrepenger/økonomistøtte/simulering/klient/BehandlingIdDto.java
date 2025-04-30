package no.nav.foreldrepenger.økonomistøtte.simulering.klient;

import java.util.UUID;

public record BehandlingIdDto(Long behandlingId,
                              UUID behandlingUuid,
                              String saksnummer) { }
