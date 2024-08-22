package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.Personopplysninger.PersonstatusPeriode;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.Personopplysninger.PersonstatusPeriode.Type;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.Personopplysninger.RegionPeriode;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemskapVurderingPeriodeTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
class MedlemRegelGrunnlagBygger {

    private MedlemTjeneste medlemTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private MedlemskapVurderingPeriodeTjeneste vurderingPeriodeTjeneste;

    @Inject
    MedlemRegelGrunnlagBygger(MedlemTjeneste medlemTjeneste,
                              PersonopplysningTjeneste personopplysningTjeneste,
                              MedlemskapVurderingPeriodeTjeneste vurderingPeriodeTjeneste) {
        this.medlemTjeneste = medlemTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.vurderingPeriodeTjeneste = vurderingPeriodeTjeneste;
    }

    MedlemRegelGrunnlagBygger() {
        //CDI
    }

    MedlemInngangsvilkårRegelGrunnlag lagRegelGrunnlagInngangsvilkår(BehandlingReferanse behandlingRef) {
        var vurderingsperiodeBosatt = vurderingPeriodeTjeneste.bosattVurderingsintervall(behandlingRef);
        var vurderingsperiodeLovligOpphold = vurderingPeriodeTjeneste.lovligOppholdVurderingsintervall(behandlingRef);
        var registrertMedlemskapPerioder = hentMedlemskapPerioder(behandlingRef);
        var opplysningsperiode = new LocalDateInterval(
            LocalDateInterval.min(vurderingsperiodeBosatt.getFomDato(), vurderingsperiodeLovligOpphold.getFomDato()),
            LocalDateInterval.max(vurderingsperiodeBosatt.getTomDato(), vurderingsperiodeLovligOpphold.getTomDato()));
        var personopplysningGrunnlag = hentPersonopplysninger(behandlingRef, opplysningsperiode);
        var søknad = hentSøknad(behandlingRef);

        return new MedlemInngangsvilkårRegelGrunnlag(vurderingsperiodeBosatt, vurderingsperiodeLovligOpphold, registrertMedlemskapPerioder,
            personopplysningGrunnlag, søknad);
    }

    MedlemFortsattRegelGrunnlag lagRegelGrunnlagFortsattMedlem(BehandlingReferanse behandlingRef) {
        var vurderingsperiode = vurderingPeriodeTjeneste.fortsattBosattVurderingsintervall(behandlingRef);
        var registrertMedlemskapPerioder = hentMedlemskapPerioder(behandlingRef);
        var opplysningsperiode = new LocalDateInterval(vurderingsperiode.getFomDato().minusDays(1), vurderingsperiode.getTomDato()); //minus 1 dag her for at fortsatt medlem må ha gjeldende personstatus ved start av vurderingsperioden
        var personopplysningGrunnlag = hentPersonopplysninger(behandlingRef, opplysningsperiode);

        return new MedlemFortsattRegelGrunnlag(vurderingsperiode, registrertMedlemskapPerioder, personopplysningGrunnlag);
    }

    private MedlemInngangsvilkårRegelGrunnlag.Søknad hentSøknad(BehandlingReferanse behandlingRef) {
        var utenlandsopphold = medlemTjeneste.hentMedlemskap(behandlingRef.behandlingId())
            .orElseThrow()
            .getOppgittTilknytning()
            .map(MedlemskapOppgittTilknytningEntitet::getOpphold)
            .orElse(Set.of())
            .stream()
            .filter(o -> o.getLand() != Landkoder.NOR)
            .map(o -> new LocalDateInterval(o.getPeriodeFom(), o.getPeriodeTom()))
            .collect(Collectors.toSet());
        return new MedlemInngangsvilkårRegelGrunnlag.Søknad(utenlandsopphold);
    }

    private Set<RegisterMedlemskapBeslutning> hentMedlemskapPerioder(BehandlingReferanse behandlingRef) {
        return medlemTjeneste.hentMedlemskap(behandlingRef.behandlingId())
            .orElseThrow()
            .getRegistrertMedlemskapPerioder()
            .stream()
            .map(p -> new RegisterMedlemskapBeslutning(new LocalDateInterval(p.getFom(), p.getTom()), p.getBeslutningsdato()))
            .collect(Collectors.toSet());
    }

    private Personopplysninger hentPersonopplysninger(BehandlingReferanse behandlingRef,
                                                      LocalDateInterval opplysningsperiode) {
        var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingRef,
            DatoIntervallEntitet.fraOgMedTilOgMed(opplysningsperiode.getFomDato(), opplysningsperiode.getTomDato())).orElseThrow();
        var aktørId = behandlingRef.aktørId();
        var regioner = personopplysningerAggregat.getStatsborgerskapRegionIInterval(aktørId, opplysningsperiode)
            .stream()
            .map(s -> new RegionPeriode(s.getLocalDateInterval(), map(s.getValue())))
            .collect(Collectors.toSet());
        var oppholdstillatelser = personopplysningerAggregat.getOppholdstillatelseFor(aktørId)
            .stream()
            .map(o -> map(o.getPeriode()))
            .collect(Collectors.toSet());
        var personstatus = personopplysningerAggregat.getPersonstatuserFor(aktørId).stream().map(this::map).collect(Collectors.toSet());
        var adresser = personopplysningerAggregat.getAdresserFor(behandlingRef.aktørId())
            .stream()
            .map(a -> new Personopplysninger.Adresse(map(a.getPeriode()), map(a.getAdresseType()), a.erUtlandskAdresse()))
            .collect(Collectors.toSet());
        return new Personopplysninger(regioner, oppholdstillatelser, personstatus, adresser);
    }

    private Personopplysninger.Region map(Region region) {
        return switch (region) {
            case NORDEN -> Personopplysninger.Region.NORDEN;
            case EOS -> Personopplysninger.Region.EØS;
            case TREDJELANDS_BORGER, UDEFINERT -> Personopplysninger.Region.TREDJELAND;
        };
    }

    private static Personopplysninger.Adresse.Type map(AdresseType adresseType) {
        return switch (adresseType) {
            case BOSTEDSADRESSE -> Personopplysninger.Adresse.Type.BOSTEDSADRESSE;
            case POSTADRESSE -> Personopplysninger.Adresse.Type.POSTADRESSE;
            case POSTADRESSE_UTLAND -> Personopplysninger.Adresse.Type.POSTADRESSE_UTLAND;
            case MIDLERTIDIG_POSTADRESSE_NORGE -> Personopplysninger.Adresse.Type.MIDLERTIDIG_POSTADRESSE_NORGE;
            case MIDLERTIDIG_POSTADRESSE_UTLAND -> Personopplysninger.Adresse.Type.MIDLERTIDIG_POSTADRESSE_UTLAND;
            case UKJENT_ADRESSE -> Personopplysninger.Adresse.Type.UKJENT_ADRESSE;
        };
    }

    private PersonstatusPeriode map(PersonstatusEntitet personstatus) {
        return new PersonstatusPeriode(map(personstatus.getPeriode()), switch (personstatus.getPersonstatus()) {
            case ADNR -> Type.D_NUMMER;
            case BOSA -> Type.BOSATT_ETTER_FOLKEREGISTERLOVEN;
            case DØD -> Type.DØD;
            case FOSV -> Type.FORSVUNNET;
            case FØDR, UTVA, UREG -> Type.IKKE_BOSATT;
            case UTPE -> Type.OPPHØRT;
            case UDEFINERT -> null;
            case UTAN -> throw new IllegalArgumentException("Ukjent status " + personstatus.getPersonstatus());
        });
    }

    private static LocalDateInterval map(DatoIntervallEntitet periode) {
        return new LocalDateInterval(periode.getFomDato(), periode.getTomDato());
    }
}
