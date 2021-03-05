package no.nav.foreldrepenger.web.app.tjenester.registrering;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.dto.AndreYtelserDto;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.dto.NaringsvirksomhetTypeDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.AnnenForelderDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.ArbeidsforholdDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.EgenVirksomhetDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.FrilansDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.MedInntektArbeidYtelseRegistrering;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.RettigheterDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.UtenlandsoppholdDto;
import no.nav.foreldrepenger.web.app.tjenester.registrering.dto.VirksomhetDto;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Adopsjon;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.AnnenForelder;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.AnnenForelderMedNorskIdent;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.AnnenForelderUtenNorskIdent;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Bruker;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Foedsel;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Medlemskap;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Omsorgsovertakelse;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.OppholdNorge;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.OppholdUtlandet;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Periode;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.SoekersRelasjonTilBarnet;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Termin;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.UkjentForelder;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.AnnenOpptjening;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.EgenNaering;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Frilans;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Frilansoppdrag;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.NorskOrganisasjon;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Opptjening;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Regnskapsfoerer;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.UtenlandskArbeidsforhold;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.UtenlandskOrganisasjon;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.AnnenOpptjeningTyper;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Brukerroller;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Land;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Omsorgsovertakelseaarsaker;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Spraakkode;
import no.nav.vedtak.felles.xml.soeknad.kodeverk.v3.Virksomhetstyper;
import no.nav.vedtak.felles.xml.soeknad.v3.Soeknad;
import no.nav.vedtak.konfig.Tid;
import no.nav.vedtak.util.StringUtils;

public class SøknadMapperFelles {

    private SøknadMapperFelles() {
    }

    public static Soeknad mapSøknad(ManuellRegistreringDto registreringDto, NavBruker navBruker) {
        Soeknad søknad = new Soeknad();
        søknad.setMottattDato(registreringDto.getMottattDato());
        søknad.setSoeker(mapBruker(registreringDto.getSoker(), navBruker)); //Stønadstype Søker fnr og søkertype(mor/far/annen/medmor)
        søknad.setTilleggsopplysninger(registreringDto.getTilleggsopplysninger());
        søknad.setSprakvalg(mapSpråk(registreringDto.getSpråkkode()));
        søknad.setBegrunnelseForSenSoeknad(registreringDto.getBegrunnelse());
        return søknad;
    }

    public static AnnenForelder mapAnnenForelder(ManuellRegistreringDto registreringDto, PersoninfoAdapter personinfoAdapter) {
        AnnenForelderDto annenForelderDto = registreringDto.getAnnenForelder();
        if (annenForelderDto == null) {
            return null;
        }
        if (TRUE.equals(annenForelderDto.getKanIkkeOppgiAnnenForelder())) {
            AnnenForelderDto.KanIkkeOppgiBegrunnelse oppgittBegrunnelse = annenForelderDto.getKanIkkeOppgiBegrunnelse();
            String utenlandskFoedselsnummer = oppgittBegrunnelse.getUtenlandskFoedselsnummer();
            if (!StringUtils.isBlank(utenlandskFoedselsnummer)) {
                AnnenForelderUtenNorskIdent annenForelderUtenNorskIdent = new AnnenForelderUtenNorskIdent();
                annenForelderUtenNorskIdent.setUtenlandskPersonidentifikator(utenlandskFoedselsnummer);
                if (!StringUtils.isBlank(oppgittBegrunnelse.getLand())) {
                    annenForelderUtenNorskIdent.setLand(getLandkode(oppgittBegrunnelse.getLand()));
                }
                return annenForelderUtenNorskIdent;
            } else {
                return new UkjentForelder();
            }
        }

        AnnenForelderMedNorskIdent annenForelderMedNorskIdent = new AnnenForelderMedNorskIdent();
        AktørId aktørId = personinfoAdapter.hentAktørForFnr(PersonIdent.fra(annenForelderDto.getFoedselsnummer()))
            .orElseThrow(() -> new TekniskException("FP-453257", "Fant ikke aktør-ID for fødselsnummer"));
        annenForelderMedNorskIdent.setAktoerId(aktørId.getId());

        return annenForelderMedNorskIdent;

    }

    private static List<OppholdNorge> opprettOppholdNorge(final LocalDate mottattDato, boolean fremtidigOppholdNorge, boolean tidligereOppholdNorge) {
        List<OppholdNorge> oppholdNorgeListe = new ArrayList<>();
        if (nonNull(mottattDato)) {
            if (tidligereOppholdNorge) {
                OppholdNorge oppholdNorgeSistePeriode = new OppholdNorge();
                Periode periode = new Periode();
                periode.setFom(mottattDato.minusYears(1));
                periode.setTom(mottattDato);
                oppholdNorgeSistePeriode.setPeriode(periode);
                oppholdNorgeListe.add(oppholdNorgeSistePeriode);
            }
            if (fremtidigOppholdNorge) {
                OppholdNorge oppholdNorgeNestePeriode = new OppholdNorge();
                Periode periode = new Periode();
                periode.setFom(mottattDato);
                periode.setTom(mottattDato.plusYears(1));
                oppholdNorgeNestePeriode.setPeriode(periode);
                oppholdNorgeListe.add(oppholdNorgeNestePeriode);
            }
        }

        return oppholdNorgeListe;
    }

    static Bruker mapBruker(ForeldreType søker, NavBruker navBruker) {
        Bruker bruker = new Bruker();
        bruker.setAktoerId(navBruker.getAktørId().getId());
        Brukerroller brukerroller = new Brukerroller();

        //TODO PFP-8742 Bør sees på ifm adopsjon PFP-334 og stebarnsadopsjon PFP-182
        String brukerRolleKode = "ANDRE".equals(søker.getKode()) ? "IKKE_RELEVANT" : søker.getKode();

        brukerroller.setKode(brukerRolleKode);
        bruker.setSoeknadsrolle(brukerroller);
        return bruker;
    }

    private static Spraakkode mapSpråk(Språkkode språkkode) {
        if (null == språkkode){
            return null;
        }
        Spraakkode spraakkode = new Spraakkode();
        spraakkode.setKode(språkkode.getKode());
        spraakkode.setKodeverk(språkkode.getKodeverk());
        return spraakkode;
    }

    public static SoekersRelasjonTilBarnet mapRelasjonTilBarnet(ManuellRegistreringDto registreringDto) {
        // Hvis det er gjort et valg av rettigheter knyttet til omsorgovertakelse for far skal saken opprettes som en omsorgsovertakelse.
        boolean rettigheterRelatertTilOmsorgErSatt = registreringDto.getRettigheter() != null
            && !RettigheterDto.MANN_ADOPTERER_ALENE.equals(registreringDto.getRettigheter());
        if (rettigheterRelatertTilOmsorgErSatt) {
            return mapOmsorgsovertakelse(registreringDto);
        }
        //SøkersRelasjonTilBarnet = adopsjon, fødsel, termin eller omsorg
        FamilieHendelseType tema = registreringDto.getTema();
        if (erSøknadVedAdopsjon(tema)) {
            return mapAdopsjon(registreringDto);
        } else if (erSøknadVedFødsel(registreringDto.getErBarnetFodt(), registreringDto.getTema())) {
            return mapFødsel(registreringDto);
        } else if (erSøknadVedTermin(registreringDto.getErBarnetFodt(), registreringDto.getTema())) {
            return mapTermin(registreringDto);
        } else {
            throw new IllegalArgumentException(String.format("Ugyldig temakode: %s ", tema));
        }
    }

    static Foedsel mapFødsel(ManuellRegistreringDto registreringDto) {
        Foedsel fødsel = new Foedsel();
        if (harFødselsdato(registreringDto)) {
            List<LocalDate> foedselsDato = registreringDto.getFoedselsDato();
            if (foedselsDato.size() != 1) {
                throw new IllegalArgumentException("Støtter bare 1 fødselsdato på fødsel");
            }
            fødsel.setFoedselsdato(registreringDto.getFoedselsDato().get(0));
            fødsel.setTermindato(registreringDto.getTermindato());
        }
        fødsel.setAntallBarn(registreringDto.getAntallBarn());
        return fødsel;
    }

    private static List<OppholdUtlandet> mapUtenlandsopphold(List<UtenlandsoppholdDto> utenlandsopphold) {
        List<OppholdUtlandet> utenlandsoppholdListe = new ArrayList<>();
        for (UtenlandsoppholdDto utenlandsoppholdDto : utenlandsopphold) {
            OppholdUtlandet nyttOpphold = new OppholdUtlandet();
            nyttOpphold.setLand(getLandkode(utenlandsoppholdDto.getLand()));
            Periode periode = new Periode();
            periode.setFom(utenlandsoppholdDto.getPeriodeFom());
            periode.setTom(utenlandsoppholdDto.getPeriodeTom());
            nyttOpphold.setPeriode(periode);
            utenlandsoppholdListe.add(nyttOpphold);
        }
        return utenlandsoppholdListe;
    }

    static Termin mapTermin(ManuellRegistreringDto registreringDto) {
        Termin termin = new Termin();
        if (harTermindato(registreringDto)) {
            termin.setTermindato(registreringDto.getTermindato());
            termin.setAntallBarn(registreringDto.getAntallBarnFraTerminbekreftelse());
            termin.setUtstedtdato(registreringDto.getTerminbekreftelseDato());
        }
        return termin;
    }

    static Adopsjon mapAdopsjon(ManuellRegistreringDto registreringDto) {
        Adopsjon adopsjon = new Adopsjon();
        adopsjon.setOmsorgsovertakelsesdato(registreringDto.getOmsorg().getOmsorgsovertakelsesdato());
        adopsjon.setAnkomstdato(registreringDto.getOmsorg().getAnkomstdato());
        List<LocalDate> foedselsdatoer = registreringDto.getOmsorg().getFoedselsDato();
        for (LocalDate dato : foedselsdatoer) {
            adopsjon.getFoedselsdato().add(dato);
        }

        adopsjon.setAntallBarn(registreringDto.getOmsorg().getAntallBarn());
        adopsjon.setAdopsjonAvEktefellesBarn(registreringDto.getOmsorg().isErEktefellesBarn());
        return adopsjon;
    }

    static Omsorgsovertakelse mapOmsorgsovertakelse(ManuellRegistreringDto registreringDto) {
        Omsorgsovertakelse omsorgsovertakelse = new Omsorgsovertakelse();

        omsorgsovertakelse.setOmsorgsovertakelseaarsak(mapOmsorgsovertakelseaarsaker(registreringDto));
        omsorgsovertakelse.setOmsorgsovertakelsesdato(registreringDto.getOmsorg().getOmsorgsovertakelsesdato());

        List<LocalDate> foedselsdatoer = registreringDto.getOmsorg().getFoedselsDato();
        for (LocalDate dato : foedselsdatoer) {
            omsorgsovertakelse.getFoedselsdato().add(dato);
        }

        omsorgsovertakelse.setAntallBarn(registreringDto.getOmsorg().getAntallBarn());
        return omsorgsovertakelse;
    }

    private static Omsorgsovertakelseaarsaker mapOmsorgsovertakelseaarsaker(ManuellRegistreringDto registreringDto) {
        Omsorgsovertakelseaarsaker omsorgsovertakelseaarsaker = new Omsorgsovertakelseaarsaker();
        FarSøkerType farSøkerType = switch (registreringDto.getRettigheter()) {
            case ANNEN_FORELDER_DOED -> FarSøkerType.ANDRE_FORELDER_DØD;
            case MANN_ADOPTERER_ALENE -> FarSøkerType.ADOPTERER_ALENE;
            default -> (erSøknadVedFødsel(registreringDto.getErBarnetFodt(), registreringDto.getTema())
                ? FarSøkerType.OVERTATT_OMSORG_F
                : FarSøkerType.OVERTATT_OMSORG
            );
        };
        omsorgsovertakelseaarsaker.setKode(farSøkerType.getKode());
        return omsorgsovertakelseaarsaker;
    }

    private static boolean harTermindato(ManuellRegistreringDto registreringDto) {
        return nonNull(registreringDto.getTermindato());
    }

    private static boolean harFødselsdato(ManuellRegistreringDto registreringDto) {
        return !erTomListe(registreringDto.getFoedselsDato());
    }

    private static boolean erSøknadVedFødsel(Boolean erBarnetFødt, FamilieHendelseType tema) {
        boolean fødsel = FamilieHendelseType.FØDSEL.getKode().equals(tema.getKode());
        return (fødsel && (TRUE.equals(erBarnetFødt)));
    }

    private static boolean erSøknadVedTermin(Boolean erBarnetFødt, FamilieHendelseType tema) {
        boolean fødsel = FamilieHendelseType.FØDSEL.getKode().equals(tema.getKode());
        return (fødsel && !(TRUE.equals(erBarnetFødt))); //Barnet er ikke født ennå, termin.
    }

    private static boolean erSøknadVedAdopsjon(FamilieHendelseType tema) {
        return FamilieHendelseType.ADOPSJON.getKode().equals(tema.getKode());
    }

    private static Land getLandkode(String land) {
        Land landkode = new Land();
        landkode.setKode(land);
        return landkode;
    }

    private static boolean erTomListe(List<?> liste) {
        return liste == null || liste.isEmpty();
    }

    public static Medlemskap mapMedlemskap(ManuellRegistreringDto registreringDto) {
        Medlemskap medlemskap = new Medlemskap();

        boolean harFremtidigOppholdUtenlands = registreringDto.getHarFremtidigeOppholdUtenlands();
        boolean harTidligereOppholdUtenlands = registreringDto.getHarTidligereOppholdUtenlands();

        List<OppholdNorge> oppholdNorge = opprettOppholdNorge(registreringDto.getMottattDato(), !harFremtidigOppholdUtenlands, !harTidligereOppholdUtenlands);//Ikke utenlandsopphold tolkes som opphold i norge
        medlemskap.getOppholdNorge().addAll(oppholdNorge);
        medlemskap.setINorgeVedFoedselstidspunkt(registreringDto.getOppholdINorge());
        if (harFremtidigOppholdUtenlands) {
            if (!erTomListe(registreringDto.getFremtidigeOppholdUtenlands())) {
                medlemskap.getOppholdUtlandet().addAll(mapUtenlandsopphold(registreringDto.getFremtidigeOppholdUtenlands()));
            }
        }
        if (harTidligereOppholdUtenlands) {
            if (!erTomListe(registreringDto.getTidligereOppholdUtenlands())) {
                medlemskap.getOppholdUtlandet().addAll(mapUtenlandsopphold(registreringDto.getTidligereOppholdUtenlands()));
            }
        }

        return medlemskap;
    }

    public static Opptjening mapOpptjening(MedInntektArbeidYtelseRegistrering registreringDto, VirksomhetTjeneste virksomhetTjeneste) {
        Opptjening opptjening = new Opptjening();
        opptjening.getAnnenOpptjening().addAll(mapAndreYtelser(registreringDto.getAndreYtelser()));
        opptjening.getEgenNaering().addAll(mapEgneNæringer(registreringDto.getEgenVirksomhet(), virksomhetTjeneste));
        opptjening.getUtenlandskArbeidsforhold().addAll(mapAlleUtenlandskeArbeidsforhold(registreringDto.getArbeidsforhold()));
        opptjening.setFrilans(mapFrilans(registreringDto.getFrilans()));
        return opptjening;
    }

    private static Frilans mapFrilans(FrilansDto dto) {
        if (dto == null || dto.getPerioder() == null) {
            return null;
        }
        Frilans frilans = new Frilans();
        frilans.getPeriode().addAll(dto.getPerioder().stream().map(p -> {
            Periode periode = new Periode();
            periode.setFom(p.getPeriodeFom());
            periode.setTom(p.getPeriodeTom());
            return periode;
        }).collect(Collectors.toList()));

        frilans.setErNyoppstartet(getNullBooleanAsFalse(dto.getErNyoppstartetFrilanser()));
        frilans.setNaerRelasjon(getNullBooleanAsFalse(dto.getHarHattOppdragForFamilie()));
        frilans.setHarInntektFraFosterhjem(getNullBooleanAsFalse(dto.getHarInntektFraFosterhjem()));

        if (getNullBooleanAsFalse(dto.getHarHattOppdragForFamilie())) {
            List<Frilansoppdrag> frilansoppdrag = dto.getOppdragPerioder().stream().map(SøknadMapperFelles::mapAlleFrilansOppdragperioder).collect(Collectors.toList());
            frilans.getFrilansoppdrag().addAll(frilansoppdrag);
        }

        return frilans;
    }

    private static Frilansoppdrag mapAlleFrilansOppdragperioder(FrilansDto.Oppdragperiode oppdragperiode) {
        Frilansoppdrag oppdrag = new Frilansoppdrag();
        oppdrag.setOppdragsgiver(oppdragperiode.getOppdragsgiver());

        Periode periode = new Periode();
        periode.setFom(oppdragperiode.getFomDato());
        periode.setTom(oppdragperiode.getTomDato());
        oppdrag.setPeriode(periode);

        return oppdrag;
    }

    static List<UtenlandskArbeidsforhold> mapAlleUtenlandskeArbeidsforhold(List<ArbeidsforholdDto> arbeidsforhold) {
        if (isNull(arbeidsforhold)) {
            return new ArrayList<>();
        }
        //Arbeidsforhold kan komme inn som liste med ett ArbeidsforholdDto objekt som bare inneholder null verdier. Denne må filtreres bort, siden frontend ikke gjør dette for oss i tilfellene hvor det ikke registreres utenlands arbeidsforhold.
        Predicate<ArbeidsforholdDto> predicateArbeidsforholdFelterUlikNull = arbeidsforholdDto -> arbeidsforholdDto.getPeriodeFom() != null && arbeidsforholdDto.getPeriodeTom() != null && arbeidsforholdDto.getLand() != null;
        return arbeidsforhold.stream().filter(predicateArbeidsforholdFelterUlikNull).map(SøknadMapperFelles::mapUtenlandskArbeidsforhold).collect(Collectors.toList());
    }

    private static UtenlandskArbeidsforhold mapUtenlandskArbeidsforhold(ArbeidsforholdDto arbeidsforholdDto) {
        UtenlandskArbeidsforhold arbeidsforhold = new UtenlandskArbeidsforhold();
        arbeidsforhold.setArbeidsgiversnavn(arbeidsforholdDto.getArbeidsgiver());

        Periode periode = new Periode();
        periode.setFom(arbeidsforholdDto.getPeriodeFom());
        periode.setTom(arbeidsforholdDto.getPeriodeTom());
        arbeidsforhold.setPeriode(periode);

        arbeidsforhold.setArbeidsland(getLandkode(arbeidsforholdDto.getLand()));
        return arbeidsforhold;

    }

    private static List<EgenNaering> mapEgneNæringer(EgenVirksomhetDto egenVirksomhetDto, VirksomhetTjeneste virksomhetTjeneste) {
        if ((isNull(egenVirksomhetDto)) || isNull(egenVirksomhetDto.getVirksomheter())) {
            return new ArrayList<>();
        }
        return egenVirksomhetDto.getVirksomheter().stream().map(v -> mapEgenNæring(v, virksomhetTjeneste)).collect(Collectors.toList());
    }

    //TODO PFP-8741: mapEgenNæring kan ikke fullføres. Er avheig av avklaringer: https://confluence.adeo.no/display/MODNAV/05g.+Avklaringer
    static EgenNaering mapEgenNæring(VirksomhetDto virksomhetDto, VirksomhetTjeneste virksomhetTjeneste) {
        EgenNaering egenNaering;
        if (TRUE.equals(virksomhetDto.getVirksomhetRegistrertINorge())) {
            NorskOrganisasjon norskOrganisasjon = new NorskOrganisasjon();
            norskOrganisasjon.setOrganisasjonsnummer(virksomhetDto.getOrganisasjonsnummer());
            norskOrganisasjon.setNavn(virksomhetDto.getNavn());
            Virksomhet virksomhet = virksomhetTjeneste.hentOrganisasjon(virksomhetDto.getOrganisasjonsnummer());
            Periode periode = new Periode();
            periode.setFom(virksomhet.getRegistrert());
            periode.setTom(virksomhet.getAvslutt() != null ? virksomhet.getAvslutt() : Tid.TIDENES_ENDE);
            norskOrganisasjon.setPeriode(periode);
            egenNaering = norskOrganisasjon;
        } else {
            UtenlandskOrganisasjon utenlandskOrganisasjon = new UtenlandskOrganisasjon();
            utenlandskOrganisasjon.setNavn(virksomhetDto.getNavn());
            Periode periode = new Periode();
            periode.setFom(virksomhetDto.getFom());
            periode.setTom(virksomhetDto.getTom() != null ? virksomhetDto.getTom() : Tid.TIDENES_ENDE);
            utenlandskOrganisasjon.setPeriode(periode);
            utenlandskOrganisasjon.setRegistrertILand(getLandkode(virksomhetDto.getLandJobberFra()));
            egenNaering = utenlandskOrganisasjon;
        }
        egenNaering.setNaerRelasjon(getNullBooleanAsFalse(virksomhetDto.getFamilieEllerVennerTilknyttetNaringen()));
        egenNaering.setErNyIArbeidslivet(getNullBooleanAsFalse(virksomhetDto.getErNyIArbeidslivet()));
        egenNaering.setOppstartsdato(virksomhetDto.getOppstartsdato());

        if (TRUE.equals(virksomhetDto.getHarRegnskapsforer())) {
            Regnskapsfoerer regnskapsfoerer = new Regnskapsfoerer();
            regnskapsfoerer.setNavn(virksomhetDto.getNavnRegnskapsforer());
            regnskapsfoerer.setTelefon(virksomhetDto.getTlfRegnskapsforer());
            egenNaering.setRegnskapsfoerer(regnskapsfoerer);
        }

        if (TRUE.equals(virksomhetDto.getVarigEndretEllerStartetSisteFireAr())) {
            egenNaering.setBeskrivelseAvEndring(virksomhetDto.getBeskrivelseAvEndring());
            egenNaering.setErVarigEndring(getNullBooleanAsFalse(virksomhetDto.getHarVarigEndring()));
            egenNaering.setEndringsDato(virksomhetDto.getVarigEndringGjeldendeFom());
            if (virksomhetDto.getInntekt() != null) {
                egenNaering.setNaeringsinntektBrutto(BigInteger.valueOf(virksomhetDto.getInntekt()));
            }
            egenNaering.setErNyoppstartet(getNullBooleanAsFalse(virksomhetDto.getErNyoppstartet()));
        }
        finnTypeVirksomhet(virksomhetDto, egenNaering);
        return egenNaering;
    }

    private static Boolean getNullBooleanAsFalse(Boolean booleanInn) {
        return booleanInn != null ? booleanInn : false;
    }

    private static void finnTypeVirksomhet(VirksomhetDto virksomhetDto, EgenNaering egenNaering) {
        NaringsvirksomhetTypeDto typeVirksomhet = virksomhetDto.getTypeVirksomhet();
        List<Virksomhetstyper> virksomhetstyper = egenNaering.getVirksomhetstype();
        if (!isNull(typeVirksomhet)) {
            if (typeVirksomhet.getAnnen()) {
                Virksomhetstyper virksomhetstype = new Virksomhetstyper();
                virksomhetstype.setKode(VirksomhetType.ANNEN.getKode());
                virksomhetstyper.add(virksomhetstype);
            }
            if (typeVirksomhet.getFiske()) {
                Virksomhetstyper virksomhetstype = new Virksomhetstyper();
                virksomhetstype.setKode(VirksomhetType.FISKE.getKode());
                virksomhetstyper.add(virksomhetstype);
            }
            if (typeVirksomhet.getDagmammaEllerFamiliebarnehage()) {
                Virksomhetstyper virksomhetstype = new Virksomhetstyper();
                virksomhetstype.setKode(VirksomhetType.DAGMAMMA.getKode());
                virksomhetstyper.add(virksomhetstype);
            }
            if (typeVirksomhet.getJordbrukEllerSkogbruk()) {
                Virksomhetstyper virksomhetstype = new Virksomhetstyper();
                virksomhetstype.setKode(VirksomhetType.JORDBRUK_SKOGBRUK.getKode());
                virksomhetstyper.add(virksomhetstype);
            }
        } else {
            Virksomhetstyper virksomhetstype = new Virksomhetstyper();
            virksomhetstype.setKode(VirksomhetType.UDEFINERT.getKode());
            virksomhetstyper.add(virksomhetstype);
        }
    }

    static List<AnnenOpptjening> mapAndreYtelser(List<AndreYtelserDto> andreYtelser) {
        if (isNull(andreYtelser)) {
            return new ArrayList<>();
        }
        return andreYtelser.stream().map(SøknadMapperFelles::opprettAnnenOpptjening).collect(Collectors.toList());
    }

    private static AnnenOpptjening opprettAnnenOpptjening(AndreYtelserDto opptjening) {
        AnnenOpptjening annenOpptjening = new AnnenOpptjening();
        AnnenOpptjeningTyper typer = new AnnenOpptjeningTyper();
        typer.setKode(opptjening.getYtelseType().getKode());
        annenOpptjening.setType(typer);

        Periode periode = new Periode();
        periode.setFom(opptjening.getPeriodeFom());
        periode.setTom(opptjening.getPeriodeTom());
        annenOpptjening.setPeriode(periode);
        return annenOpptjening;

    }

}
