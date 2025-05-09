package no.nav.foreldrepenger.domene.person.verge;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeOrganisasjonEntitet;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeBackendDto;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeDto;
import no.nav.foreldrepenger.domene.typer.AktørId;

@ApplicationScoped
public class VergeDtoTjeneste {

    private PersoninfoAdapter personinfoAdapter;

    VergeDtoTjeneste() {
    }

    @Inject
    public VergeDtoTjeneste(PersoninfoAdapter personinfoAdapter) {
        this.personinfoAdapter = personinfoAdapter;
    }

    public Optional<VergeDto> lagVergeDto(VergeAggregat vergeAggregat) {
        if (vergeAggregat == null)
            return Optional.empty();
        return vergeAggregat.getVerge().map(v -> mapTilVergeDto(vergeAggregat, v));
    }

    private VergeDto mapTilVergeDto(VergeAggregat vergeAggregat, VergeEntitet verge) {
        var dto = new VergeDto();

        dto.setGyldigFom(verge.getGyldigFom());
        dto.setGyldigTom(verge.getGyldigTom());
        dto.setVergeType(verge.getVergeType());

        if (verge.getVergeOrganisasjon().isPresent()) {
            verge.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getOrganisasjonsnummer).ifPresent(dto::setOrganisasjonsnummer);
            verge.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getNavn).ifPresent(dto::setNavn);
        } else {
            vergeAggregat.getAktørId().ifPresent(a -> setPersonIdent(a, dto));
        }

        return dto;
    }

    public Optional<VergeBackendDto> lagVergeBackendDto(VergeAggregat vergeAggregat) {
        return vergeAggregat.getVerge().map(v -> mapTilBackendDto(vergeAggregat, v));
    }

    private VergeBackendDto mapTilBackendDto(VergeAggregat vergeAggregat, VergeEntitet verge) {
        return new VergeBackendDto(vergeAggregat.getAktørId().map(AktørId::getId).orElse(null),
            verge.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getNavn).orElse(null),
            verge.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getOrganisasjonsnummer).orElse(null),
            verge.getGyldigFom(), verge.getGyldigTom(), verge.getVergeType());
    }

    private void setPersonIdent(AktørId aktørId, VergeDto dto) {
        personinfoAdapter.hentBrukerVergeForAktør(aktørId).ifPresent(pib -> {
            dto.setNavn(pib.getNavn());
            dto.setFnr(pib.getPersonIdent().getIdent());
        });
    }
}
