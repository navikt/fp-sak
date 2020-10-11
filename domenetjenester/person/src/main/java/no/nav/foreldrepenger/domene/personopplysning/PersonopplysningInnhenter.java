package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.threeten.extra.Interval;

import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.Familierelasjon;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.PersonstatusPeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.StatsborgerskapPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class PersonopplysningInnhenter {

    private PersoninfoAdapter personinfoAdapter;

    PersonopplysningInnhenter() {
        // for CDI proxy
    }

    @Inject
    public PersonopplysningInnhenter(PersoninfoAdapter personinfoAdapter) {
        this.personinfoAdapter = personinfoAdapter;
    }

    public List<FødtBarnInfo> innhentAlleFødteForIntervaller(AktørId aktørId, List<LocalDateInterval> intervaller) {
        return personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(aktørId, intervaller);
    }

    public void innhentPersonopplysninger(PersonInformasjonBuilder informasjonBuilder, AktørId søker, Optional<AktørId> annenPart,
                                          Interval opplysningsperiode, List<LocalDateInterval> fødselsIntervaller) {
        Personinfo søkerInfo = innhentSaksopplysningerForSøker(søker);
        if (søkerInfo == null) {
            throw new IllegalArgumentException("Finner ikke personinformasjon for aktør " + søker.getId());
        }
        Optional<Personinfo> medsøkerInfo = annenPart.flatMap(this::innhentSaksopplysningerFor);

        byggPersonopplysningMedRelasjoner(informasjonBuilder, søkerInfo, medsøkerInfo, opplysningsperiode, fødselsIntervaller);
    }

    private Personinfo innhentSaksopplysningerForSøker(AktørId søkerAktørId) {
        return personinfoAdapter.innhentSaksopplysningerForSøker(søkerAktørId);
    }

    private Optional<Personinfo> innhentSaksopplysningerFor(AktørId aktørId) {
        return personinfoAdapter.innhentSaksopplysningerForEktefelle(aktørId);
    }

    private PersonInformasjonBuilder byggPersonopplysningMedRelasjoner(PersonInformasjonBuilder informasjonBuilder,
                                                                       Personinfo søkerPersonInfo,
                                                                       Optional<Personinfo> annenPartInfo,
                                                                       Interval opplysningsperioden,
                                                                       List<LocalDateInterval> fødselsintervall) {

        // Historikk for søker
        final Personhistorikkinfo personhistorikkinfo = personinfoAdapter.innhentPersonopplysningerHistorikk(søkerPersonInfo.getAktørId(), opplysningsperioden);
        if (personhistorikkinfo != null) {
            mapAdresser(personhistorikkinfo.getAdressehistorikk(), informasjonBuilder, søkerPersonInfo);
            mapStatsborgerskap(personhistorikkinfo.getStatsborgerskaphistorikk(), informasjonBuilder, søkerPersonInfo);
            mapPersonstatus(personhistorikkinfo.getPersonstatushistorikk(), informasjonBuilder, søkerPersonInfo);
        }

        mapTilPersonopplysning(søkerPersonInfo, informasjonBuilder, true, false, fødselsintervall);
        // Ektefelle
        leggTilEktefelle(søkerPersonInfo, informasjonBuilder, fødselsintervall);

        // Medsøker (annen part). kan være samme person som Ektefelle
        annenPartInfo.ifPresent(annenPart -> leggTilMedsøkerAnnenPart(søkerPersonInfo, annenPart, informasjonBuilder, fødselsintervall));

        return informasjonBuilder;
    }

    private void mapPersonstatus(List<PersonstatusPeriode> personstatushistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        for (PersonstatusPeriode personstatus : personstatushistorikk) {
            final PersonstatusType status = personstatus.getPersonstatus();
            final DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(
                brukFødselsdatoHvisEtter(personstatus.getGyldighetsperiode().getFom(), personinfo.getFødselsdato()),
                personstatus.getGyldighetsperiode().getTom());
            final PersonInformasjonBuilder.PersonstatusBuilder builder = informasjonBuilder.getPersonstatusBuilder(personinfo.getAktørId(), periode);
            builder.medPeriode(periode)
                .medPersonstatus(status);
            informasjonBuilder.leggTil(builder);
        }
    }

    private void mapStatsborgerskap(List<StatsborgerskapPeriode> statsborgerskaphistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        for (StatsborgerskapPeriode statsborgerskap : statsborgerskaphistorikk) {
            final Landkoder landkode = Landkoder.fraKode(statsborgerskap.getStatsborgerskap().getLandkode());

            Region region = MapRegionLandkoder.mapLandkode(statsborgerskap.getStatsborgerskap().getLandkode());

            final DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(brukFødselsdatoHvisEtter(
                statsborgerskap.getGyldighetsperiode().getFom(), personinfo.getFødselsdato()), statsborgerskap.getGyldighetsperiode().getTom());
            final PersonInformasjonBuilder.StatsborgerskapBuilder builder = informasjonBuilder.getStatsborgerskapBuilder(personinfo.getAktørId(), periode,
                landkode, region);
            builder.medPeriode(periode)
                .medStatsborgerskap(landkode);
            builder.medRegion(region);

            informasjonBuilder.leggTil(builder);
        }
    }

    private void mapAdresser(List<AdressePeriode> adressehistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        AktørId aktørId = personinfo.getAktørId();
        for (AdressePeriode adresse : adressehistorikk) {
            final DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(
                brukFødselsdatoHvisEtter(adresse.getGyldighetsperiode().getFom(), personinfo.getFødselsdato()), adresse.getGyldighetsperiode().getTom());
            final PersonInformasjonBuilder.AdresseBuilder adresseBuilder = informasjonBuilder.getAdresseBuilder(aktørId, periode,
                adresse.getAdresse().getAdresseType());
            adresseBuilder.medPeriode(periode)
                .medAdresselinje1(adresse.getAdresse().getAdresselinje1())
                .medAdresselinje2(adresse.getAdresse().getAdresselinje2())
                .medAdresselinje3(adresse.getAdresse().getAdresselinje3())
                .medAdresselinje4(adresse.getAdresse().getAdresselinje4())
                .medLand(adresse.getAdresse().getLand())
                .medPostnummer(adresse.getAdresse().getPostnummer())
                .medPoststed(adresse.getAdresse().getPoststed());
            informasjonBuilder.leggTil(adresseBuilder);
        }
    }

    private LocalDate brukFødselsdatoHvisEtter(LocalDate dato, LocalDate fødseldato) {
        if (dato.isBefore(fødseldato)) {
            return fødseldato;
        }
        return dato;
    }

    private void mapTilPersonopplysning(Personinfo personinfo, PersonInformasjonBuilder informasjonBuilder, boolean skalHenteBarnRelasjoner,
                                        boolean erIkkeSøker, List<LocalDateInterval> fødselsintervall) {
        mapInfoTilEntitet(personinfo, informasjonBuilder, erIkkeSøker);

        if (skalHenteBarnRelasjoner) {
            List<Personinfo> barna = hentBarnRelatertTil(personinfo, fødselsintervall);
            barna.forEach(barn -> {
                mapInfoTilEntitet(barn, informasjonBuilder, true);
                mapRelasjon(personinfo, barn, Collections.singletonList(RelasjonsRolleType.BARN), informasjonBuilder);
                mapRelasjon(barn, personinfo, utledRelasjonsrolleTilBarn(personinfo, barn), informasjonBuilder);
            });
        }
    }

    private List<RelasjonsRolleType> utledRelasjonsrolleTilBarn(Personinfo personinfo, Personinfo barn) {
        if (barn == null) {
            return Collections.emptyList();
        }
        return barn.getFamilierelasjoner().stream()
            .filter(fr -> fr.getPersonIdent().equals(personinfo.getPersonIdent()))
            .map(rel -> utledRelasjonsrolleTilBarn(personinfo.getKjønn(), rel.getRelasjonsrolle()))
            .collect(Collectors.toList());
    }

    private void mapRelasjon(Personinfo fra, Personinfo til, List<RelasjonsRolleType> roller, PersonInformasjonBuilder informasjonBuilder) {
        if (til == null) {
            return;
        }
        for (RelasjonsRolleType rolle : roller) {
            final PersonInformasjonBuilder.RelasjonBuilder builder = informasjonBuilder.getRelasjonBuilder(fra.getAktørId(), til.getAktørId(), rolle);
            builder.harSammeBosted(utledSammeBosted(fra, til, rolle));
            informasjonBuilder.leggTil(builder);
        }
    }

    private boolean utledSammeBosted(Personinfo personinfo, Personinfo barn, RelasjonsRolleType rolle) {
        final Optional<Boolean> sammeBosted = personinfo.getFamilierelasjoner().stream()
            .filter(fr -> fr.getRelasjonsrolle().equals(rolle) && fr.getPersonIdent().equals(barn.getPersonIdent()))
            .findAny()
            .map(Familierelasjon::getHarSammeBosted);
        return sammeBosted.orElse(false);
    }

    private void mapInfoTilEntitet(Personinfo personinfo, PersonInformasjonBuilder informasjonBuilder, boolean lagreIHistoriskeTabeller) {
        final DatoIntervallEntitet periode = getPeriode(personinfo.getFødselsdato(), Tid.TIDENES_ENDE);
        final PersonInformasjonBuilder.PersonopplysningBuilder builder = informasjonBuilder.getPersonopplysningBuilder(personinfo.getAktørId());
        builder.medKjønn(personinfo.getKjønn())
            .medFødselsdato(personinfo.getFødselsdato())
            .medNavn(personinfo.getNavn())
            .medDødsdato(personinfo.getDødsdato())
            .medSivilstand(personinfo.getSivilstandType())
            .medRegion(personinfo.getRegion());
        informasjonBuilder.leggTil(builder);

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttStatsborgerskapHistorikk(personinfo.getAktørId())) {
            final PersonInformasjonBuilder.StatsborgerskapBuilder statsborgerskapBuilder = informasjonBuilder.getStatsborgerskapBuilder(personinfo.getAktørId(),
                periode, personinfo.getLandkode(), personinfo.getRegion());
            informasjonBuilder.leggTil(statsborgerskapBuilder);
        }

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttAdresseHistorikk(personinfo.getAktørId())) {
            for (Adresseinfo adresse : personinfo.getAdresseInfoList()) {
                final PersonInformasjonBuilder.AdresseBuilder adresseBuilder = informasjonBuilder.getAdresseBuilder(personinfo.getAktørId(),
                    periode, adresse.getGjeldendePostadresseType());
                informasjonBuilder.leggTil(adresseBuilder
                    .medAdresselinje1(adresse.getAdresselinje1())
                    .medAdresselinje2(adresse.getAdresselinje2())
                    .medAdresselinje3(adresse.getAdresselinje3())
                    .medPostnummer(adresse.getPostNr())
                    .medPoststed(adresse.getPoststed())
                    .medLand(adresse.getLand())
                    .medAdresseType(adresse.getGjeldendePostadresseType())
                    .medPeriode(periode));
            }
        }

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttPersonstatusHistorikk(personinfo.getAktørId())) {
            final PersonInformasjonBuilder.PersonstatusBuilder personstatusBuilder = informasjonBuilder.getPersonstatusBuilder(personinfo.getAktørId(),
                periode).medPersonstatus(personinfo.getPersonstatus());
            informasjonBuilder.leggTil(personstatusBuilder);
        }
    }

    private DatoIntervallEntitet getPeriode(LocalDate fom, LocalDate tom) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom != null ? tom : Tid.TIDENES_ENDE);
    }

    private void leggTilMedsøkerAnnenPart(Personinfo søkerPersonInfo, Personinfo annenPart, PersonInformasjonBuilder informasjonBuilder,
                                          List<LocalDateInterval> fødselsintervall) {
        // Medsøker - kan være samme person som ektefelle
        List<Personinfo> fellesBarn = finnFellesBarn(annenPart, søkerPersonInfo, fødselsintervall);

        mapTilPersonopplysning(annenPart, informasjonBuilder, false, true, fødselsintervall);
        for (Personinfo barn : fellesBarn) {
            final Personinfo til = personinfoAdapter.innhentSaksopplysninger(barn.getPersonIdent()).orElse(null);
            mapRelasjon(annenPart, til, Collections.singletonList(RelasjonsRolleType.BARN), informasjonBuilder);
            mapRelasjon(til, annenPart, utledRelasjonsrolleTilBarn(annenPart, til), informasjonBuilder); // NOSONAR
        }
    }

    private void leggTilEktefelle(Personinfo søkerPersonInfo, PersonInformasjonBuilder informasjonBuilder, List<LocalDateInterval> fødselsintervall) {
        // Ektefelle
        final List<Familierelasjon> familierelasjoner = søkerPersonInfo.getFamilierelasjoner()
            .stream()
            .filter(f -> f.getRelasjonsrolle().equals(RelasjonsRolleType.EKTE) ||
                f.getRelasjonsrolle().equals(RelasjonsRolleType.REGISTRERT_PARTNER) ||
                f.getRelasjonsrolle().equals(RelasjonsRolleType.SAMBOER))
            .collect(Collectors.toList());
        for (Familierelasjon familierelasjon : familierelasjoner) {
            Optional<Personinfo> ektefelleInfo = personinfoAdapter.innhentSaksopplysninger(familierelasjon.getPersonIdent());
            if (ektefelleInfo.isPresent()) {
                final Personinfo personinfo = ektefelleInfo.get();
                mapTilPersonopplysning(personinfo, informasjonBuilder, false, true, fødselsintervall);
                mapRelasjon(søkerPersonInfo, personinfo, Collections.singletonList(familierelasjon.getRelasjonsrolle()), informasjonBuilder);
                mapRelasjon(personinfo, søkerPersonInfo, Collections.singletonList(familierelasjon.getRelasjonsrolle()), informasjonBuilder);
            }
        }
    }

    private List<Personinfo> hentBarnRelatertTil(Personinfo personinfo, List<LocalDateInterval> fødselsintervall) {
        List<Personinfo> relaterteBarn = hentAlleRelaterteBarn(personinfo);

        return relaterteBarn.stream().filter(b -> fødselsintervall.stream().anyMatch(i -> i.encloses(b.getFødselsdato()))).collect(Collectors.toList());
    }

    private List<Personinfo> hentAlleRelaterteBarn(Personinfo søkerPersonInfo) {
        return søkerPersonInfo.getFamilierelasjoner()
            .stream()
            .filter(r -> r.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(r -> personinfoAdapter.innhentSaksopplysningerForBarn(r.getPersonIdent()).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private RelasjonsRolleType utledRelasjonsrolleTilBarn(NavBrukerKjønn kjønn, RelasjonsRolleType rolle) {
        if (kjønn.equals(NavBrukerKjønn.KVINNE) && rolle.equals(RelasjonsRolleType.FARA)) {
            return RelasjonsRolleType.MEDMOR;
        }

        return NavBrukerKjønn.KVINNE.equals(kjønn) ? RelasjonsRolleType.MORA : RelasjonsRolleType.FARA;
    }

    private List<Personinfo> finnFellesBarn(Personinfo annenPart, Personinfo førstePart, List<LocalDateInterval> fødselsintervall) {
        List<PersonIdent> fnrAnnenPartsBarn = annenPart.getFamilierelasjoner().stream()
            .filter(f -> f.getRelasjonsrolle().equals(RelasjonsRolleType.BARN))
            .map(Familierelasjon::getPersonIdent)
            .collect(Collectors.toList());

        final List<Personinfo> barnPersonInfo = hentBarnRelatertTil(førstePart, fødselsintervall);

        return barnPersonInfo.stream()
            .filter(barn -> fnrAnnenPartsBarn.contains(barn.getPersonIdent()))
            .collect(Collectors.toList());
    }

}
