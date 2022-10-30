package no.nav.foreldrepenger.web.app.tjenester.fagsak.dto;

import java.util.List;

public record AktoerInfoDto(String aktørId, PersonDto person, List<FagsakSøkDto> fagsaker) {
}
