package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.FamilierelasjonVL;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.AdressePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.OppholdstillatelsePeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.PersonstatusPeriode;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.StatsborgerskapPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class PersonopplysningInnhenter {

    private static final LocalDate FIKTIV_FOM = LocalDate.of(1900,1,1);

    private PersoninfoAdapter personinfoAdapter;

    PersonopplysningInnhenter() {
        // for CDI proxy
    }

    @Inject
    public PersonopplysningInnhenter(PersoninfoAdapter personinfoAdapter) {
        this.personinfoAdapter = personinfoAdapter;
    }

    public List<FødtBarnInfo> innhentAlleFødteForIntervaller(FagsakYtelseType ytelseType, AktørId aktørId, List<LocalDateInterval> intervaller) {
        return personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(ytelseType, aktørId, intervaller);
    }

    public Optional<PersonIdent> hentPersonIdentForAktør(AktørId aktørId) {
        return personinfoAdapter.hentFnr(aktørId);
    }

    public void innhentPersonopplysninger(FagsakYtelseType ytelseType, PersonInformasjonBuilder informasjonBuilder, AktørId søker, Optional<AktørId> annenPart,
                                          SimpleLocalDateInterval opplysningsperiode, List<FødtBarnInfo> filtrertFødselFREG) {

        // Fase 1 - Innhent persongalleri - søker, annenpart, relevante barn og ektefelle
        Map<PersonIdent, Personinfo> innhentet = new LinkedHashMap<>();
        var søkerPersonInfo = innhentAktørId(ytelseType, søker, innhentet)
            .orElseThrow(() -> new IllegalArgumentException("Finner ikke personinformasjon for aktør " + søker.getId()));

        var annenPartInfo = annenPart.flatMap(ap -> innhentAktørId(ytelseType, ap, innhentet));
        var annenPartsBarn = annenPartInfo.map(this::getAnnenPartsBarn).orElse(Set.of());

        var barnSomSkalInnhentes = finnBarnRelatertTil(filtrertFødselFREG);
        var ektefelleSomSkalInnhentes = finnEktefelle(søkerPersonInfo);

        barnSomSkalInnhentes.forEach(barn -> innhentPersonIdent(ytelseType, barn, innhentet));
        ektefelleSomSkalInnhentes.forEach(ekte -> innhentPersonIdent(ytelseType, ekte, innhentet));

        // Historikk for søker
        var personhistorikkinfo = personinfoAdapter.innhentPersonopplysningerHistorikk(ytelseType, søkerPersonInfo.getAktørId(), opplysningsperiode);
        if (personhistorikkinfo != null) {
            mapAdresser(personhistorikkinfo.getAdressehistorikk(), informasjonBuilder, søkerPersonInfo);
            mapStatsborgerskap(personhistorikkinfo.getStatsborgerskaphistorikk(), informasjonBuilder, søkerPersonInfo);
            mapPersonstatus(personhistorikkinfo.getPersonstatushistorikk(), informasjonBuilder, søkerPersonInfo);
            mapOppholdstillatelse(personhistorikkinfo.getOppholdstillatelsehistorikk(), informasjonBuilder, søkerPersonInfo);
        }

        // Fase 2 - mapping til
        mapInfoTilEntitet(søkerPersonInfo, informasjonBuilder, false);
        // Ektefelle
        if (!FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType)) {
            leggTilEktefelle(søkerPersonInfo, informasjonBuilder, innhentet);
        }

        // Medsøker (annen part). kan være samme person som Ektefelle
        annenPartInfo.ifPresent(ap -> mapInfoTilEntitet(ap, informasjonBuilder, true));

        barnSomSkalInnhentes.stream()
            .filter(barn -> innhentet.get(barn) != null)
            .forEach(barnIdent -> {
                var barn = innhentet.get(barnIdent);
                mapInfoTilEntitet(barn, informasjonBuilder, true);
                mapRelasjon(søkerPersonInfo, barn, RelasjonsRolleType.BARN, informasjonBuilder);
                mapRelasjon(barn, søkerPersonInfo, utledRelasjonsrolleTilBarn(søkerPersonInfo, barn), informasjonBuilder);
                if (annenPartsBarn.contains(barnIdent)) {
                    annenPartInfo.ifPresent(a -> mapRelasjon(a, barn, RelasjonsRolleType.BARN, informasjonBuilder));
                    annenPartInfo.ifPresent(a -> mapRelasjon(barn, a, utledRelasjonsrolleTilBarn(a, barn), informasjonBuilder));
                }
        });
    }

    private void mapPersonstatus(List<PersonstatusPeriode> personstatushistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        for (var personstatus : personstatushistorikk) {
            var status = personstatus.getPersonstatus();
            var periode = fødselsJustertPeriode(personstatus.getGyldighetsperiode().getFom(), personinfo.getFødselsdato(),
                personstatus.getGyldighetsperiode().getTom());

            informasjonBuilder.leggTil(informasjonBuilder.getPersonstatusBuilder(personinfo.getAktørId(), periode).medPersonstatus(status));
        }
    }

    private void mapOppholdstillatelse(List<OppholdstillatelsePeriode> oppholdshistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        for (var tillatelse : oppholdshistorikk) {
            var type = tillatelse.getTillatelse();
            var periode = fødselsJustertPeriode(tillatelse.getGyldighetsperiode().getFom(), FIKTIV_FOM, tillatelse.getGyldighetsperiode().getTom());

            informasjonBuilder.leggTil(informasjonBuilder.getOppholdstillatelseBuilder(personinfo.getAktørId(), periode).medOppholdstillatelse(type));
        }
    }

    private void mapStatsborgerskap(List<StatsborgerskapPeriode> statsborgerskaphistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        for (var statsborgerskap : statsborgerskaphistorikk) {
            var landkode = Landkoder.fraKode(statsborgerskap.getStatsborgerskap().getLandkode());

            var periode = fødselsJustertPeriode(statsborgerskap.getGyldighetsperiode().getFom(), personinfo.getFødselsdato(),
                statsborgerskap.getGyldighetsperiode().getTom());

            informasjonBuilder
                .leggTil(informasjonBuilder.getStatsborgerskapBuilder(personinfo.getAktørId(), periode, landkode));
        }
    }

    private void mapAdresser(List<AdressePeriode> adressehistorikk, PersonInformasjonBuilder informasjonBuilder, Personinfo personinfo) {
        var aktørId = personinfo.getAktørId();
        for (var adresse : adressehistorikk) {
            var periode = fødselsJustertPeriode(adresse.getGyldighetsperiode().getFom(), personinfo.getFødselsdato(),
                adresse.getGyldighetsperiode().getTom());
            var adresseBuilder = informasjonBuilder.getAdresseBuilder(aktørId, periode, adresse.getAdresse().getAdresseType())
                .medMatrikkelId(adresse.getAdresse().getMatrikkelId())
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

    private DatoIntervallEntitet fødselsJustertPeriode(LocalDate fom, LocalDate fødselsdato, LocalDate tom) {
        var brukFom = fom.isBefore(fødselsdato) ? fødselsdato : fom;
        var safeFom = tom != null && brukFom.isAfter(tom) ? tom : brukFom;
        return tom != null ? DatoIntervallEntitet.fraOgMedTilOgMed(safeFom, tom) : DatoIntervallEntitet.fraOgMed(safeFom);
    }

    private RelasjonsRolleType utledRelasjonsrolleTilBarn(Personinfo personinfo, Personinfo barn) {
        if (barn == null) {
            return RelasjonsRolleType.UDEFINERT;
        }
        return barn.getFamilierelasjoner().stream()
            .filter(fr -> fr.getPersonIdent().equals(personinfo.getPersonIdent()))
            .map(FamilierelasjonVL::getRelasjonsrolle)
            .filter(RelasjonsRolleType::erRegistrertForeldre)
            .findFirst().orElse(RelasjonsRolleType.UDEFINERT);
    }

    private void mapRelasjon(Personinfo fra, Personinfo til, RelasjonsRolleType rolle, PersonInformasjonBuilder informasjonBuilder) {
        if (til == null || rolle == null || RelasjonsRolleType.UDEFINERT.equals(rolle)) {
            return;
        }
        informasjonBuilder
            .leggTil(informasjonBuilder.getRelasjonBuilder(fra.getAktørId(), til.getAktørId(), rolle).harSammeBosted(utledSammeBosted(fra, til)));
    }

    private boolean utledSammeBosted(Personinfo fra, Personinfo til) {
        var tilAdresser = til.getAdresseInfoList().stream()
            .filter(ad -> AdresseType.BOSTEDSADRESSE.equals(ad.getGjeldendePostadresseType()))
            .toList();
        return fra.getAdresseInfoList().stream()
            .filter(a -> AdresseType.BOSTEDSADRESSE.equals(a.getGjeldendePostadresseType()))
            .anyMatch(adr1 -> tilAdresser.stream().anyMatch(adr2 -> Adresseinfo.likeAdresser(adr1, adr2)));
    }

    private void mapInfoTilEntitet(Personinfo personinfo, PersonInformasjonBuilder informasjonBuilder, boolean lagreIHistoriskeTabeller) {
        var periode = getPeriode(personinfo.getFødselsdato(), Tid.TIDENES_ENDE);
        var builder = informasjonBuilder.getPersonopplysningBuilder(personinfo.getAktørId())
            .medKjønn(personinfo.getKjønn())
            .medFødselsdato(personinfo.getFødselsdato())
            .medNavn(personinfo.getNavn())
            .medDødsdato(personinfo.getDødsdato())
            .medSivilstand(personinfo.getSivilstandType());
        informasjonBuilder.leggTil(builder);

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttStatsborgerskapHistorikk(personinfo.getAktørId())) {
            var prioritertStatsborgerskap = MapRegionLandkoder.finnRangertLandkode(personinfo.getLandkoder());
            informasjonBuilder
                .leggTil(informasjonBuilder.getStatsborgerskapBuilder(personinfo.getAktørId(), periode, prioritertStatsborgerskap));
        }

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttAdresseHistorikk(personinfo.getAktørId())) {
            for (var adresse : personinfo.getAdresseInfoList()) {
                var adresseBuilder = informasjonBuilder.getAdresseBuilder(personinfo.getAktørId(), periode, adresse.getGjeldendePostadresseType())
                    .medMatrikkelId(adresse.getMatrikkelId())
                    .medAdresselinje1(adresse.getAdresselinje1())
                    .medAdresselinje2(adresse.getAdresselinje2())
                    .medAdresselinje3(adresse.getAdresselinje3())
                    .medAdresselinje4(adresse.getAdresselinje4())
                    .medPostnummer(adresse.getPostNr())
                    .medPoststed(adresse.getPoststed())
                    .medLand(adresse.getLand());
                informasjonBuilder.leggTil(adresseBuilder);
            }
        }

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttPersonstatusHistorikk(personinfo.getAktørId())) {
            informasjonBuilder
                .leggTil(informasjonBuilder.getPersonstatusBuilder(personinfo.getAktørId(), periode).medPersonstatus(personinfo.getPersonstatus()));
        }
    }

    private DatoIntervallEntitet getPeriode(LocalDate fom, LocalDate tom) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom != null ? tom : Tid.TIDENES_ENDE);
    }

    private void leggTilEktefelle(Personinfo søkerPersonInfo, PersonInformasjonBuilder informasjonBuilder, Map<PersonIdent, Personinfo> innhentet) {
        søkerPersonInfo.getFamilierelasjoner().stream()
            .filter(f -> f.getRelasjonsrolle().equals(RelasjonsRolleType.EKTE) ||
                f.getRelasjonsrolle().equals(RelasjonsRolleType.REGISTRERT_PARTNER))
            .filter(ekte -> innhentet.get(ekte.getPersonIdent()) != null)
            .forEach(relasjon -> {
                var ekte = innhentet.get(relasjon.getPersonIdent());
                mapInfoTilEntitet(ekte, informasjonBuilder, true);
                mapRelasjon(søkerPersonInfo, ekte, relasjon.getRelasjonsrolle(), informasjonBuilder);
                mapRelasjon(ekte, søkerPersonInfo, relasjon.getRelasjonsrolle(), informasjonBuilder);
            });
    }

    private Set<PersonIdent> finnEktefelle(Personinfo personinfo) {
        return personinfo.getFamilierelasjoner().stream()
            .filter(f -> f.getRelasjonsrolle().equals(RelasjonsRolleType.EKTE) ||
                f.getRelasjonsrolle().equals(RelasjonsRolleType.REGISTRERT_PARTNER))
            .map(FamilierelasjonVL::getPersonIdent)
            .collect(Collectors.toSet());
    }

    private Set<PersonIdent> finnBarnRelatertTil(List<FødtBarnInfo> filtrertFødselFREG) {
        return filtrertFødselFREG.stream()
            .map(FødtBarnInfo::ident)
            .filter(Objects::nonNull) // Dødfødsel
            .collect(Collectors.toSet());
    }

    private Set<PersonIdent> getAnnenPartsBarn(Personinfo annenPartInfo) {
        return annenPartInfo.getFamilierelasjoner().stream()
            .filter(f -> RelasjonsRolleType.BARN.equals(f.getRelasjonsrolle()))
            .map(FamilierelasjonVL::getPersonIdent)
            .collect(Collectors.toSet());
    }

    private Optional<Personinfo> innhentAktørId(FagsakYtelseType ytelseType, AktørId aktørId, Map<PersonIdent, Personinfo> innhentet) {
        var personinfo = personinfoAdapter.innhentPersonopplysningerFor(ytelseType, aktørId);
        personinfo.ifPresent(pi -> innhentet.put(pi.getPersonIdent(), pi));
        return personinfo;
    }

    private Optional<Personinfo> innhentPersonIdent(FagsakYtelseType ytelseType, PersonIdent ident, Map<PersonIdent, Personinfo> innhentet) {
        if (innhentet.get(ident) != null)
            return Optional.of(innhentet.get(ident));
        var personinfo = personinfoAdapter.innhentPersonopplysningerFor(ytelseType, ident);
        personinfo.ifPresent(pi -> innhentet.put(pi.getPersonIdent(), pi));
        return personinfo;
    }
}
