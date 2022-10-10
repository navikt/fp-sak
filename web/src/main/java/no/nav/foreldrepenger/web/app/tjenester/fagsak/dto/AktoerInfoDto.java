package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.util.List;

public record AktoerInfoDto(@Deprecated(forRemoval = true) String aktoerId, String aktørId, PersonDto person, List<FagsakSøkDto> fagsaker) {
}
