package no.nav.foreldrepenger.domene.person.verge;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeOrganisasjonEntitet;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
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

    public Optional<VergeDto> lagVergeDto(Optional<VergeAggregat> vergeAggregat) {
        if (vergeAggregat.isPresent() && vergeAggregat.get().getVerge().isPresent()) {
            VergeAggregat aggregat = vergeAggregat.get();
            VergeEntitet verge = aggregat.getVerge().get();
            VergeDto dto = new VergeDto();

            dto.setGyldigFom(verge.getGyldigFom());
            dto.setGyldigTom(verge.getGyldigTom());
            dto.setVergeType(verge.getVergeType());

            if (verge.getVergeOrganisasjon().isPresent()) {
                verge.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getOrganisasjonsnummer).ifPresent(dto::setOrganisasjonsnummer);
                verge.getVergeOrganisasjon().map(VergeOrganisasjonEntitet::getNavn).ifPresent(dto::setNavn);
            } else if (aggregat.getAktørId().isPresent()){
                setPersonIdent(aggregat.getAktørId().get(), dto);
            }

            return Optional.of(dto);
        } else {
            return Optional.empty();
        }
    }

    private void setPersonIdent(AktørId aktørId, VergeDto dto) {
        personinfoAdapter.hentBrukerArbeidsgiverForAktør(aktørId).ifPresent(pib -> {
            dto.setNavn(pib.getNavn());
            dto.setFnr(pib.getPersonIdent().getIdent());
        });
    }
}
