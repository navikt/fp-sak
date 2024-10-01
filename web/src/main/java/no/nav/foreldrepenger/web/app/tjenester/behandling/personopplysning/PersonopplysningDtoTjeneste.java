package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;

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

    private static List<PersonadresseDto> lagAddresseDto(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat, LocalDate tidspunkt) {
        return aggregat.getAdresserFor(personopplysning.getAktørId(), SimpleLocalDateInterval.enDag(tidspunkt)).stream()
            .map(PersonadresseDto::tilDto)
            .toList();
    }

    public PersonopplysningTilbakeDto lagPersonopplysningTilbakeDto(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var antallBarn = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getAntallBarn).orElse(0);

        return new PersonopplysningTilbakeDto(behandling.getFagsak().getAktørId().getId(), antallBarn);
    }

    private boolean harOppgittLand(OppgittAnnenPartEntitet annenPart) {
        return annenPart != null && annenPart.getUtenlandskFnrLand() != null && !Landkoder.UDEFINERT.equals(annenPart.getUtenlandskFnrLand());
    }

    public Optional<PersonoversiktDto> lagPersonversiktDto(Long behandlingId, LocalDate tidspunkt) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(behandlingId, behandling.getAktørId())
            .filter(a -> a.getSøker() != null)
            .map(a -> mapPersonoversikt(a, tidspunkt));
    }

    private PersonoversiktDto mapPersonoversikt(PersonopplysningerAggregat aggregat, LocalDate tidspunkt) {
        var dto = new PersonoversiktDto();

        dto.setBruker(enkelPersonMapping(aggregat.getSøker(), aggregat, tidspunkt));
        aggregat.getAnnenPartEllerEktefelle().map(p -> enkelPersonMapping(p, aggregat, tidspunkt)).ifPresent(dto::setAnnenPart);
        aggregat.getBarna().stream().map(p -> enkelPersonMapping(p, aggregat, tidspunkt)).forEach(dto::leggTilBarn);

        if (dto.getAnnenPart() == null) {
            aggregat.getOppgittAnnenPart().filter(this::harOppgittLand)
                .ifPresent(annenpart -> dto.setAnnenPart(enkelUtenlandskAnnenPartMapping()));
        }
        return dto;
    }

    private PersonopplysningBasisDto enkelPersonMapping(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat, LocalDate tidspunkt) {
        var dto = new PersonopplysningBasisDto(personopplysning.getAktørId());
        dto.setKjønn(personopplysning.getKjønn());
        dto.setSivilstand(personopplysning.getSivilstand());
        dto.setAktoerId(personopplysning.getAktørId());
        dto.setFødselsdato(personopplysning.getFødselsdato());
        dto.setDødsdato(personopplysning.getDødsdato());
        dto.setAdresser(lagAddresseDto(personopplysning, aggregat, tidspunkt));
        return dto;
    }

    private PersonopplysningBasisDto enkelUtenlandskAnnenPartMapping() {
        var dto = new PersonopplysningBasisDto(null);
        dto.setKjønn(NavBrukerKjønn.UDEFINERT);
        dto.setNavn(UTLAND_NAVN);
        return dto;
    }
}
