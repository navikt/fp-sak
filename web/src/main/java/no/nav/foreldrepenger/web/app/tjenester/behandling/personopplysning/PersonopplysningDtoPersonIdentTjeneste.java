package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

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
        Function<AktørId, Optional<PersonIdent>> personInfoFinder = memoize((aktørId) -> personinfoAdapter.hentFnr(aktørId));
        Function<AktørId, Optional<String>> diskresjonskodeFinder = memoize((aktørId) -> personinfoAdapter.hentDiskresjonskodeForAktør(aktørId));

        // Sett fødselsnummer og diskresjonskodepå personopplysning for alle
        // behandlinger. Fødselsnummer og diskresjonskode lagres ikke i basen og må derfor hentes fra
        // TPS/IdentRepository// for å vises i GUI.
        if (personopplysningDto != null) {
            setFnrPaPersonopplysning(personopplysningDto,
                personInfoFinder,
                diskresjonskodeFinder);
        }

    }

    void setFnrPaPersonopplysning(PersonopplysningDto dto, Function<AktørId, Optional<PersonIdent>> tpsFnrFinder,
                                  Function<AktørId, Optional<String>> tpsKodeFinder) {

        // Soker
        dto.setFnr(findFnr(dto.getAktoerId(), tpsFnrFinder)); // forelder / soeker
        dto.setDiskresjonskode(findKode(dto.getAktoerId(), tpsKodeFinder));

        // Medsoker
        if (dto.getAnnenPart() != null) {
            dto.getAnnenPart().setFnr(findFnr(dto.getAnnenPart().getAktoerId(), tpsFnrFinder));
            dto.getAnnenPart().setDiskresjonskode(findKode(dto.getAnnenPart().getAktoerId(), tpsKodeFinder));
            // Medsøkers barn
            if (!dto.getAnnenPart().getBarn().isEmpty()) {
                for (PersonopplysningDto dtoBarn : dto.getAnnenPart().getBarn()) {
                    dtoBarn.setFnr(findFnr(dtoBarn.getAktoerId(), tpsFnrFinder));
                    dtoBarn.setDiskresjonskode(findKode(dtoBarn.getAktoerId(), tpsKodeFinder));
                }
            }
        }

        // ektefelle
        if (dto.getEktefelle() != null) {
            dto.getEktefelle().setFnr(findFnr(dto.getEktefelle().getAktoerId(), tpsFnrFinder));
            dto.getEktefelle().setDiskresjonskode(findKode(dto.getEktefelle().getAktoerId(), tpsKodeFinder));
        }

        // Barn
        for (PersonopplysningDto dtoBarn : dto.getBarn()) {
            dtoBarn.setFnr(findFnr(dtoBarn.getAktoerId(), tpsFnrFinder));
            dtoBarn.setDiskresjonskode(findKode(dtoBarn.getAktoerId(), tpsKodeFinder));
        }
    }

    private Diskresjonskode findKode(AktørId aktørId, Function<AktørId, Optional<String>> tpsKodeFinder) {
        if (aktørId != null) {
            Optional<String> kode = tpsKodeFinder.apply(aktørId);
            if (kode.isPresent()) {
                return Diskresjonskode.fraKode(kode.get());
            }
        }
        return Diskresjonskode.UDEFINERT;
    }

    private String findFnr(AktørId aktørId, Function<AktørId, Optional<PersonIdent>> tpsFnrFinder) {
        return aktørId == null ? null : tpsFnrFinder.apply(aktørId).map(PersonIdent::getIdent).orElse(null);

    }

    /** Lag en funksjon som husker resultat av tidligere input. Nyttig for repeterende lookups */
    static <I, O> Function<I, O> memoize(Function<I, O> f) {
        ConcurrentMap<I, O> lookup = new ConcurrentHashMap<>();
        return input -> input == null ? null : lookup.computeIfAbsent(input, f);
    }
}
