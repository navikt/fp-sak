package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import static no.nav.foreldrepenger.web.app.util.StringUtils.formaterMedStoreOgSmåBokstaver;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadDtoFeil;

@ApplicationScoped
public class PersonopplysningDtoTjeneste {

    private static final String UTLAND = "UtlandskIdent";

    private VergeRepository vergeRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;

    PersonopplysningDtoTjeneste() {
    }

    @Inject
    public PersonopplysningDtoTjeneste(PersonopplysningTjeneste personopplysningTjeneste,
                                       BehandlingRepositoryProvider repositoryProvider,
                                       VergeRepository vergeRepository,
                                       RelatertBehandlingTjeneste relatertBehandlingTjeneste) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.vergeRepository = vergeRepository;
        this.relatertBehandlingTjeneste = relatertBehandlingTjeneste;
    }

    private static List<PersonadresseDto> lagAddresseDto(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat) {
        return aggregat.getAdresserFor(personopplysning.getAktørId()).stream()
            .map(e -> lagDto(e, personopplysning.getNavn()))
            .collect(Collectors.toList());
    }

    private static LandkoderDto lagLandkoderDto(Landkoder landkode) {
        LandkoderDto dto = new LandkoderDto();
        dto.setKode(landkode.getKode());
        dto.setKodeverk(landkode.getKodeverk());
        dto.setNavn(formaterMedStoreOgSmåBokstaver(landkode.getNavn()));
        return dto;
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

    private boolean harVerge(Long behandlingId) {
        return vergeRepository.hentAggregat(behandlingId).map(VergeAggregat::getVerge).isPresent();
    }

    public PersonopplysningTilbakeDto lagPersonopplysningTilbakeDto(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        var antallBarn = familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getAntallBarn).orElse(0);

        return new PersonopplysningTilbakeDto(behandling.getFagsak().getAktørId().getId(), antallBarn);
    }

    public Optional<PersonopplysningDto> lagPersonopplysningDto(Long behandlingId, LocalDate tidspunkt) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        return personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(behandling.getId(), behandling.getAktørId(), tidspunkt)
            .filter(a -> a.getSøker() != null)
            .map(aggregat -> mapPersonopplysningDto(behandling, aggregat));
    }

    private PersonopplysningDto mapPersonopplysningDto(Behandling behandling,
                                                       PersonopplysningerAggregat aggregat) {

        PersonopplysningEntitet søker = aggregat.getSøker();
        RelasjonsRolleType rolleForSøker = utledRolleForSøker(behandling.getFagsak(), aggregat, søker);
        PersonopplysningDto dto = enkelMapping(søker, aggregat, rolleForSøker);

        dto.setBarn(aggregat.getBarna()
            .stream()
            .map(e -> enkelMapping(e, aggregat, RelasjonsRolleType.BARN))
            .collect(Collectors.toList()));

        var søknadsbarn = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeBarna).orElse(Collections.emptyList()).stream()
            .map(this::enkelFHMapping)
            .collect(Collectors.toList());
        dto.setBarnSoktFor(søknadsbarn);

        mapAnnenpart(søker, rolleForSøker, aggregat, behandling.getFagsak().getSaksnummer()).ifPresent(ap -> {
            ap.setBarn(aggregat.getFellesBarn().stream()
                .map(e -> enkelMapping(e, aggregat, RelasjonsRolleType.BARN))
                .collect(Collectors.toList()));
            dto.setAnnenPart(ap);
        });

        aggregat.getEktefelle().ifPresent(ektefelle -> {
            if (ektefelle.equals(søker)) {
                throw SøknadDtoFeil.FACTORY.kanIkkeVæreSammePersonSomSøker().toException();
            }
            PersonopplysningDto ektefelleDto = enkelMapping(ektefelle, aggregat, RelasjonsRolleType.EKTE);
            dto.setEktefelle(ektefelleDto);
        });

        dto.setHarVerge(harVerge(behandling.getId()));
        return dto;
    }

    private RelasjonsRolleType utledRolleForSøker(Fagsak fagsak, PersonopplysningerAggregat aggregat, PersonopplysningEntitet person) {
        if (RelasjonsRolleType.UDEFINERT.equals(fagsak.getRelasjonsRolleType())) {
            return aggregat.getRelasjoner().stream()
                .filter(r -> r.getTilAktørId().equals(fagsak.getAktørId()) && RelasjonsRolleType.erRegistrertForeldre(r.getRelasjonsrolle()))
                .map(PersonRelasjonEntitet::getRelasjonsrolle)
                .findFirst().orElseGet(() -> NavBrukerKjønn.KVINNE.equals(person.getKjønn()) ? RelasjonsRolleType.MORA : RelasjonsRolleType.FARA);
        }
        return fagsak.getRelasjonsRolleType();
    }

    private RelasjonsRolleType utledRolleForAnnenPart(Fagsak fagsak, RelasjonsRolleType rolleSøker,
                                                      PersonopplysningerAggregat aggregat, PersonopplysningEntitet annenPart) {
        var fraFagsak = fagsak != null ? fagsak.getRelasjonsRolleType() : RelasjonsRolleType.UDEFINERT;
        if (RelasjonsRolleType.erRegistrertForeldre(fraFagsak)) {
            return fraFagsak;
        }
        if (annenPart == null) {
            return RelasjonsRolleType.erMor(rolleSøker) ? RelasjonsRolleType.FARA : RelasjonsRolleType.MORA;
        }
        return aggregat.getRelasjoner().stream()
            .filter(r -> r.getTilAktørId().equals(annenPart.getAktørId()) && RelasjonsRolleType.erRegistrertForeldre(r.getRelasjonsrolle()))
            .map(PersonRelasjonEntitet::getRelasjonsrolle)
            .findFirst().orElseGet(() -> {
                if (RelasjonsRolleType.erMor(rolleSøker)) {
                    return NavBrukerKjønn.MANN.equals(annenPart.getKjønn()) ? RelasjonsRolleType.FARA : RelasjonsRolleType.MEDMOR;
                }
                return RelasjonsRolleType.MORA;
            });
    }

    private Optional<PersonopplysningDto> mapAnnenpart(PersonopplysningEntitet søker,
                                                       RelasjonsRolleType rolleForSøker,
                                                       PersonopplysningerAggregat aggregat,
                                                       Saksnummer saksnummner) {
        var annenPartOpplysning = aggregat.getAnnenPart().orElse(null);
        var relatertBehandling = relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(saksnummner);
        if (relatertBehandling.isPresent()) {
            var annenPartsRolle = utledRolleForAnnenPart(relatertBehandling.map(Behandling::getFagsak).orElse(null), rolleForSøker,
                aggregat, annenPartOpplysning);
            return mapRelatertAnnenpart(aggregat, relatertBehandling.get(), annenPartsRolle);
        }

        if (annenPartOpplysning != null) {
            if (søker.getAktørId().equals(annenPartOpplysning.getAktørId())) {
                throw SøknadDtoFeil.FACTORY.kanIkkeVæreBådeFarOgMorTilEtBarn().toException();
            }
            var annenPartsRolle = utledRolleForAnnenPart(null, rolleForSøker, aggregat, annenPartOpplysning);
            return Optional.of(enkelMapping(annenPartOpplysning, aggregat, annenPartsRolle));
        }

        var oppgittAnnenPart = aggregat.getOppgittAnnenPart();
        if (oppgittAnnenPart.isPresent() && harOppgittLand(oppgittAnnenPart.orElse(null))) {
            var annenPartsRolle = utledRolleForAnnenPart(null, rolleForSøker, aggregat, null);
            return Optional.of(enkelUtenlandskAnnenPartMapping(oppgittAnnenPart.get(), annenPartsRolle));
        }
        return Optional.empty();
    }

    private Optional<PersonopplysningDto> mapRelatertAnnenpart(PersonopplysningerAggregat aggregat, Behandling relatertBehandling, RelasjonsRolleType rolle) {
        return Optional.ofNullable(aggregat.getPersonopplysning(relatertBehandling.getAktørId())).map(p -> enkelMapping(p, aggregat, rolle));
    }

    private boolean harOppgittLand(OppgittAnnenPartEntitet annenPart) {
        return annenPart != null && annenPart.getUtenlandskFnrLand() != null && !Landkoder.UDEFINERT.equals(annenPart.getUtenlandskFnrLand());
    }

    private PersonopplysningDto enkelUtenlandskAnnenPartMapping(OppgittAnnenPartEntitet oppgittAnnenPart, RelasjonsRolleType rolle) {
        PersonopplysningDto dto = new PersonopplysningDto();
        PersonstatusType ureg = PersonstatusType.UREG;
        dto.setAvklartPersonstatus(new AvklartPersonstatus(ureg, ureg));
        dto.setPersonstatus(ureg);
        dto.setRelasjonsRolle(rolle);

        dto.setNavBrukerKjonn(NavBrukerKjønn.UDEFINERT);
        if (oppgittAnnenPart.getAktørId() != null) {
            dto.setAktoerId(oppgittAnnenPart.getAktørId());
        }
        dto.setNavn(oppgittAnnenPart.getUtenlandskPersonident());
        if (oppgittAnnenPart.getUtenlandskFnrLand() != null) {
            dto.setStatsborgerskap(lagLandkoderDto(oppgittAnnenPart.getUtenlandskFnrLand()));
        }
        return dto;
    }


    private PersonopplysningDto enkelFHMapping(UidentifisertBarn uidentifisertBarn) {
        PersonopplysningDto dto = new PersonopplysningDto();
        dto.setNummer(uidentifisertBarn.getBarnNummer());
        dto.setFodselsdato(uidentifisertBarn.getFødselsdato());
        uidentifisertBarn.getDødsdato().ifPresent(dto::setDodsdato);
        return dto;
    }

    private PersonopplysningDto enkelMapping(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat, RelasjonsRolleType rolle) {
        PersonopplysningDto dto = new PersonopplysningDto();
        dto.setNavBrukerKjonn(personopplysning.getKjønn());
        dto.setRelasjonsRolle(rolle);
        final Optional<Landkoder> landkoder = aggregat.getStatsborgerskapFor(personopplysning.getAktørId()).stream().findFirst().map(StatsborgerskapEntitet::getStatsborgerskap);
        landkoder.ifPresent(landkoder1 -> dto.setStatsborgerskap(lagLandkoderDto(landkoder1)));
        final PersonstatusType gjeldendePersonstatus = hentPersonstatus(personopplysning, aggregat);
        dto.setPersonstatus(gjeldendePersonstatus);
        final AvklartPersonstatus avklartPersonstatus = new AvklartPersonstatus(aggregat.getOrginalPersonstatusFor(personopplysning.getAktørId())
            .map(PersonstatusEntitet::getPersonstatus).orElse(gjeldendePersonstatus),
            gjeldendePersonstatus);
        dto.setAvklartPersonstatus(avklartPersonstatus);
        dto.setSivilstand(personopplysning.getSivilstand());

        dto.setAktoerId(personopplysning.getAktørId());
        dto.setNavn(formaterMedStoreOgSmåBokstaver(personopplysning.getNavn()));
        dto.setDodsdato(personopplysning.getDødsdato());
        dto.setAdresser(lagAddresseDto(personopplysning, aggregat));
        if (personopplysning.getRegion() != null) {
            dto.setRegion(personopplysning.getRegion());
        }
        dto.setFodselsdato(personopplysning.getFødselsdato());
        return dto;
    }

    private PersonstatusType hentPersonstatus(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat) {
        PersonstatusEntitet personstatus = aggregat.getPersonstatusFor(personopplysning.getAktørId());
        if (personstatus == null) {
            return PersonstatusType.UDEFINERT;
        }
        return personstatus.getPersonstatus();
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
        var bruknavn = Optional.ofNullable(oppgittAnnenPart.getUtenlandskPersonident()).orElse(oppgittAnnenPart.getType().getKode());
        dto.setNavn(bruknavn);
        dto.setNavBrukerKjonn(NavBrukerKjønn.UDEFINERT);
        dto.setPersonstatus(PersonstatusType.UDEFINERT);
        dto.setRegion(MapRegionLandkoder.mapLandkode(oppgittAnnenPart.getUtenlandskFnrLand().getKode()));
        return dto;
    }

    private PersonopplysningMedlemDto enkelMappingMedlemskap(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat) {
        PersonopplysningMedlemDto dto = new PersonopplysningMedlemDto();
        dto.setAktoerId(personopplysning.getAktørId());
        dto.setNavn(formaterMedStoreOgSmåBokstaver(personopplysning.getNavn()));
        dto.setNavBrukerKjonn(personopplysning.getKjønn());
        Optional.ofNullable(personopplysning.getRegion()).ifPresent(dto::setRegion);
        dto.setPersonstatus(hentPersonstatus(personopplysning, aggregat));
        dto.setFodselsdato(personopplysning.getFødselsdato());
        dto.setDodsdato(personopplysning.getDødsdato());
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

        aggregat.getAktørPersonopplysningMap().forEach((k, v) -> dto.leggTilPerson(k.getId(), enkelPersonMapping(v, aggregat)));

        dto.setBrukerId(aggregat.getSøker().getAktørId().getId());
        aggregat.getAnnenPartEllerEktefelle().map(PersonopplysningEntitet::getAktørId).map(AktørId::getId).ifPresent(dto::setAnnenpartId);
        dto.setBarnMedId(aggregat.getBarna().stream().map(PersonopplysningEntitet::getAktørId).map(AktørId::getId).collect(Collectors.toList()));

        if (dto.getAnnenpartId() == null) {
            aggregat.getOppgittAnnenPart().filter(this::harOppgittLand)
                .ifPresent(annenpart -> {
                    dto.setAnnenpartId(UTLAND);
                    dto.leggTilPerson(UTLAND, enkelUtenlandskAnnenPartMapping(annenpart));
                });
        }
        return dto;
    }

    private PersonopplysningBasisDto enkelPersonMapping(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat) {
        var dto = new PersonopplysningBasisDto(personopplysning.getAktørId().getId());
        dto.setNavBrukerKjonn(personopplysning.getKjønn());
        dto.setSivilstand(personopplysning.getSivilstand());
        dto.setAktoerId(personopplysning.getAktørId());
        dto.setNavn(formaterMedStoreOgSmåBokstaver(personopplysning.getNavn()));
        dto.setFodselsdato(personopplysning.getFødselsdato());
        dto.setDodsdato(personopplysning.getDødsdato());
        dto.setAdresser(lagAddresseDto(personopplysning, aggregat));
        return dto;
    }

    private PersonopplysningBasisDto enkelUtenlandskAnnenPartMapping(OppgittAnnenPartEntitet oppgittAnnenPart) {
        var dto = new PersonopplysningBasisDto(UTLAND);

        dto.setNavBrukerKjonn(NavBrukerKjønn.UDEFINERT);
        var bruknavn = Optional.ofNullable(oppgittAnnenPart.getUtenlandskPersonident()).orElse(oppgittAnnenPart.getType().getKode());
        dto.setNavn(bruknavn);
        return dto;
    }
}
