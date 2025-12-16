package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperFelles.mapAnnenForelder;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperFelles.mapRelasjonTilBarnet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.web.app.tjenester.registrering.ManuellRegistreringDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapper;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperFelles;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.GraderingDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OppholdDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.OverføringsperiodeDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtsettelseDto;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Rettigheter;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Dekningsgrad;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.ObjectFactory;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Dekningsgrader;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.MorsAktivitetsTyper;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Oppholdsaarsaker;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Overfoeringsaarsaker;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Utsettelsesaarsaker;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Uttaksperiodetyper;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Arbeidsgiver;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Fordeling;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Gradering;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.LukketPeriodeMedVedlegg;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Oppholdsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Overfoeringsperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Utsettelsesperiode;
import no.nav.vedtak.felles.xml.soeknad.uttak.v3.Uttaksperiode;
import no.nav.vedtak.felles.xml.soeknad.v3.Soeknad;

@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@BehandlingTypeRef
@ApplicationScoped
public class YtelseSøknadMapper implements SøknadMapper {

    private PersoninfoAdapter personinfoAdapter;
    private VirksomhetTjeneste virksomhetTjeneste;

    public YtelseSøknadMapper() {
    }

    @Inject
    public YtelseSøknadMapper(PersoninfoAdapter personinfoAdapter, VirksomhetTjeneste virksomhetTjeneste) {
        this.personinfoAdapter = personinfoAdapter;
        this.virksomhetTjeneste = virksomhetTjeneste;
    }

    @Override
    public <V extends ManuellRegistreringDto> Soeknad mapSøknad(V registreringDto, NavBruker navBruker) {
        var søknad = SøknadMapperFelles.mapSøknad(registreringDto, navBruker);

        var foreldrepenger = new ObjectFactory().createForeldrepenger();

        var dto = (ManuellRegistreringForeldrepengerDto)registreringDto;
        var søkersRelasjonTilBarnet = mapRelasjonTilBarnet(registreringDto); // Fødsel, termin, adopsjon eller omsorg
        foreldrepenger.setRelasjonTilBarnet(søkersRelasjonTilBarnet);
        foreldrepenger.setRettigheter(mapRettigheter(dto));
        foreldrepenger.setMedlemskap(SøknadMapperFelles.mapMedlemskap(registreringDto));
        foreldrepenger.setAnnenForelder(mapAnnenForelder(registreringDto, personinfoAdapter));
        foreldrepenger.setDekningsgrad(mapDekningsgrad(dto));
        foreldrepenger.setFordeling(mapFordeling(dto));
        foreldrepenger.setOpptjening(SøknadMapperFelles.mapOpptjening(dto, virksomhetTjeneste));

        var omYtelse = new no.nav.vedtak.felles.xml.soeknad.v3.ObjectFactory().createOmYtelse();
        omYtelse.getAny().add(new ObjectFactory().createForeldrepenger(foreldrepenger));
        søknad.setOmYtelse(omYtelse);
        søknad.setTilleggsopplysninger(registreringDto.getTilleggsopplysninger());

        return søknad;
    }

    static Dekningsgrad mapDekningsgrad(ManuellRegistreringForeldrepengerDto registreringDto) {
        if (isNull(registreringDto.getDekningsgrad())) {
            return null;
        }

        var dekningsgrad = new Dekningsgrad();
        var dekningsgrader = new Dekningsgrader();
        dekningsgrader.setKode(registreringDto.getDekningsgrad().getValue());
        dekningsgrad.setDekningsgrad(dekningsgrader);

        return dekningsgrad;
    }

    static Fordeling mapFordeling(ManuellRegistreringForeldrepengerDto registreringDto) {
        var fordeling = new Fordeling();
        fordeling.setAnnenForelderErInformert(registreringDto.getAnnenForelderInformert());

        var perioder = mapFordelingPerioder(registreringDto.getTidsromPermisjon(), registreringDto.getSøker());
        fordeling.getPerioder().addAll(perioder.stream().filter(Objects::nonNull).toList());

        return fordeling;
    }

    private static List<LukketPeriodeMedVedlegg> mapFordelingPerioder(TidsromPermisjonDto tidsromPermisjon, ForeldreType soker) {
        List<LukketPeriodeMedVedlegg> result = new ArrayList<>();
        if (!isNull(tidsromPermisjon)) {
            result.addAll(mapOverføringsperioder(tidsromPermisjon.getOverføringsperioder(), soker));
            result.addAll(mapUtsettelsesperioder(tidsromPermisjon.getUtsettelsePeriode()));
            result.addAll(mapUttaksperioder(tidsromPermisjon.getPermisjonsPerioder()));
            result.addAll(mapGraderingsperioder(tidsromPermisjon.getGraderingPeriode()));
            result.addAll(mapOppholdsperioder(tidsromPermisjon.getOppholdPerioder()));
        }
        return result;
    }

    static List<Uttaksperiode> mapUttaksperioder(List<PermisjonPeriodeDto> permisjonsPerioder) {
        List<Uttaksperiode> result = new ArrayList<>();
        if (!isNull(permisjonsPerioder)) {
            result.addAll(permisjonsPerioder.stream().map(YtelseSøknadMapper::mapPermisjonPeriodeDto).toList());
        }
        return result;
    }

    static List<Uttaksperiode> mapGraderingsperioder(List<GraderingDto> graderingsperioder) {
        List<Uttaksperiode> result = new ArrayList<>();

        if (!isNull(graderingsperioder)) {
            return graderingsperioder.stream().map(YtelseSøknadMapper::mapGraderingsperiode).toList();
        }
        return result;
    }

    static List<Oppholdsperiode> mapOppholdsperioder(List<OppholdDto> oppholdPerioder) {
        List<Oppholdsperiode> result = new ArrayList<>();

        if (!isNull(oppholdPerioder)) {
            return oppholdPerioder.stream().map(YtelseSøknadMapper::mapOppholdPeriode).toList();
        }
        return result;
    }

    private static Oppholdsperiode mapOppholdPeriode(OppholdDto oppholdDto) {
        var oppholdPeriode = new Oppholdsperiode();
        oppholdPeriode.setFom(oppholdDto.getPeriodeFom());
        oppholdPeriode.setTom(oppholdDto.getPeriodeTom());
        var oppholdsaarsaker = new Oppholdsaarsaker();
        oppholdsaarsaker.setKode(oppholdDto.getÅrsak().getKode());
        oppholdsaarsaker.setKodeverk(OppholdÅrsak.DISKRIMINATOR);
        oppholdPeriode.setAarsak(oppholdsaarsaker);
        return oppholdPeriode;
    }

    static Uttaksperiode mapPermisjonPeriodeDto(PermisjonPeriodeDto dto) {
        var uttaksperiode = new Uttaksperiode();
        uttaksperiode.setFom(dto.getPeriodeFom());
        uttaksperiode.setTom(dto.getPeriodeTom());
        uttaksperiode.setOenskerSamtidigUttak(dto.getHarSamtidigUttak());
        uttaksperiode.setOenskerFlerbarnsdager(dto.isFlerbarnsdager());

        if (dto.getSamtidigUttaksprosent() != null) {
            uttaksperiode.setSamtidigUttakProsent(dto.getSamtidigUttaksprosent().doubleValue());
        }

        var uttaksperiodetyper = new Uttaksperiodetyper();
        uttaksperiodetyper.setKode(dto.getPeriodeType().getKode());
        uttaksperiode.setType(uttaksperiodetyper);

        Optional.ofNullable(dto.getMorsAktivitet()).ifPresent(ma -> {
            var morsAktivitetsTyper = new MorsAktivitetsTyper();
            morsAktivitetsTyper.setKode(ma.getKode());
            uttaksperiode.setMorsAktivitetIPerioden(morsAktivitetsTyper);
        });

        return uttaksperiode;
    }

    static Uttaksperiode mapGraderingsperiode(GraderingDto dto) {
        var gradering = new Gradering();
        gradering.setArbeidsforholdSomSkalGraderes(TRUE.equals(dto.getSkalGraderes()));

        if (dto.getArbeidsgiverIdentifikator() != null) {
            var arbeidsgiver = mapArbeidsgiver(dto.getArbeidsgiverIdentifikator());
            gradering.setArbeidsgiver(arbeidsgiver);
        }

        if (!dto.isErArbeidstaker() && !dto.isErFrilanser() && !dto.isErSelvstNæringsdrivende()) {
            throw new IllegalArgumentException("Graderingsperioder må enten ha valgt at/fl/sn");
        }

        gradering.setFom(dto.getPeriodeFom());
        gradering.setTom(dto.getPeriodeTom());
        gradering.setArbeidtidProsent(dto.getProsentandelArbeid().doubleValue());
        gradering.setErArbeidstaker(dto.isErArbeidstaker());
        gradering.setErFrilanser(dto.isErFrilanser());
        gradering.setErSelvstNæringsdrivende(dto.isErSelvstNæringsdrivende());
        gradering.setOenskerSamtidigUttak(dto.getHarSamtidigUttak());
        gradering.setOenskerFlerbarnsdager(dto.isFlerbarnsdager());
        if (dto.getSamtidigUttaksprosent() != null) {
            gradering.setSamtidigUttakProsent(dto.getSamtidigUttaksprosent().doubleValue());
        }

        var uttaksperiodetyper = new Uttaksperiodetyper();
        uttaksperiodetyper.setKode(dto.getPeriodeForGradering().getKode());
        gradering.setType(uttaksperiodetyper);

        return gradering;
    }

    static Rettigheter mapRettigheter(ManuellRegistreringForeldrepengerDto registreringDto) {
        var tidsromPermisjon = registreringDto.getTidsromPermisjon();
        var annenForelder = registreringDto.getAnnenForelder();
        if (!isNull(tidsromPermisjon)) {
            var rettighet = new Rettigheter();
            if (!isNull(annenForelder)) {
                rettighet.setHarAleneomsorgForBarnet(annenForelder.getSøkerHarAleneomsorg());
                rettighet.setHarAnnenForelderRett(annenForelder.getDenAndreForelderenHarRettPåForeldrepenger());
                Optional.ofNullable(annenForelder.getMorMottarUføretrygd()).ifPresent(rettighet::setHarMorUforetrygd);
                Optional.ofNullable(annenForelder.getAnnenForelderRettEØS())
                    .ifPresent(rettighet::setHarAnnenForelderTilsvarendeRettEOS);
            }
            rettighet.setHarOmsorgForBarnetIPeriodene(true);
            return rettighet;
        }
        return null;
    }

    private static Arbeidsgiver mapArbeidsgiver(String arbeidsgiverIdentifikator) {
        Arbeidsgiver arbeidsgiver;
        if (PersonIdent.erGyldigFnr(arbeidsgiverIdentifikator)) {
            arbeidsgiver = new no.nav.vedtak.felles.xml.soeknad.uttak.v3.ObjectFactory().createPerson();
        } else {
            arbeidsgiver = new no.nav.vedtak.felles.xml.soeknad.uttak.v3.ObjectFactory().createVirksomhet();
        }
        arbeidsgiver.setIdentifikator(arbeidsgiverIdentifikator);
        return arbeidsgiver;
    }

    private static List<Utsettelsesperiode> mapUtsettelsesperioder(List<UtsettelseDto> utsettelserDto) {
        if (isNull(utsettelserDto)) {
            return new ArrayList<>();
        }
        return utsettelserDto.stream().map(YtelseSøknadMapper::mapUtsettelsesperiode).toList();
    }

    static Utsettelsesperiode mapUtsettelsesperiode(UtsettelseDto utsettelserDto) {
        var utsettelsesperiode = new Utsettelsesperiode();
        if (!isNull(utsettelserDto.getArsakForUtsettelse())) {
            var årsak = new Utsettelsesaarsaker();
            årsak.setKode(utsettelserDto.getArsakForUtsettelse().getKode());
            utsettelsesperiode.setAarsak(årsak);
        }

        if (!isNull(utsettelserDto.getPeriodeForUtsettelse())) {
            var uttaksperiodetyper = new Uttaksperiodetyper();
            uttaksperiodetyper.setKode(utsettelserDto.getPeriodeForUtsettelse().getKode());
            utsettelsesperiode.setUtsettelseAv(uttaksperiodetyper);
        }
        utsettelsesperiode.setFom(utsettelserDto.getPeriodeFom());
        utsettelsesperiode.setTom(utsettelserDto.getPeriodeTom());
        Optional.ofNullable(utsettelserDto.getMorsAktivitet()).ifPresent(ma -> {
            var morsAktivitetsTyper = new MorsAktivitetsTyper();
            morsAktivitetsTyper.setKode(ma.getKode());
            utsettelsesperiode.setMorsAktivitetIPerioden(morsAktivitetsTyper);
        });
        return utsettelsesperiode;
    }

    static List<Overfoeringsperiode> mapOverføringsperioder(List<OverføringsperiodeDto> overføringsperioder, ForeldreType søker) {
        return overføringsperioder.stream().map(p -> mapOverføringsperiode(p, søker)).toList();
    }

    static Overfoeringsperiode mapOverføringsperiode(OverføringsperiodeDto overføringsperiode, ForeldreType soker) {
        var overfoeringsperiode = new Overfoeringsperiode();
        var uttaksperiodetyper = new Uttaksperiodetyper();

        if (soker.equals(ForeldreType.MOR)) {
            uttaksperiodetyper.setKode(UttakPeriodeType.FEDREKVOTE.getKode());
        } else {
            uttaksperiodetyper.setKode(UttakPeriodeType.MØDREKVOTE.getKode());
        }
        overfoeringsperiode.setOverfoeringAv(uttaksperiodetyper);

        overfoeringsperiode.setFom(overføringsperiode.getPeriodeFom());
        overfoeringsperiode.setTom(overføringsperiode.getPeriodeTom());

        var årsak = new Overfoeringsaarsaker();
        årsak.setKode(overføringsperiode.getOverforingArsak().getKode());
        årsak.setKodeverk(OverføringÅrsak.DISKRIMINATOR);
        overfoeringsperiode.setAarsak(årsak);

        return overfoeringsperiode;
    }

}
