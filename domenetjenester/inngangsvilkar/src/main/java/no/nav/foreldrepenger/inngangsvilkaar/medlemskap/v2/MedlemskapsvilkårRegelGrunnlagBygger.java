package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;

import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapsvilkårRegelGrunnlag.Personopplysninger.PersonstatusPeriode;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapsvilkårRegelGrunnlag.Personopplysninger.PersonstatusPeriode.Type;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapsvilkårRegelGrunnlag.Personopplysninger.Region;
import static no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapsvilkårRegelGrunnlag.Personopplysninger.RegionPeriode;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusEntitet;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.MapRegionLandkoder;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
class MedlemskapsvilkårRegelGrunnlagBygger {

    private MedlemTjeneste medlemTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    MedlemskapsvilkårRegelGrunnlagBygger(MedlemTjeneste medlemTjeneste, PersonopplysningTjeneste personopplysningTjeneste) {
        this.medlemTjeneste = medlemTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    MedlemskapsvilkårRegelGrunnlagBygger() {
        //CDI
    }

    MedlemskapsvilkårRegelGrunnlag lagRegelGrunnlag(BehandlingReferanse behandlingRef) {
        var vurderingsperiodeBosatt = utledVurderingsperiodeBosatt(behandlingRef);
        var vurderingsperiodeLovligOpphold = utledVurderingsperiodeLovligOpphold(behandlingRef);
        var registrertMedlemskapPerioder = hentMedlemskapPerioder(behandlingRef);
        var opplysningsperiode = new LocalDateInterval(
            LocalDateInterval.min(vurderingsperiodeBosatt.getFomDato(), vurderingsperiodeLovligOpphold.getFomDato()),
            LocalDateInterval.max(vurderingsperiodeBosatt.getTomDato(), vurderingsperiodeLovligOpphold.getTomDato()));
        var personopplysningGrunnlag = hentPersonopplysninger(behandlingRef, opplysningsperiode);
        var søknad = hentSøknad(behandlingRef);

        return new MedlemskapsvilkårRegelGrunnlag(vurderingsperiodeBosatt, vurderingsperiodeLovligOpphold, registrertMedlemskapPerioder, personopplysningGrunnlag, søknad);
    }

    // TODO
    private LocalDateInterval utledVurderingsperiodeLovligOpphold(BehandlingReferanse behandlingRef) {
        var fom = behandlingRef.skjæringstidspunkt().getUtledetSkjæringstidspunkt();
        var tom = behandlingRef.skjæringstidspunkt().getUtledetSkjæringstidspunkt();
        return new LocalDateInterval(fom, tom);
    }

    // TODO
    private LocalDateInterval utledVurderingsperiodeBosatt(BehandlingReferanse behandlingRef) {
        var fom = behandlingRef.skjæringstidspunkt().getUtledetSkjæringstidspunkt();
        var tom = behandlingRef.skjæringstidspunkt().getUtledetSkjæringstidspunkt();
        return new LocalDateInterval(fom, tom);
    }

    private MedlemskapsvilkårRegelGrunnlag.Søknad hentSøknad(BehandlingReferanse behandlingRef) {
        var utenlandsopphold = medlemTjeneste.hentMedlemskap(behandlingRef.behandlingId())
            .orElseThrow()
            .getOppgittTilknytning()
            .map(MedlemskapOppgittTilknytningEntitet::getOpphold)
            .orElse(Set.of())
            .stream()
            .filter(o -> o.getLand() != Landkoder.NOR)
            .map(o -> new LocalDateInterval(o.getPeriodeFom(), o.getPeriodeTom()))
            .collect(Collectors.toSet());
        return new MedlemskapsvilkårRegelGrunnlag.Søknad(utenlandsopphold);
    }

    private Set<LocalDateInterval> hentMedlemskapPerioder(BehandlingReferanse behandlingRef) {
        return medlemTjeneste.hentMedlemskap(behandlingRef.behandlingId())
            .orElseThrow()
            .getRegistrertMedlemskapPerioder()
            .stream()
            .map(p -> new LocalDateInterval(p.getFom(), p.getTom()))
            .collect(Collectors.toSet());
    }

    private MedlemskapsvilkårRegelGrunnlag.Personopplysninger hentPersonopplysninger(BehandlingReferanse behandlingRef,
                                                                                     LocalDateInterval opplysningsperiode) {
        var personopplysningerAggregat = personopplysningTjeneste.hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingRef,
                DatoIntervallEntitet.fraOgMedTilOgMed(opplysningsperiode.getFomDato(), opplysningsperiode.getTomDato()))
            .orElseThrow();
        var aktørId = behandlingRef.aktørId();
        var regioner = personopplysningerAggregat.getStatsborgerskap(aktørId)
            .stream()
            .map(s -> new RegionPeriode(map(s.getPeriode()), tilRegion(s.getStatsborgerskap())))
            .collect(Collectors.toSet());
        var oppholdstillatelser = personopplysningerAggregat.getOppholdstillatelseFor(aktørId)
            .stream()
            .map(o -> map(o.getPeriode()))
            .collect(Collectors.toSet());
        var personstatus = personopplysningerAggregat.getPersonstatuserFor(aktørId).stream().map(this::map).collect(Collectors.toSet());
        var adresser = personopplysningerAggregat.getAdresserFor(behandlingRef.aktørId())
            .stream()
            .map(a -> new MedlemskapsvilkårRegelGrunnlag.Adresse(map(a.getPeriode()), map(a.getAdresseType()), a.erUtlandskAdresse()))
            .collect(Collectors.toSet());
        return new MedlemskapsvilkårRegelGrunnlag.Personopplysninger(regioner, oppholdstillatelser, personstatus, adresser);
    }

    private static MedlemskapsvilkårRegelGrunnlag.Adresse.Type map(AdresseType adresseType) {
        return switch (adresseType) {
            case BOSTEDSADRESSE -> MedlemskapsvilkårRegelGrunnlag.Adresse.Type.BOSTED;
            default -> MedlemskapsvilkårRegelGrunnlag.Adresse.Type.ANNEN;
        };
    }

    private PersonstatusPeriode map(PersonstatusEntitet personstatus) {
        return new PersonstatusPeriode(map(personstatus.getPeriode()), switch (personstatus.getPersonstatus()) {
            case ADNR -> Type.D_NUMMER;
            case BOSA -> Type.BOSATT;
            case DØD -> Type.DØD;
            case FOSV -> Type.FORSVUNNET;
            case FØDR -> Type.FØDSELREGISTRERT;
            case UREG -> Type.UREGISTRERT;
            case UTAN -> Type.UTGÅTT_ANNULLERT;
            case UTPE -> Type.UTGÅTT;
            case UTVA -> Type.UTVANDRET;
            case UDEFINERT -> null;
        });
    }

    private static Region tilRegion(Landkoder landkode) {
        return switch (MapRegionLandkoder.mapLandkode(landkode)) {
            case NORDEN -> Region.NORDEN;
            case EOS -> Region.EØS;
            case TREDJELANDS_BORGER, UDEFINERT -> Region.TREDJELAND;
        };
    }

    private static LocalDateInterval map(DatoIntervallEntitet periode) {
        return new LocalDateInterval(periode.getFomDato(), periode.getTomDato());
    }
}
