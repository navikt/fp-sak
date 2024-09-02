package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;

@ApplicationScoped
public class PersonopplysningDtoTjeneste {

    private static String UTLAND_NAVN = "Utlandsk personident";

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

    private PersonstatusType hentPersonstatus(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat, LocalDate tidspunkt) {
        return Optional.ofNullable(aggregat.getPersonstatusFor(personopplysning.getAktørId(), SimpleLocalDateInterval.enDag(tidspunkt)))
            .map(PersonstatusEntitet::getPersonstatus).orElse(PersonstatusType.UDEFINERT);
    }

    private Region hentRegion(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat, LocalDate tidspunkt, LocalDate stp) {
        return aggregat.getStatsborgerskapRegionVedTidspunkt(personopplysning.getAktørId(), tidspunkt, stp);
    }

    public Optional<PersonopplysningMedlemDto> lagPersonopplysningMedlemskapDto(BehandlingReferanse ref,  Skjæringstidspunkt stp, LocalDate tidspunkt) {
        return personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref)
            .map(aggregat -> enkelMappingMedlemskap(aggregat.getSøker(), aggregat, tidspunkt, stp.getSkjæringstidspunktHvisUtledet()));
    }

    public Optional<PersonopplysningMedlemDto> lagAnnenpartPersonopplysningMedlemskapDto(BehandlingReferanse ref,  Skjæringstidspunkt stp, LocalDate tidspunkt) {
        return personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref)
            .flatMap(agg -> mapAnnenpartMedlemskap(agg, tidspunkt, stp.getSkjæringstidspunktHvisUtledet()));
    }

    private Optional<PersonopplysningMedlemDto> mapAnnenpartMedlemskap(PersonopplysningerAggregat aggregat, LocalDate tidspunkt, Optional<LocalDate> stp) {
        var oppgittAnnenPart = aggregat.getOppgittAnnenPart()
            .filter(oap -> oap.getAktørId() == null && harOppgittLand(oap));
        return oppgittAnnenPart.map(this::enkelUtenlandskAnnenPartMappingMedlemskap)
            .or(() -> aggregat.getAnnenPartEllerEktefelle().map(ap -> enkelMappingMedlemskap(ap, aggregat, tidspunkt, stp)));

    }

    private PersonopplysningMedlemDto enkelUtenlandskAnnenPartMappingMedlemskap(OppgittAnnenPartEntitet oppgittAnnenPart) {
        var dto = new PersonopplysningMedlemDto();
        var bruknavn = Optional.ofNullable(oppgittAnnenPart.getUtenlandskPersonident()).orElse(oppgittAnnenPart.getUtenlandskFnrLand().getKode());
        dto.setNavn(bruknavn);
        dto.setPersonstatus(PersonstatusType.UREG);
        dto.setRegion(MapRegionLandkoder.mapLandkode(oppgittAnnenPart.getUtenlandskFnrLand()));
        return dto;
    }

    private PersonopplysningMedlemDto enkelMappingMedlemskap(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat,
                                                             LocalDate tidspunkt, Optional<LocalDate> skjæringstidspunkt) {
        var dto = new PersonopplysningMedlemDto();
        dto.setAktoerId(personopplysning.getAktørId());
        dto.setRegion(hentRegion(personopplysning, aggregat, tidspunkt, skjæringstidspunkt.orElse(null)));
        dto.setPersonstatus(hentPersonstatus(personopplysning, aggregat, tidspunkt));
        dto.setAdresser(lagAddresseDto(personopplysning, aggregat, tidspunkt));
        return dto;
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
