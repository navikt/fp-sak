package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonIdentMedDiskresjonskode;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

@ApplicationScoped
public class PersonopplysningDtoPersonIdentTjeneste {
    private PersoninfoAdapter personinfoAdapter;

    public PersonopplysningDtoPersonIdentTjeneste() {
    }

    @Inject
    public PersonopplysningDtoPersonIdentTjeneste(PersoninfoAdapter personinfoAdapter) {
        this.personinfoAdapter = personinfoAdapter;
    }


    public void oppdaterMedPersonIdent(PersonIdentDto dto) {
        // memoriser oppslagsfunksjoner - unngår repeterende tjeneste kall eksternt
        Function<AktørId, Optional<PersonIdentMedDiskresjonskode>> piDiskresjonFinder = memoize((aktørId) -> personinfoAdapter.hentPersonIdentMedDiskresjonskode(aktørId));

        // Sett fødselsnummer og diskresjonskodepå personopplysning for alle
        // behandlinger. Fødselsnummer og diskresjonskode lagres ikke i basen og må derfor hentes fra
        // TPS/IdentRepository// for å vises i GUI.
        if (dto.getAktoerId() != null) {
            dto.setFnr(findFnr(dto.getAktoerId(), piDiskresjonFinder));
            dto.setDiskresjonskode(findKode(dto.getAktoerId(), piDiskresjonFinder));
        }
    }

    public void oppdaterMedPersonIdent(PersonoversiktDto dto) {
        // memoriser oppslagsfunksjoner - unngår repeterende tjeneste kall eksternt
        // Sett fødselsnummer og diskresjonskodepå personopplysning for alle
        // behandlinger. Fødselsnummer og diskresjonskode lagres ikke i basen og må derfor hentes fra
        // TPS/IdentRepository// for å vises i GUI.
        var alle = new ArrayList<>(List.of(dto.getBruker()));
        Optional.ofNullable(dto.getAnnenPart()).ifPresent(alle::add);
        alle.addAll(dto.getBarn());
        alle.forEach(this::oppdaterMedPersonIdent);
    }

    private Diskresjonskode findKode(AktørId aktørId, Function<AktørId, Optional<PersonIdentMedDiskresjonskode>> piDiskresjonFinder) {
        return aktørId == null ? null : piDiskresjonFinder.apply(aktørId).map(PersonIdentMedDiskresjonskode::diskresjonskode).orElse(Diskresjonskode.UDEFINERT);
    }

    private String findFnr(AktørId aktørId, Function<AktørId, Optional<PersonIdentMedDiskresjonskode>> piDiskresjonFinder) {
        return aktørId == null ? null : piDiskresjonFinder.apply(aktørId).map(PersonIdentMedDiskresjonskode::personIdent).map(PersonIdent::getIdent).orElse(null);

    }

    /** Lag en funksjon som husker resultat av tidligere input. Nyttig for repeterende lookups */
    static <I, O> Function<I, O> memoize(Function<I, O> f) {
        ConcurrentMap<I, O> lookup = new ConcurrentHashMap<>();
        return input -> input == null ? null : lookup.computeIfAbsent(input, f);
    }
}
