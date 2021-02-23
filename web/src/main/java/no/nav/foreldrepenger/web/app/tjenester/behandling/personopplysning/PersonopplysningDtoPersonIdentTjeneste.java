package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
public class PersonopplysningDtoPersonIdentTjeneste {
    private PersoninfoAdapter personinfoAdapter;

    public PersonopplysningDtoPersonIdentTjeneste() {
    }

    @Inject
    public PersonopplysningDtoPersonIdentTjeneste(PersoninfoAdapter personinfoAdapter) {
        this.personinfoAdapter = personinfoAdapter;
    }

    // oppdater med foedselsnr
    public void oppdaterMedPersonIdent(PersonopplysningDto personopplysningDto) {
        // memoriser oppslagsfunksjoner - unngår repeterende tjeneste kall eksternt
        Function<AktørId, Optional<Tuple<PersonIdent, Diskresjonskode>>> piDiskresjonFinder = memoize((aktørId) -> personinfoAdapter.hentPersonIdentMedDiskresjonskode(aktørId));

        // Sett fødselsnummer og diskresjonskodepå personopplysning for alle
        // behandlinger. Fødselsnummer og diskresjonskode lagres ikke i basen og må derfor hentes fra
        // TPS/IdentRepository// for å vises i GUI.
        if (personopplysningDto != null) {
            setFnrPaPersonopplysning(personopplysningDto, piDiskresjonFinder);
        }

    }

    public void oppdaterMedPersonIdent(PersonIdentDto dto) {
        // memoriser oppslagsfunksjoner - unngår repeterende tjeneste kall eksternt
        Function<AktørId, Optional<Tuple<PersonIdent, Diskresjonskode>>> piDiskresjonFinder = memoize((aktørId) -> personinfoAdapter.hentPersonIdentMedDiskresjonskode(aktørId));

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

    void setFnrPaPersonopplysning(PersonopplysningDto dto, Function<AktørId, Optional<Tuple<PersonIdent, Diskresjonskode>>> piDiskresjonFinder) {

        // Soker
        dto.setFnr(findFnr(dto.getAktoerId(), piDiskresjonFinder)); // forelder / soeker
        dto.setDiskresjonskode(findKode(dto.getAktoerId(), piDiskresjonFinder));

        // Medsoker
        if (dto.getAnnenPart() != null) {
            dto.getAnnenPart().setFnr(findFnr(dto.getAnnenPart().getAktoerId(), piDiskresjonFinder));
            dto.getAnnenPart().setDiskresjonskode(findKode(dto.getAnnenPart().getAktoerId(), piDiskresjonFinder));
            // Medsøkers barn
            if (!dto.getAnnenPart().getBarn().isEmpty()) {
                for (PersonopplysningDto dtoBarn : dto.getAnnenPart().getBarn()) {
                    dtoBarn.setFnr(findFnr(dtoBarn.getAktoerId(), piDiskresjonFinder));
                    dtoBarn.setDiskresjonskode(findKode(dtoBarn.getAktoerId(), piDiskresjonFinder));
                }
            }
        }

        // ektefelle
        if (dto.getEktefelle() != null) {
            dto.getEktefelle().setFnr(findFnr(dto.getEktefelle().getAktoerId(), piDiskresjonFinder));
            dto.getEktefelle().setDiskresjonskode(findKode(dto.getEktefelle().getAktoerId(), piDiskresjonFinder));
        }

        // Barn
        for (PersonopplysningDto dtoBarn : dto.getBarn()) {
            dtoBarn.setFnr(findFnr(dtoBarn.getAktoerId(), piDiskresjonFinder));
            dtoBarn.setDiskresjonskode(findKode(dtoBarn.getAktoerId(), piDiskresjonFinder));
        }
    }

    private Diskresjonskode findKode(AktørId aktørId, Function<AktørId, Optional<Tuple<PersonIdent, Diskresjonskode>>> piDiskresjonFinder) {
        return aktørId == null ? null : piDiskresjonFinder.apply(aktørId).map(Tuple::getElement2).orElse(Diskresjonskode.UDEFINERT);
    }

    private String findFnr(AktørId aktørId, Function<AktørId, Optional<Tuple<PersonIdent, Diskresjonskode>>> piDiskresjonFinder) {
        return aktørId == null ? null : piDiskresjonFinder.apply(aktørId).map(Tuple::getElement1).map(PersonIdent::getIdent).orElse(null);

    }

    /** Lag en funksjon som husker resultat av tidligere input. Nyttig for repeterende lookups */
    static <I, O> Function<I, O> memoize(Function<I, O> f) {
        ConcurrentMap<I, O> lookup = new ConcurrentHashMap<>();
        return input -> input == null ? null : lookup.computeIfAbsent(input, f);
    }
}
