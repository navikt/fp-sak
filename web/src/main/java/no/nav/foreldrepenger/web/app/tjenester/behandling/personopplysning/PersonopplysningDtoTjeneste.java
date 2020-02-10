package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonAdresseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.StatsborgerskapEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.verge.VergeRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.SøknadDtoFeil;

@ApplicationScoped
public class PersonopplysningDtoTjeneste {

    private VergeRepository vergeRepository;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    PersonopplysningDtoTjeneste() {
    }

    @Inject
    public PersonopplysningDtoTjeneste(PersonopplysningTjeneste personopplysningTjeneste,
                                       BehandlingRepositoryProvider repositoryProvider,
                                       VergeRepository vergeRepository) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.vergeRepository = vergeRepository;
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

    public Optional<PersonopplysningDto> lagPersonopplysningDto(Long behandlingId, LocalDate tidspunkt) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<PersonopplysningerAggregat> aggregatOpt = personopplysningTjeneste.hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(behandling.getId(), behandling.getAktørId(), tidspunkt);

        if (aggregatOpt.isPresent()) {
            Optional<FamilieHendelseGrunnlagEntitet> familieHendelseAggregat = familieHendelseRepository
                .hentAggregatHvisEksisterer(behandlingId);
            PersonopplysningerAggregat aggregat = aggregatOpt.get();
            return Optional.ofNullable(aggregat.getSøker())
                .map(søker -> mapPersonopplysningDto(behandlingId, søker, aggregat, familieHendelseAggregat));
        }
        return Optional.empty();
    }

    private PersonopplysningDto mapPersonopplysningDto(Long behandlingId, PersonopplysningEntitet søker,
                                                       PersonopplysningerAggregat aggregat, Optional<FamilieHendelseGrunnlagEntitet> familieHendelseAggregat) {

        PersonopplysningDto dto = enkelMapping(søker, aggregat);

        dto.setBarn(aggregat.getBarna()
            .stream()
            .map(e -> enkelMapping(e, aggregat))
            .collect(Collectors.toList()));

        dto.setBarnSoktFor(Collections.emptyList());

        if (familieHendelseAggregat.isPresent()) {
            final FamilieHendelseGrunnlagEntitet grunnlag = familieHendelseAggregat.get();
            dto.setBarnSoktFor(grunnlag.getGjeldendeBarna().stream()
                .map(this::enkelFHMapping)
                .collect(Collectors.toList()));
        }

        Optional<OppgittAnnenPartEntitet> oppgittAnnenPart = aggregat.getOppgittAnnenPart();

        if (oppgittAnnenPart.isPresent()) {
            Optional<PersonopplysningDto> annenPart = mapAnnenPart(søker, aggregat, oppgittAnnenPart.get());
            annenPart.ifPresent(dto::setAnnenPart);
        }
        Optional<PersonopplysningEntitet> ektefelleOpt = aggregat.getEktefelle();
        if (ektefelleOpt.isPresent() && ektefelleOpt.get().equals(søker)) {
            throw SøknadDtoFeil.FACTORY.kanIkkeVæreSammePersonSomSøker().toException();
        }

        if (ektefelleOpt.isPresent()) {
            PersonopplysningDto ektefelle = enkelMapping(ektefelleOpt.get(), aggregat);
            dto.setEktefelle(ektefelle);
        }

        if (harVerge(behandlingId)) {
            dto.setHarVerge(true);
        }
        return dto;
    }

    private Optional<PersonopplysningDto> mapAnnenPart(PersonopplysningEntitet søker, PersonopplysningerAggregat aggregat, OppgittAnnenPartEntitet oppgittAnnenPart) {
        if (søker.getAktørId().equals(oppgittAnnenPart.getAktørId())) {
            throw SøknadDtoFeil.FACTORY.kanIkkeVæreBådeFarOgMorTilEtBarn().toException();
        }

        PersonopplysningDto annenPart = null;
        Optional<PersonopplysningEntitet> annenPartOpt = aggregat.getAnnenPart();

        if (annenPartOpt.isPresent()) {
            annenPart = enkelMapping(annenPartOpt.get(), aggregat);
        } else if (harOppgittLand(oppgittAnnenPart.getUtenlandskFnrLand())) {
            annenPart = enkelUtenlandskAnnenPartMapping(oppgittAnnenPart);
        }

        if (annenPart != null) {
            annenPart.setBarn(aggregat.getFellesBarn().stream()
                .map(e -> enkelMapping(e, aggregat))
                .collect(Collectors.toList()));
        }
        return Optional.ofNullable(annenPart);
    }

    private boolean harOppgittLand(Landkoder utenlandskFnrLand) {
        return utenlandskFnrLand != null && !Landkoder.UDEFINERT.equals(utenlandskFnrLand);
    }

    private PersonopplysningDto enkelUtenlandskAnnenPartMapping(OppgittAnnenPartEntitet oppgittAnnenPart) {
        PersonopplysningDto dto = new PersonopplysningDto();
        PersonstatusType ureg = PersonstatusType.UREG;
        dto.setAvklartPersonstatus(new AvklartPersonstatus(ureg, ureg));
        dto.setPersonstatus(ureg);

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

    private PersonopplysningDto enkelMapping(PersonopplysningEntitet personopplysning, PersonopplysningerAggregat aggregat) {
        PersonopplysningDto dto = new PersonopplysningDto();
        dto.setNavBrukerKjonn(personopplysning.getKjønn());
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
}
