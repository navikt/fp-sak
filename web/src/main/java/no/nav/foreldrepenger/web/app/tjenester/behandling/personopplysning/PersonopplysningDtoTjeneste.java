package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class PersonopplysningDtoTjeneste {

    private static final String UTLAND_NAVN = "Utlandsk personident";

    private PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;


    PersonopplysningDtoTjeneste() {
    }

    @Inject
    public PersonopplysningDtoTjeneste(PersonopplysningTjeneste personopplysningTjeneste,
                                       BehandlingRepositoryProvider repositoryProvider) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    private static List<PersonadresseDto> lagAddresseDto(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat) {
        return aggregat.getAdresserFor(personopplysning.getAktørId(), SimpleLocalDateInterval.fraOgMedTomNotNull(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)).stream()
            .map(PersonadresseDto::tilDto)
            .toList();
    }

    private boolean harOppgittLand(OppgittAnnenPartEntitet annenPart) {
        return annenPart != null && annenPart.getUtenlandskFnrLand() != null && !Landkoder.UDEFINERT.equals(annenPart.getUtenlandskFnrLand());
    }

    public Optional<PersonoversiktDto> lagPersonversiktDto(BehandlingReferanse ref) {
        return personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref)
            .filter(a -> a.getSøker() != null)
            .map(this::mapPersonoversikt);
    }

    private PersonoversiktDto mapPersonoversikt(PersonopplysningerAggregat aggregat) {
        var dto = new PersonoversiktDto();

        dto.setBruker(enkelPersonMapping(aggregat.getSøker(), aggregat));
        aggregat.getAnnenPartEllerEktefelle().map(p -> enkelPersonMapping(p, aggregat)).ifPresent(dto::setAnnenPart);
        aggregat.getBarna().stream().map(p -> enkelPersonMapping(p, aggregat)).forEach(dto::leggTilBarn);

        if (dto.getAnnenPart() == null) {
            aggregat.getOppgittAnnenPart().filter(this::harOppgittLand)
                .ifPresent(annenpart -> dto.setAnnenPart(enkelUtenlandskAnnenPartMapping()));
        }
        return dto;
    }

    private PersonopplysningBasisDto enkelPersonMapping(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat) {
        var dto = new PersonopplysningBasisDto();
        dto.setKjønn(personopplysning.getKjønn());
        dto.setSivilstand(personopplysning.getSivilstand());
        dto.setAktørId(personopplysning.getAktørId());
        dto.setFødselsdato(personopplysning.getFødselsdato());
        dto.setDødsdato(personopplysning.getDødsdato());
        dto.setAdresser(lagAddresseDto(personopplysning, aggregat));
        return dto;
    }

    private PersonopplysningBasisDto enkelUtenlandskAnnenPartMapping() {
        var dto = new PersonopplysningBasisDto();
        dto.setKjønn(NavBrukerKjønn.UDEFINERT);
        dto.setNavn(UTLAND_NAVN);
        return dto;
    }
}
