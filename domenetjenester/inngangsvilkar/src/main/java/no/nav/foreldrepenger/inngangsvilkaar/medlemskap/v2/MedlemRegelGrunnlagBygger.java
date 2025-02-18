package no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonstatusIntervall;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemskapVurderingPeriodeTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.v2.MedlemskapsvilkårGrunnlag;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medlemskap.v2.Personopplysninger;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@ApplicationScoped
public class MedlemRegelGrunnlagBygger {

    private MedlemTjeneste medlemTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private MedlemskapVurderingPeriodeTjeneste vurderingPeriodeTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private SatsRepository satsRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private PersoninfoAdapter personinfoAdapter;

    @Inject
    public MedlemRegelGrunnlagBygger(MedlemTjeneste medlemTjeneste,
                              PersonopplysningTjeneste personopplysningTjeneste,
                              MedlemskapVurderingPeriodeTjeneste vurderingPeriodeTjeneste,
                              InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                              SatsRepository satsRepository,
                              SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                              PersoninfoAdapter personinfoAdapter) {
        this.medlemTjeneste = medlemTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.vurderingPeriodeTjeneste = vurderingPeriodeTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.satsRepository = satsRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.personinfoAdapter = personinfoAdapter;
    }

    MedlemRegelGrunnlagBygger() {
        //CDI
    }

    MedlemskapsvilkårGrunnlag lagRegelGrunnlag(BehandlingReferanse behandlingRef) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingRef.behandlingId());
        var vurderingsperiodeBosatt = vurderingPeriodeTjeneste.bosattVurderingsintervall(behandlingRef, skjæringstidspunkt);
        var vurderingsperiodeLovligOpphold = vurderingPeriodeTjeneste.lovligOppholdVurderingsintervall(behandlingRef, skjæringstidspunkt);
        var registrertMedlemskapPerioder = hentMedlemskapPerioder(behandlingRef);
        var opplysningsperiode = SimpleLocalDateInterval.fraOgMedTomNotNull(
            LocalDateInterval.min(vurderingsperiodeBosatt.getFomDato(), vurderingsperiodeLovligOpphold.getFomDato()),
            LocalDateInterval.max(vurderingsperiodeBosatt.getTomDato(), vurderingsperiodeLovligOpphold.getTomDato()));
        var personopplysningGrunnlag = hentPersonopplysninger(behandlingRef, skjæringstidspunkt, opplysningsperiode);
        var søknad = hentSøknad(behandlingRef);
        var arbeid = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingRef.behandlingId())
            .map(iay -> new MedlemskapsvilkårGrunnlag.Arbeid(hentAnsettelsePerioder(iay, behandlingRef), hentInntekt(iay, behandlingRef)))
            .orElse(new MedlemskapsvilkårGrunnlag.Arbeid(Set.of(), Set.of()));
        var utledetSkjæringstidspunkt = skjæringstidspunkt.getUtledetSkjæringstidspunkt();
        var behandlingsdato = LocalDate.now();
        var grunnbeløp = new MedlemskapsvilkårGrunnlag.Beløp(
            BigDecimal.valueOf(satsRepository.finnGjeldendeSats(BeregningSatsType.GRUNNBELØP).getVerdi()));

        return new MedlemskapsvilkårGrunnlag(vurderingsperiodeBosatt, vurderingsperiodeLovligOpphold, registrertMedlemskapPerioder,
            personopplysningGrunnlag, søknad, arbeid, utledetSkjæringstidspunkt, behandlingsdato, grunnbeløp);
    }

    private static Set<LocalDateInterval> hentAnsettelsePerioder(InntektArbeidYtelseGrunnlag iay, BehandlingReferanse referanse) {
        var filter = new YrkesaktivitetFilter(iay.getArbeidsforholdInformasjon(), iay.getAktørArbeidFraRegister(referanse.aktørId()));

        return filter.getAnsettelsesPerioder()
            .stream()
            .map(ap -> new LocalDateInterval(ap.getPeriode().getFomDato(), ap.getPeriode().getTomDato()))
            .collect(Collectors.toSet());
    }

    private static Set<MedlemskapsvilkårGrunnlag.Arbeid.Inntekt> hentInntekt(InntektArbeidYtelseGrunnlag iay, BehandlingReferanse referanse) {
        var inntektFilter = new InntektFilter(iay.getAktørInntektFraRegister(referanse.aktørId()));
        return inntektFilter.getInntektsposterPensjonsgivende()
            .stream()
            .map(p -> new MedlemskapsvilkårGrunnlag.Arbeid.Inntekt(map(p.getPeriode()), new MedlemskapsvilkårGrunnlag.Beløp(p.getBeløp().getVerdi())))
            .collect(Collectors.toSet());
    }

    private MedlemskapsvilkårGrunnlag.Søknad hentSøknad(BehandlingReferanse behandlingRef) {
        var utenlandsopphold = medlemTjeneste.hentMedlemskap(behandlingRef.behandlingId())
            .orElseThrow()
            .getOppgittTilknytning()
            .map(MedlemskapOppgittTilknytningEntitet::getOpphold)
            .orElse(Set.of())
            .stream()
            .filter(o -> o.getLand() != Landkoder.NOR)
            .map(o -> new LocalDateInterval(o.getPeriodeFom(), o.getPeriodeTom()))
            .collect(Collectors.toSet());
        return new MedlemskapsvilkårGrunnlag.Søknad(utenlandsopphold);
    }

    private Set<LocalDateInterval> hentMedlemskapPerioder(BehandlingReferanse behandlingRef) {
        return medlemTjeneste.hentMedlemskap(behandlingRef.behandlingId())
            .orElseThrow()
            .getRegistrertMedlemskapPerioder()
            .stream()
            .map(p -> new LocalDateInterval(p.getFom(), p.getTom()))
            .collect(Collectors.toSet());
    }

    private Personopplysninger hentPersonopplysninger(BehandlingReferanse behandlingRef,
                                                      Skjæringstidspunkt stp,
                                                      AbstractLocalDateInterval opplysningsperiode) {
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(behandlingRef).orElseThrow();
        var aktørId = behandlingRef.aktørId();
        var regioner = personopplysningerAggregat.getStatsborgerskapRegionIInterval(aktørId, opplysningsperiode, stp.getUtledetSkjæringstidspunkt())
            .stream()
            .map(s -> new Personopplysninger.RegionPeriode(s.getLocalDateInterval(), map(s.getValue())))
            .collect(Collectors.toSet());
        var oppholdstillatelser = personopplysningerAggregat.getOppholdstillatelseFor(aktørId, opplysningsperiode)
            .stream()
            .filter(o -> !OppholdstillatelseType.UDEFINERT.equals(o.getTillatelse()))
            .map(o -> map(o.getPeriode()))
            .collect(Collectors.toSet());
        var personstatus = personopplysningerAggregat.getPersonstatuserFor(aktørId, opplysningsperiode)
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());
        var adresser = personopplysningerAggregat.getAdresserFor(behandlingRef.aktørId(), opplysningsperiode)
            .stream()
            .map(a -> new Personopplysninger.Adresse(map(a.getPeriode()), map(a.getAdresseType()), a.erUtlandskAdresse()))
            .collect(Collectors.toSet());
        var personIdentType = finnPersonIdentType(aktørId);
        return new Personopplysninger(regioner, oppholdstillatelser, personstatus, adresser, personIdentType);
    }

    private Personopplysninger.PersonIdentType finnPersonIdentType(AktørId aktørId) {
        return personinfoAdapter.hentFnr(aktørId)
            .filter(p -> PersonIdent.erGyldigFnr(p.getIdent()) && !p.erDnr())
            .map(p -> Personopplysninger.PersonIdentType.FNR)
            .orElse(Personopplysninger.PersonIdentType.DNR);
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
            case BOSTEDSADRESSE, BOSTEDSADRESSE_UTLAND -> Personopplysninger.Adresse.Type.BOSTEDSADRESSE;
            case POSTADRESSE -> Personopplysninger.Adresse.Type.KONTAKTADRESSE;
            case POSTADRESSE_UTLAND -> Personopplysninger.Adresse.Type.KONTAKTADRESSE_UTLAND;
            case MIDLERTIDIG_POSTADRESSE_NORGE -> Personopplysninger.Adresse.Type.OPPHOLDSADRESSE_NORGE;
            case MIDLERTIDIG_POSTADRESSE_UTLAND -> Personopplysninger.Adresse.Type.OPPHOLDSADRESSE_UTLAND;
            case UKJENT_ADRESSE -> Personopplysninger.Adresse.Type.UKJENT_ADRESSE;
        };
    }

    private Personopplysninger.PersonstatusPeriode map(PersonstatusIntervall personstatus) {
        return new Personopplysninger.PersonstatusPeriode(personstatus.intervall(), switch (personstatus.personstatus()) {
            case ADNR -> Personopplysninger.PersonstatusPeriode.Type.D_NUMMER;
            case BOSA -> Personopplysninger.PersonstatusPeriode.Type.BOSATT_ETTER_FOLKEREGISTERLOVEN;
            case DØD -> Personopplysninger.PersonstatusPeriode.Type.DØD;
            case FOSV -> Personopplysninger.PersonstatusPeriode.Type.FORSVUNNET;
            case FØDR, UTVA, UREG, UDEFINERT -> Personopplysninger.PersonstatusPeriode.Type.IKKE_BOSATT;
            case UTPE -> Personopplysninger.PersonstatusPeriode.Type.OPPHØRT;
        });
    }

    private static LocalDateInterval map(DatoIntervallEntitet periode) {
        return new LocalDateInterval(periode.getFomDato(), periode.getTomDato());
    }
}
