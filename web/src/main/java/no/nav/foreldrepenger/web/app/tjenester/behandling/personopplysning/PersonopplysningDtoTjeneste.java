package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import static no.nav.foreldrepenger.web.app.util.StringUtils.formaterMedStoreOgSmåBokstaver;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;

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

    private static List<PersonadresseDto> lagAddresseDto(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat) {
        return aggregat.getAdresserFor(personopplysning.getAktørId()).stream()
            .map(e -> lagDto(e, personopplysning.getNavn()))
            .collect(Collectors.toList());
    }

    private static PersonadresseDto lagDto(PersonAdresseEntitet adresse, String navn) {
        PersonadresseDto dto = new PersonadresseDto();
        dto.setAdresselinje1(formaterMedStoreOgSmåBokstaver(adresse.getAdresselinje1()));
        dto.setAdresselinje2(formaterMedStoreOgSmåBokstaver(adresse.getAdresselinje2()));
        dto.setAdresselinje3(formaterMedStoreOgSmåBokstaver(adresse.getAdresselinje3()));
        dto.setMottakerNavn(formaterMedStoreOgSmåBokstaver(navn));
        dto.setPoststed(formaterMedStoreOgSmåBokstaver(adresse.getPoststed()));
        dto.setPostNummer(adresse.getPostnummer());
        dto.setLand(adresse.getLand());
        dto.setAdresseType(adresse.getAdresseType());
        return dto;

    }

    public PersonopplysningTilbakeDto lagPersonopplysningTilbakeDto(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        var antallBarn = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getAntallBarn).orElse(0);

        return new PersonopplysningTilbakeDto(behandling.getFagsak().getAktørId().getId(), antallBarn);
    }

    private boolean harOppgittLand(OppgittAnnenPartEntitet annenPart) {
        return annenPart != null && annenPart.getUtenlandskFnrLand() != null && !Landkoder.UDEFINERT.equals(annenPart.getUtenlandskFnrLand());
    }

    private PersonstatusType hentPersonstatus(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat) {
        return Optional.ofNullable(aggregat.getPersonstatusFor(personopplysning.getAktørId()))
            .map(PersonstatusEntitet::getPersonstatus).orElse(PersonstatusType.UDEFINERT);
    }

    public Optional<PersonopplysningMedlemDto> lagPersonopplysningMedlemskapDto(Long behandlingId, LocalDate tidspunkt) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(behandling.getId(), behandling.getAktørId(), tidspunkt)
            .map(aggregat -> enkelMappingMedlemskap(aggregat.getSøker(), aggregat));
    }

    public Optional<PersonopplysningMedlemDto> lagAnnenpartPersonopplysningMedlemskapDto(Long behandlingId, LocalDate tidspunkt) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(behandlingId, behandling.getAktørId(), tidspunkt)
            .flatMap(this::mapAnnenpartMedlemskap);
    }

    private Optional<PersonopplysningMedlemDto> mapAnnenpartMedlemskap(PersonopplysningerAggregat aggregat) {
        var oppgittAnnenPart = aggregat.getOppgittAnnenPart()
            .filter(oap -> oap.getAktørId() == null && harOppgittLand(oap));
        return oppgittAnnenPart.map(this::enkelUtenlandskAnnenPartMappingMedlemskap)
            .or(() -> aggregat.getAnnenPartEllerEktefelle().map(ap -> enkelMappingMedlemskap(ap, aggregat)));

    }

    private PersonopplysningMedlemDto enkelUtenlandskAnnenPartMappingMedlemskap(OppgittAnnenPartEntitet oppgittAnnenPart) {
        PersonopplysningMedlemDto dto = new PersonopplysningMedlemDto();
        var bruknavn = Optional.ofNullable(oppgittAnnenPart.getUtenlandskPersonident()).orElse(oppgittAnnenPart.getUtenlandskFnrLand().getKode());
        dto.setNavn(bruknavn);
        dto.setPersonstatus(PersonstatusType.UREG);
        dto.setRegion(MapRegionLandkoder.mapLandkode(oppgittAnnenPart.getUtenlandskFnrLand().getKode()));
        return dto;
    }

    private PersonopplysningMedlemDto enkelMappingMedlemskap(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat) {
        PersonopplysningMedlemDto dto = new PersonopplysningMedlemDto();
        dto.setAktoerId(personopplysning.getAktørId());
        dto.setNavn(formaterMedStoreOgSmåBokstaver(personopplysning.getNavn()));
        Optional.ofNullable(personopplysning.getRegion()).ifPresent(dto::setRegion);
        var gjeldendePersonstatus = hentPersonstatus(personopplysning, aggregat);
        dto.setPersonstatus(gjeldendePersonstatus);
        dto.setAdresser(lagAddresseDto(personopplysning, aggregat));
        return dto;
    }

    public Optional<PersonoversiktDto> lagPersonversiktDto(Long behandlingId, LocalDate tidspunkt) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(behandlingId, behandling.getAktørId(), tidspunkt)
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
        var dto = new PersonopplysningBasisDto(personopplysning.getAktørId());
        dto.setKjønn(personopplysning.getKjønn());
        dto.setSivilstand(personopplysning.getSivilstand());
        dto.setAktoerId(personopplysning.getAktørId());
        dto.setNavn(formaterMedStoreOgSmåBokstaver(personopplysning.getNavn()));
        dto.setFødselsdato(personopplysning.getFødselsdato());
        dto.setDødsdato(personopplysning.getDødsdato());
        dto.setAdresser(lagAddresseDto(personopplysning, aggregat));
        return dto;
    }

    private PersonopplysningBasisDto enkelUtenlandskAnnenPartMapping() {
        var dto = new PersonopplysningBasisDto(null);
        dto.setKjønn(NavBrukerKjønn.UDEFINERT);
        dto.setNavn(UTLAND_NAVN);
        return dto;
    }
}
