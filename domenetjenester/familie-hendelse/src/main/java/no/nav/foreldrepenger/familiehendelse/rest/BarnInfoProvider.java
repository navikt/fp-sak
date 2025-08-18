package no.nav.foreldrepenger.familiehendelse.rest;

import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.DokumentertBarnDto;

import java.util.List;

public interface BarnInfoProvider {
    List<DokumentertBarnDto> getBarn();
}
