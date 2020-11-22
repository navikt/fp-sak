package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadDtoFeil;

@ApplicationScoped
public class PersonopplysningDtoTjeneste {

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
        List<PersonAdresseEntitet> adresser = aggregat.getAdresserFor(personopplysning.getAktørId());
        return adresser.stream().map(e -> lagDto(e, personopplysning.getNavn())).collect(Collectors.toList());
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

    private static String formaterMedStoreOgSmåBokstaver(String tekst) {
        if (tekst == null || (tekst = tekst.trim()).isEmpty()) { // NOSONAR
            return null;
        }
        String skilletegnPattern = "(\\s|[()\\-_.,/])";
        char[] tegn = tekst.toLowerCase(Locale.getDefault()).toCharArray();
        boolean nesteSkalHaStorBokstav = true;
        for (int i = 0; i < tegn.length; i++) {
            boolean erSkilletegn = String.valueOf(tegn[i]).matches(skilletegnPattern);
            if (!erSkilletegn && nesteSkalHaStorBokstav) {
                tegn[i] = Character.toTitleCase(tegn[i]);
            }
            nesteSkalHaStorBokstav = erSkilletegn;
        }
        return new String(tegn);
    }

    private boolean harVerge(Long behandlingId) {
        Optional<VergeAggregat> verge = vergeRepository.hentAggregat(behandlingId);
        return verge.isPresent() && verge.get().getVerge().isPresent();
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
        Optional<PersonopplysningerAggregat> aggregatOpt = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(behandling.getId(), behandling.getAktørId(), tidspunkt);

        if (aggregatOpt.isPresent()) {
            Optional<FamilieHendelseGrunnlagEntitet> familieHendelseAggregat = familieHendelseRepository
                .hentAggregatHvisEksisterer(behandlingId);
            PersonopplysningerAggregat aggregat = aggregatOpt.get();
            return Optional.ofNullable(aggregat.getSøker())
                .map(søker -> mapPersonopplysningDto(behandling, søker, aggregat, familieHendelseAggregat));
        }
        return Optional.empty();
    }

    private PersonopplysningDto mapPersonopplysningDto(Behandling behandling,
                                                       PersonopplysningEntitet søker,
                                                       PersonopplysningerAggregat aggregat,
                                                       Optional<FamilieHendelseGrunnlagEntitet> familieHendelseAggregat) {

        RelasjonsRolleType rolleForSøker = utledRolleForSøker(behandling.getFagsak(), aggregat, søker);
        PersonopplysningDto dto = enkelMapping(søker, aggregat, rolleForSøker);

        dto.setBarn(aggregat.getBarna()
            .stream()
            .map(e -> enkelMapping(e, aggregat, RelasjonsRolleType.BARN))
            .collect(Collectors.toList()));

        dto.setBarnSoktFor(Collections.emptyList());

        if (familieHendelseAggregat.isPresent()) {
            final FamilieHendelseGrunnlagEntitet grunnlag = familieHendelseAggregat.get();
            dto.setBarnSoktFor(grunnlag.getGjeldendeBarna().stream()
                .map(this::enkelFHMapping)
                .collect(Collectors.toList()));
        }

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
        if (oppgittAnnenPart.isPresent() && harOppgittLand(oppgittAnnenPart.get().getUtenlandskFnrLand())) {
            var annenPartsRolle = utledRolleForAnnenPart(null, rolleForSøker, aggregat, null);
            return Optional.of(enkelUtenlandskAnnenPartMapping(oppgittAnnenPart.get(), annenPartsRolle));
        }
        return Optional.empty();
    }

    private Optional<PersonopplysningDto> mapRelatertAnnenpart(PersonopplysningerAggregat aggregat, Behandling relatertBehandling, RelasjonsRolleType rolle) {
        return Optional.ofNullable(aggregat.getPersonopplysning(relatertBehandling.getAktørId())).map(p -> enkelMapping(p, aggregat, rolle));
    }

    private boolean harOppgittLand(Landkoder utenlandskFnrLand) {
        return utenlandskFnrLand != null && !Landkoder.UDEFINERT.equals(utenlandskFnrLand);
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
        dto.setNavn(oppgittAnnenPart.getNavn());
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

    public Optional<PersonopplysningMedlemskapDto> lagPersonopplysningMedlemskapDto(Long behandlingId, LocalDate tidspunkt) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<PersonopplysningerAggregat> aggregatOpt = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(behandling.getId(), behandling.getAktørId(), tidspunkt);

        if (aggregatOpt.isPresent()) {
            PersonopplysningerAggregat aggregat = aggregatOpt.get();
            return Optional.ofNullable(aggregat.getSøker())
                .map(søker -> mapPersonopplysningMedlemskapDto(behandling, søker, aggregat));
        }
        return Optional.empty();
    }

    private PersonopplysningMedlemskapDto mapPersonopplysningMedlemskapDto(Behandling behandling,
                                                                           PersonopplysningEntitet søker,
                                                                           PersonopplysningerAggregat aggregat) {

        RelasjonsRolleType rolleForSøker = utledRolleForSøker(behandling.getFagsak(), aggregat, søker);
        PersonopplysningMedlemskapDto dto = enkelMappingMedlemskap(søker, aggregat, rolleForSøker);

        mapAnnenpartMedlemskap(søker, rolleForSøker, aggregat, behandling.getFagsak().getSaksnummer()).ifPresent(dto::setAnnenPart);

        return dto;

    }

    private Optional<PersonopplysningMedlemskapDto> mapAnnenpartMedlemskap(PersonopplysningEntitet søker,
                                                       RelasjonsRolleType rolleForSøker,
                                                       PersonopplysningerAggregat aggregat,
                                                       Saksnummer saksnummner) {
        var annenPartOpplysning = aggregat.getAnnenPart().orElse(null);
        var relatertBehandling = relatertBehandlingTjeneste.hentAnnenPartsGjeldendeVedtattBehandling(saksnummner);
        if (relatertBehandling.isPresent()) {
            var annenPartsRolle = utledRolleForAnnenPart(relatertBehandling.map(Behandling::getFagsak).orElse(null), rolleForSøker,
                aggregat, annenPartOpplysning);
            return mapRelatertAnnenpartMedlemskap(aggregat, relatertBehandling.get(), annenPartsRolle);
        }

        if (annenPartOpplysning != null) {
            if (søker.getAktørId().equals(annenPartOpplysning.getAktørId())) {
                throw SøknadDtoFeil.FACTORY.kanIkkeVæreBådeFarOgMorTilEtBarn().toException();
            }
            var annenPartsRolle = utledRolleForAnnenPart(null, rolleForSøker, aggregat, annenPartOpplysning);
            return Optional.of(enkelMappingMedlemskap(annenPartOpplysning, aggregat, annenPartsRolle));
        }

        var oppgittAnnenPart = aggregat.getOppgittAnnenPart();
        if (oppgittAnnenPart.isPresent() && harOppgittLand(oppgittAnnenPart.get().getUtenlandskFnrLand())) {
            var annenPartsRolle = utledRolleForAnnenPart(null, rolleForSøker, aggregat, null);
            return Optional.of(enkelUtenlandskAnnenPartMappingMedlemskap(oppgittAnnenPart.get(), annenPartsRolle));
        }
        return Optional.empty();
    }

    private Optional<PersonopplysningMedlemskapDto> mapRelatertAnnenpartMedlemskap(PersonopplysningerAggregat aggregat, Behandling relatertBehandling, RelasjonsRolleType rolle) {
        return Optional.ofNullable(aggregat.getPersonopplysning(relatertBehandling.getAktørId())).map(p -> enkelMappingMedlemskap(p, aggregat, rolle));
    }

    private PersonopplysningMedlemskapDto enkelUtenlandskAnnenPartMappingMedlemskap(OppgittAnnenPartEntitet oppgittAnnenPart, RelasjonsRolleType rolle) {
        PersonopplysningMedlemskapDto dto = new PersonopplysningMedlemskapDto();
        PersonstatusType ureg = PersonstatusType.UREG;
        dto.setAvklartPersonstatus(new AvklartPersonstatus(ureg, ureg));
        dto.setPersonstatus(ureg);
        dto.setRelasjonsRolle(rolle);

        if (oppgittAnnenPart.getAktørId() != null) {
            dto.setAktoerId(oppgittAnnenPart.getAktørId().getId());
        }
        dto.setNavn(oppgittAnnenPart.getNavn());
        if (oppgittAnnenPart.getUtenlandskFnrLand() != null) {
            dto.setStatsborgerskap(lagLandkoderDto(oppgittAnnenPart.getUtenlandskFnrLand()));
        }
        return dto;
    }

    private PersonopplysningMedlemskapDto enkelMappingMedlemskap(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat, RelasjonsRolleType rolle) {
        PersonopplysningMedlemskapDto dto = new PersonopplysningMedlemskapDto();
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

        dto.setAktoerId(personopplysning.getAktørId().getId());
        dto.setNavn(formaterMedStoreOgSmåBokstaver(personopplysning.getNavn()));
        dto.setDodsdato(personopplysning.getDødsdato());
        dto.setAdresser(lagAddresseDto(personopplysning, aggregat));
        if (personopplysning.getRegion() != null) {
            dto.setRegion(personopplysning.getRegion());
        }
        dto.setFodselsdato(personopplysning.getFødselsdato());
        return dto;
    }
}
