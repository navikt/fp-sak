package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoVisning;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.web.app.util.StringUtils;

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
        Function<AktørId, Optional<PersoninfoVisning>> piDiskresjonFinder = memoize((aktørId) -> personinfoAdapter.hentPersoninfoForVisning(aktørId));

        // Sett fødselsnummer og diskresjonskodepå personopplysning for alle
        // behandlinger. Fødselsnummer og diskresjonskode lagres ikke i basen og må derfor hentes fra
        // TPS/IdentRepository// for å vises i GUI.
        if (dto.getAktoerId() != null) {
            dto.setFnr(findFnr(dto.getAktoerId(), piDiskresjonFinder));
            dto.setDiskresjonskode(findKode(dto.getAktoerId(), piDiskresjonFinder));
            if (dto.getNavn() == null) dto.setNavn(findNavn(dto.getAktoerId(), piDiskresjonFinder));
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

    private Diskresjonskode findKode(AktørId aktørId, Function<AktørId, Optional<PersoninfoVisning>> piDiskresjonFinder) {
        return aktørId == null ? null : piDiskresjonFinder.apply(aktørId).map(PersoninfoVisning::diskresjonskode).orElse(Diskresjonskode.UDEFINERT);
    }

    private String findFnr(AktørId aktørId, Function<AktørId, Optional<PersoninfoVisning>> piDiskresjonFinder) {
        return aktørId == null ? null : piDiskresjonFinder.apply(aktørId).map(PersoninfoVisning::personIdent).map(PersonIdent::getIdent).orElse(null);
    }

    private String findNavn(AktørId aktørId, Function<AktørId, Optional<PersoninfoVisning>> piDiskresjonFinder) {
        return aktørId == null ? "Navnløs" : piDiskresjonFinder.apply(aktørId).map(PersoninfoVisning::navn).map(StringUtils::formaterMedStoreOgSmåBokstaver).orElse("Navnløs");
    }

    /** Lag en funksjon som husker resultat av tidligere input. Nyttig for repeterende lookups */
    static <I, O> Function<I, O> memoize(Function<I, O> f) {
        ConcurrentMap<I, O> lookup = new ConcurrentHashMap<>();
        return input -> input == null ? null : lookup.computeIfAbsent(input, f);
    }
}
