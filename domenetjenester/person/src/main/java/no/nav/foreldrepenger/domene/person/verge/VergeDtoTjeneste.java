package no.nav.foreldrepenger.domene.person.verge;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeEntitet;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.person.verge.dto.VergeDto;
import no.nav.foreldrepenger.domene.typer.AktørId;
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

    public Optional<VergeDto> lagVergeDto(Optional<VergeAggregat> vergeAggregat) {
        if (vergeAggregat.isPresent() && vergeAggregat.get().getVerge().isPresent()) {
            VergeAggregat aggregat = vergeAggregat.get();
            VergeEntitet verge = aggregat.getVerge().get();
            VergeDto dto = new VergeDto();

            dto.setGyldigFom(verge.getGyldigFom());
            dto.setGyldigTom(verge.getGyldigTom());
            dto.setVergeType(verge.getVergeType());

            if (verge.getVergeOrganisasjon().isPresent()) {
                dto.setOrganisasjonsnummer(verge.getVergeOrganisasjon().get().getOrganisasjonsnummer());
                dto.setNavn(verge.getVergeOrganisasjon().get().getNavn());
            } else if (aggregat.getAktørId().isPresent()){
                setPersonIdent(aggregat.getAktørId().get(), dto);
            }

            return Optional.of(dto);
        } else {
            return Optional.empty();
        }
    }

    private void setPersonIdent(AktørId aktørId, VergeDto dto) {
        Optional<Personinfo> personinfoDto = personinfoAdapter.hentBrukerForAktør(aktørId);
        if (personinfoDto.isPresent()) {
            Personinfo personinfo = personinfoDto.get();
            String navn = personinfoDto.map(Personinfo::getNavn).orElse("Ukjent navn"); //$NON-NLS-1$
            dto.setNavn(navn);

            PersonIdent personIdent = personinfo.getPersonIdent();
            dto.setFnr(personIdent.getIdent());
        }
    }
}
