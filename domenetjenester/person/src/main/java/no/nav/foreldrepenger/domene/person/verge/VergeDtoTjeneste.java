package no.nav.foreldrepenger.domene.person.verge;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.Aktør;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoArbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeOrganisasjonEntitet;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeDto;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

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
        return vergeAggregat.getVerge().map(this::mapTilVergeDto);
    }

    private VergeDto mapTilVergeDto(VergeEntitet verge) {
        if (verge.getVergeOrganisasjon().isPresent()) {
            return VergeDto.organisasjon(verge.getVergeType(), verge.getGyldigFom(), verge.getGyldigTom(),
                verge.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getNavn).orElse(null),
                verge.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getOrganisasjonsnummer).orElse(null));
        } else {
            var vergeAktør = verge.getBruker().map(Aktør::getAktørId).flatMap(personinfoAdapter::hentBrukerVergeForAktør);
            return VergeDto.person(verge.getVergeType(), verge.getGyldigFom(), verge.getGyldigTom(),
                vergeAktør.map(PersoninfoArbeidsgiver::navn).orElse(null),
                vergeAktør.map(PersoninfoArbeidsgiver::personIdent).map(PersonIdent::getIdent).orElse(null));
        }
    }
}
