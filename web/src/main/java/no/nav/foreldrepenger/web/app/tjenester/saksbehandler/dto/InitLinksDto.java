package no.nav.foreldrepenger.web.app.tjenester.saksbehandler.dto;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.tilganger.InnloggetNavAnsattDto;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;

public record InitLinksDto(InnloggetNavAnsattDto innloggetBruker,
                           List<OrganisasjonsEnhet> behandlendeEnheter,
                           List<ResourceLink> links,
                           List<ResourceLink> sakLinks) {
}
