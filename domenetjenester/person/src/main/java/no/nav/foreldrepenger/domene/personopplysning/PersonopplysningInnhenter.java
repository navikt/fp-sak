package no.nav.foreldrepenger.domene.personopplysning;

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
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.fpsak.tidsserie.LocalDateInterval;

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

    public List<FødtBarnInfo> innhentAlleFødteForIntervaller(FagsakYtelseType ytelseType, AktørId aktørId, List<LocalDateInterval> intervaller) {
        return personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(ytelseType, aktørId, intervaller);
    }

    public List<FødtBarnInfo> innhentAlleFødteForIntervaller(FagsakYtelseType ytelseType, RelasjonsRolleType rolleType,
                                                             AktørId aktørId, List<LocalDateInterval> intervaller) {
        return personinfoAdapter.innhentAlleFødteForBehandlingIntervaller(ytelseType, rolleType, aktørId, intervaller);
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

        barnSomSkalInnhentes.forEach(barn -> innhentPersonIdent(ytelseType, barn, true, innhentet));
        ektefelleSomSkalInnhentes.forEach(ekte -> innhentPersonIdent(ytelseType, ekte, false, innhentet));

        var søkerFødt = søkerPersonInfo.getFødselsdato();
        var ferskSøkerAktørId = søkerPersonInfo.getAktørId();

        // Historikk for søker
        var personhistorikkinfo = personinfoAdapter.innhentPersonopplysningerHistorikk(ytelseType, ferskSøkerAktørId, opplysningsperiode, søkerFødt);
        if (personhistorikkinfo != null) {
            mapAdresser(personhistorikkinfo.adressehistorikk(), informasjonBuilder, ferskSøkerAktørId);
            mapStatsborgerskap(personhistorikkinfo.statsborgerskaphistorikk(), informasjonBuilder, ferskSøkerAktørId);
            mapPersonstatus(personhistorikkinfo.personstatushistorikk(), informasjonBuilder, ferskSøkerAktørId);
            mapOppholdstillatelse(personhistorikkinfo.oppholdstillatelsehistorikk(), informasjonBuilder, ferskSøkerAktørId);
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

    private void mapPersonstatus(List<PersonstatusPeriode> personstatushistorikk, PersonInformasjonBuilder informasjonBuilder, AktørId aktørId) {
        for (var personstatus : personstatushistorikk) {
            var status = personstatus.personstatus();
            var periode = DatoIntervallEntitet.fraOgMedTilOgMed(personstatus.gyldighetsperiode().fom(), personstatus.gyldighetsperiode().tom());
            informasjonBuilder.leggTil(informasjonBuilder.getPersonstatusBuilder(aktørId, periode).medPersonstatus(status));
        }
    }

    private void mapOppholdstillatelse(List<OppholdstillatelsePeriode> oppholdshistorikk, PersonInformasjonBuilder informasjonBuilder, AktørId aktørId) {
        for (var tillatelse : oppholdshistorikk) {
            var type = tillatelse.tillatelse();
            var periode = DatoIntervallEntitet.fraOgMedTilOgMed(tillatelse.gyldighetsperiode().fom(), tillatelse.gyldighetsperiode().tom());

            informasjonBuilder.leggTil(informasjonBuilder.getOppholdstillatelseBuilder(aktørId, periode).medOppholdstillatelse(type));
        }
    }

    private void mapStatsborgerskap(List<StatsborgerskapPeriode> statsborgerskaphistorikk, PersonInformasjonBuilder informasjonBuilder, AktørId aktørId) {
        for (var statsborgerskap : statsborgerskaphistorikk) {
            var landkode = statsborgerskap.statsborgerskap();
            var periode = DatoIntervallEntitet.fraOgMedTilOgMed(statsborgerskap.gyldighetsperiode().fom(), statsborgerskap.gyldighetsperiode().tom());
            informasjonBuilder.leggTil(informasjonBuilder.getStatsborgerskapBuilder(aktørId, periode, landkode));
        }
    }

    private void mapAdresser(List<AdressePeriode> adressehistorikk, PersonInformasjonBuilder informasjonBuilder, AktørId aktørId) {
        for (var adresse : adressehistorikk) {
            var periode = DatoIntervallEntitet.fraOgMedTilOgMed(adresse.gyldighetsperiode().fom(), adresse.gyldighetsperiode().tom());
            var adresseBuilder = informasjonBuilder.getAdresseBuilder(aktørId, periode, adresse.adresse().getAdresseType())
                .medMatrikkelId(adresse.adresse().getMatrikkelId())
                .medAdresselinje1(adresse.adresse().getAdresselinje1())
                .medAdresselinje2(adresse.adresse().getAdresselinje2())
                .medAdresselinje3(adresse.adresse().getAdresselinje3())
                .medAdresselinje4(adresse.adresse().getAdresselinje4())
                .medLand(adresse.adresse().getLand())
                .medPostnummer(adresse.adresse().getPostnummer())
                .medPoststed(adresse.adresse().getPoststed());
            informasjonBuilder.leggTil(adresseBuilder);
        }
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
        var tilAdresser = til.getAdresseperioder().stream()
            .filter(ad -> AdresseType.BOSTEDSADRESSE.equals(ad.adresse().getAdresseType()))
            .toList();
        return fra.getAdresseperioder().stream()
            .filter(a -> AdresseType.BOSTEDSADRESSE.equals(a.adresse().getAdresseType()))
            .anyMatch(adr1 -> tilAdresser.stream().anyMatch(adr1::overlappMedLikAdresse));
    }

    private void mapInfoTilEntitet(Personinfo personinfo, PersonInformasjonBuilder informasjonBuilder, boolean lagreIHistoriskeTabeller) {
        var builder = informasjonBuilder.getPersonopplysningBuilder(personinfo.getAktørId())
            .medKjønn(personinfo.getKjønn())
            .medFødselsdato(personinfo.getFødselsdato())
            .medNavn(personinfo.getNavn())
            .medDødsdato(personinfo.getDødsdato())
            .medSivilstand(personinfo.getSivilstandType());
        informasjonBuilder.leggTil(builder);

        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttStatsborgerskapHistorikk(personinfo.getAktørId())) {
            mapStatsborgerskap(personinfo.getLandkoder(), informasjonBuilder, personinfo.getAktørId());
        }
        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttAdresseHistorikk(personinfo.getAktørId())) {
            mapAdresser(personinfo.getAdresseperioder(), informasjonBuilder, personinfo.getAktørId());
        }
        if (lagreIHistoriskeTabeller || informasjonBuilder.harIkkeFåttPersonstatusHistorikk(personinfo.getAktørId())) {
            mapPersonstatus(personinfo.getPersonstatus(), informasjonBuilder, personinfo.getAktørId());
        }
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
        var personinfo = personinfoAdapter.innhentPersonopplysningerFor(ytelseType, aktørId, true);
        personinfo.ifPresent(pi -> innhentet.put(pi.getPersonIdent(), pi));
        return personinfo;
    }

    private Optional<Personinfo> innhentPersonIdent(FagsakYtelseType ytelseType, PersonIdent ident, boolean erBarn, Map<PersonIdent, Personinfo> innhentet) {
        if (innhentet.get(ident) != null)
            return Optional.of(innhentet.get(ident));
        var personinfo = personinfoAdapter.innhentPersonopplysningerFor(ytelseType, ident, erBarn, false);
        personinfo.ifPresent(pi -> innhentet.put(pi.getPersonIdent(), pi));
        return personinfo;
    }
}
