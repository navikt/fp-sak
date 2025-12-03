package no.nav.foreldrepenger.web.app.tjenester.registrering;

import static java.lang.Boolean.TRUE;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.FarSøkerType;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.ForeldreType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.web.app.tjenester.kodeverk.dto.AndreYtelserDto;
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

public class SøknadMapperFelles {

    private SøknadMapperFelles() {
    }

    public static Soeknad mapSøknad(ManuellRegistreringDto registreringDto, NavBruker navBruker) {
        var søknad = new Soeknad();
        søknad.setMottattDato(registreringDto.getMottattDato());
        søknad.setSoeker(mapBruker(registreringDto.getSøker(), navBruker)); //Stønadstype Søker fnr og søkertype(mor/far/annen/medmor)
        søknad.setTilleggsopplysninger(registreringDto.getTilleggsopplysninger());
        søknad.setSprakvalg(mapSpråk(registreringDto.getSpråkkode()));
        søknad.setBegrunnelseForSenSoeknad(registreringDto.getBegrunnelse());
        return søknad;
    }

    public static AnnenForelder mapAnnenForelder(ManuellRegistreringDto registreringDto, PersoninfoAdapter personinfoAdapter) {
        var annenForelderDto = registreringDto.getAnnenForelder();
        if (annenForelderDto == null) {
            return null;
        }
        if (TRUE.equals(annenForelderDto.getKanIkkeOppgiAnnenForelder())) {
            var oppgittBegrunnelse = annenForelderDto.getKanIkkeOppgiBegrunnelse();
            var utenlandskFoedselsnummer = oppgittBegrunnelse.getUtenlandskFødselsnummer();
            if (utenlandskFoedselsnummer != null && !utenlandskFoedselsnummer.isBlank()) {
                var annenForelderUtenNorskIdent = new AnnenForelderUtenNorskIdent();
                annenForelderUtenNorskIdent.setUtenlandskPersonidentifikator(utenlandskFoedselsnummer);
                if (oppgittBegrunnelse.getLand() != null && !oppgittBegrunnelse.getLand().isBlank()) {
                    annenForelderUtenNorskIdent.setLand(getLandkode(oppgittBegrunnelse.getLand()));
                }
                return annenForelderUtenNorskIdent;
            }
            return new UkjentForelder();
        }

        var annenForelderMedNorskIdent = new AnnenForelderMedNorskIdent();
        var aktørId = personinfoAdapter.hentAktørForFnr(PersonIdent.fra(annenForelderDto.getFødselsnummer()))
            .orElseThrow(() -> new TekniskException("FP-453257", "Fant ikke aktør-ID for fødselsnummer"));
        annenForelderMedNorskIdent.setAktoerId(aktørId.getId());

        return annenForelderMedNorskIdent;

    }
    static Bruker mapBruker(ForeldreType søker, NavBruker navBruker) {
        var bruker = new Bruker();
        bruker.setAktoerId(navBruker.getAktørId().getId());
        var brukerroller = new Brukerroller();

        //TODO PFP-8742 Bør sees på ifm adopsjon PFP-334 og stebarnsadopsjon PFP-182
        var brukerRolleKode = "ANDRE".equals(søker.getKode()) ? "IKKE_RELEVANT" : søker.getKode();

        brukerroller.setKode(brukerRolleKode);
        bruker.setSoeknadsrolle(brukerroller);
        return bruker;
    }

    private static Spraakkode mapSpråk(Språkkode språkkode) {
        if (null == språkkode){
            return null;
        }
        var spraakkode = new Spraakkode();
        spraakkode.setKode(språkkode.getKode());
        spraakkode.setKodeverk("SPRAAK_KODE");
        return spraakkode;
    }

    public static SoekersRelasjonTilBarnet mapRelasjonTilBarnet(ManuellRegistreringDto registreringDto) {
        // Hvis det er gjort et valg av rettigheter knyttet til omsorgovertakelse for far skal saken opprettes som en omsorgsovertakelse.
        var rettigheterRelatertTilOmsorgErSatt = registreringDto.getRettigheter() != null
            && !RettigheterDto.MANN_ADOPTERER_ALENE.equals(registreringDto.getRettigheter());
        if (rettigheterRelatertTilOmsorgErSatt) {
            return mapOmsorgsovertakelse(registreringDto);
        }
        //SøkersRelasjonTilBarnet = adopsjon, fødsel, termin eller omsorg
        var tema = registreringDto.getTema();
        if (erSøknadVedAdopsjon(tema)) {
            return mapAdopsjon(registreringDto);
        }
        if (erSøknadVedFødsel(registreringDto.getErBarnetFødt(), registreringDto.getTema())) {
            return mapFødsel(registreringDto);
        }
        if (erSøknadVedTermin(registreringDto.getErBarnetFødt(), registreringDto.getTema())) {
            return mapTermin(registreringDto);
        }
        throw new IllegalArgumentException(String.format("Ugyldig temakode: %s ", tema));
    }

    static Foedsel mapFødsel(ManuellRegistreringDto registreringDto) {
        var fødsel = new Foedsel();
        if (harFødselsdato(registreringDto)) {
            var foedselsdato = registreringDto.getFødselsdato();
            if (foedselsdato == null) {
                throw new IllegalArgumentException("Støtter bare 1 fødselsdato på fødsel");
            }
            fødsel.setFoedselsdato(registreringDto.getFødselsdato());
            fødsel.setTermindato(registreringDto.getTermindato());
        }
        fødsel.setAntallBarn(registreringDto.getAntallBarn());
        return fødsel;
    }

    private static List<OppholdUtlandet> mapUtenlandsopphold(List<UtenlandsoppholdDto> utenlandsopphold) {
        return utenlandsopphold.stream()
            .map(SøknadMapperFelles::mapEnkeltoppholdUtland)
            .toList();
    }

    private static OppholdUtlandet mapEnkeltoppholdUtland(UtenlandsoppholdDto utenlandsoppholdDto) {
        var nyttOpphold = new OppholdUtlandet();
        nyttOpphold.setLand(getLandkode(utenlandsoppholdDto.getLand()));
        var periode = new Periode();
        periode.setFom(utenlandsoppholdDto.getPeriodeFom());
        periode.setTom(utenlandsoppholdDto.getPeriodeTom());
        nyttOpphold.setPeriode(periode);
        return nyttOpphold;
    }

    static Termin mapTermin(ManuellRegistreringDto registreringDto) {
        var termin = new Termin();
        if (harTermindato(registreringDto)) {
            termin.setTermindato(registreringDto.getTermindato());
            termin.setAntallBarn(registreringDto.getAntallBarnFraTerminbekreftelse());
            termin.setUtstedtdato(registreringDto.getTerminbekreftelseDato());
        }
        return termin;
    }

    static Adopsjon mapAdopsjon(ManuellRegistreringDto registreringDto) {
        var adopsjon = new Adopsjon();
        adopsjon.setOmsorgsovertakelsesdato(registreringDto.getOmsorg().getOmsorgsovertakelsesdato());
        adopsjon.setAnkomstdato(registreringDto.getOmsorg().getAnkomstdato());
        var foedselsdatoer = registreringDto.getOmsorg().getFødselsdato();
        for (var dato : foedselsdatoer) {
            adopsjon.getFoedselsdato().add(dato);
        }

        adopsjon.setAntallBarn(registreringDto.getOmsorg().getAntallBarn());
        adopsjon.setAdopsjonAvEktefellesBarn(registreringDto.getOmsorg().isErEktefellesBarn());
        return adopsjon;
    }

    static Omsorgsovertakelse mapOmsorgsovertakelse(ManuellRegistreringDto registreringDto) {
        var omsorgsovertakelse = new Omsorgsovertakelse();

        omsorgsovertakelse.setOmsorgsovertakelseaarsak(mapOmsorgsovertakelseaarsaker(registreringDto));
        omsorgsovertakelse.setOmsorgsovertakelsesdato(registreringDto.getOmsorg().getOmsorgsovertakelsesdato());

        var foedselsdatoer = registreringDto.getOmsorg().getFødselsdato();
        for (var dato : foedselsdatoer) {
            omsorgsovertakelse.getFoedselsdato().add(dato);
        }

        omsorgsovertakelse.setAntallBarn(registreringDto.getOmsorg().getAntallBarn());
        return omsorgsovertakelse;
    }

    private static Omsorgsovertakelseaarsaker mapOmsorgsovertakelseaarsaker(ManuellRegistreringDto registreringDto) {
        var omsorgsovertakelseaarsaker = new Omsorgsovertakelseaarsaker();
        var farSøkerType = switch (registreringDto.getRettigheter()) {
            case ANNEN_FORELDER_DOED -> FarSøkerType.ANDRE_FORELDER_DØD;
            case MANN_ADOPTERER_ALENE -> FarSøkerType.ADOPTERER_ALENE;
            default -> erSøknadVedFødsel(registreringDto.getErBarnetFødt(),
                registreringDto.getTema()) ? FarSøkerType.OVERTATT_OMSORG_F : FarSøkerType.OVERTATT_OMSORG;
        };
        omsorgsovertakelseaarsaker.setKode(farSøkerType.getKode());
        return omsorgsovertakelseaarsaker;
    }

    private static boolean harTermindato(ManuellRegistreringDto registreringDto) {
        return nonNull(registreringDto.getTermindato());
    }

    private static boolean harFødselsdato(ManuellRegistreringDto registreringDto) {
        return registreringDto.getFødselsdato() != null;
    }

    private static boolean erSøknadVedFødsel(Boolean erBarnetFødt, FamilieHendelseType tema) {
        var fødsel = FamilieHendelseType.FØDSEL.getKode().equals(tema.getKode());
        return fødsel && TRUE.equals(erBarnetFødt);
    }

    private static boolean erSøknadVedTermin(Boolean erBarnetFødt, FamilieHendelseType tema) {
        var fødsel = FamilieHendelseType.FØDSEL.getKode().equals(tema.getKode());
        return fødsel && !TRUE.equals(erBarnetFødt); //Barnet er ikke født ennå, termin.
    }

    private static boolean erSøknadVedAdopsjon(FamilieHendelseType tema) {
        return FamilieHendelseType.ADOPSJON.getKode().equals(tema.getKode());
    }

    private static Land getLandkode(String land) {
        var landkode = new Land();
        landkode.setKode(land);
        return landkode;
    }

    private static boolean erTomListe(List<?> liste) {
        return liste == null || liste.isEmpty();
    }

    public static Medlemskap mapMedlemskap(ManuellRegistreringDto registreringDto) {
        var medlemskap = new Medlemskap();

        var harFremtidigOppholdUtenlands = registreringDto.getHarFremtidigeOppholdUtenlands();
        var harTidligereOppholdUtenlands = registreringDto.getHarTidligereOppholdUtenlands();

        medlemskap.setINorgeVedFoedselstidspunkt(registreringDto.getOppholdINorge());
        medlemskap.setBoddINorgeSiste12Mnd(!harTidligereOppholdUtenlands);
        medlemskap.setBorINorgeNeste12Mnd(!harFremtidigOppholdUtenlands);

        if (harFremtidigOppholdUtenlands && !erTomListe(registreringDto.getFremtidigeOppholdUtenlands())) {
            medlemskap.getOppholdUtlandet().addAll(mapUtenlandsopphold(registreringDto.getFremtidigeOppholdUtenlands()));
        }
        if (harTidligereOppholdUtenlands && !erTomListe(registreringDto.getTidligereOppholdUtenlands())) {
            medlemskap.getOppholdUtlandet().addAll(mapUtenlandsopphold(registreringDto.getTidligereOppholdUtenlands()));
        }

        return medlemskap;
    }

    public static Opptjening mapOpptjening(MedInntektArbeidYtelseRegistrering registreringDto, VirksomhetTjeneste virksomhetTjeneste) {
        var opptjening = new Opptjening();
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
        var frilans = new Frilans();
        frilans.getPeriode().addAll(dto.getPerioder().stream().map(p -> {
            var periode = new Periode();
            periode.setFom(p.getPeriodeFom());
            periode.setTom(p.getPeriodeTom());
            return periode;
        }).toList());

        frilans.setErNyoppstartet(TRUE.equals(dto.getErNyoppstartetFrilanser()));
        frilans.setNaerRelasjon(TRUE.equals(dto.getHarHattOppdragForFamilie()));
        frilans.setHarInntektFraFosterhjem(TRUE.equals(dto.getHarInntektFraFosterhjem()));

        if (TRUE.equals(dto.getHarHattOppdragForFamilie())) {
            var frilansoppdrag = dto.getOppdragPerioder().stream().map(SøknadMapperFelles::mapAlleFrilansOppdragperioder).toList();
            frilans.getFrilansoppdrag().addAll(frilansoppdrag);
        }

        return frilans;
    }

    private static Frilansoppdrag mapAlleFrilansOppdragperioder(FrilansDto.Oppdragperiode oppdragperiode) {
        var oppdrag = new Frilansoppdrag();
        oppdrag.setOppdragsgiver(oppdragperiode.getOppdragsgiver());

        var periode = new Periode();
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
        return arbeidsforhold.stream().filter(predicateArbeidsforholdFelterUlikNull).map(SøknadMapperFelles::mapUtenlandskArbeidsforhold).toList();
    }

    private static UtenlandskArbeidsforhold mapUtenlandskArbeidsforhold(ArbeidsforholdDto arbeidsforholdDto) {
        var arbeidsforhold = new UtenlandskArbeidsforhold();
        arbeidsforhold.setArbeidsgiversnavn(arbeidsforholdDto.getArbeidsgiver());

        var periode = new Periode();
        periode.setFom(arbeidsforholdDto.getPeriodeFom());
        periode.setTom(arbeidsforholdDto.getPeriodeTom());
        arbeidsforhold.setPeriode(periode);

        arbeidsforhold.setArbeidsland(getLandkode(arbeidsforholdDto.getLand()));
        return arbeidsforhold;

    }

    private static List<EgenNaering> mapEgneNæringer(EgenVirksomhetDto egenVirksomhetDto, VirksomhetTjeneste virksomhetTjeneste) {
        if (isNull(egenVirksomhetDto) || isNull(egenVirksomhetDto.getVirksomheter())) {
            return new ArrayList<>();
        }
        return egenVirksomhetDto.getVirksomheter().stream().map(v -> mapEgenNæring(v, virksomhetTjeneste)).toList();
    }

    //TODO PFP-8741: mapEgenNæring kan ikke fullføres. Er avheig av avklaringer: https://confluence.adeo.no/display/MODNAV/05g.+Avklaringer
    static EgenNaering mapEgenNæring(VirksomhetDto virksomhetDto, VirksomhetTjeneste virksomhetTjeneste) {
        EgenNaering egenNaering;
        if (TRUE.equals(virksomhetDto.getVirksomhetRegistrertINorge())) {
            var norskOrganisasjon = new NorskOrganisasjon();
            norskOrganisasjon.setOrganisasjonsnummer(virksomhetDto.getOrganisasjonsnummer());
            norskOrganisasjon.setNavn(virksomhetDto.getNavn());
            var virksomhet = virksomhetTjeneste.hentOrganisasjon(virksomhetDto.getOrganisasjonsnummer());
            var periode = new Periode();
            periode.setFom(virksomhet.getRegistrert());
            periode.setTom(virksomhet.getAvslutt() != null ? virksomhet.getAvslutt() : Tid.TIDENES_ENDE);
            norskOrganisasjon.setPeriode(periode);
            egenNaering = norskOrganisasjon;
        } else {
            var utenlandskOrganisasjon = new UtenlandskOrganisasjon();
            utenlandskOrganisasjon.setNavn(virksomhetDto.getNavn());
            var periode = new Periode();
            periode.setFom(virksomhetDto.getFom());
            periode.setTom(virksomhetDto.getTom() != null ? virksomhetDto.getTom() : Tid.TIDENES_ENDE);
            utenlandskOrganisasjon.setPeriode(periode);
            utenlandskOrganisasjon.setRegistrertILand(getLandkode(virksomhetDto.getLandJobberFra()));
            egenNaering = utenlandskOrganisasjon;
        }
        egenNaering.setNaerRelasjon(TRUE.equals(virksomhetDto.getFamilieEllerVennerTilknyttetNaringen()));
        egenNaering.setErNyIArbeidslivet(TRUE.equals(virksomhetDto.getErNyIArbeidslivet()));
        egenNaering.setOppstartsdato(virksomhetDto.getOppstartsdato());

        if (TRUE.equals(virksomhetDto.getHarRegnskapsforer())) {
            var regnskapsfoerer = new Regnskapsfoerer();
            regnskapsfoerer.setNavn(virksomhetDto.getNavnRegnskapsforer());
            regnskapsfoerer.setTelefon(virksomhetDto.getTlfRegnskapsforer());
            egenNaering.setRegnskapsfoerer(regnskapsfoerer);
        }

        if (TRUE.equals(virksomhetDto.getVarigEndretEllerStartetSisteFireAr())) {
            egenNaering.setBeskrivelseAvEndring(virksomhetDto.getBeskrivelseAvEndring());
            egenNaering.setErVarigEndring(TRUE.equals(virksomhetDto.getHarVarigEndring()));
            egenNaering.setEndringsDato(virksomhetDto.getVarigEndringGjeldendeFom());
            if (virksomhetDto.getInntekt() != null) {
                egenNaering.setNaeringsinntektBrutto(BigInteger.valueOf(virksomhetDto.getInntekt()));
            }
            egenNaering.setErNyoppstartet(TRUE.equals(virksomhetDto.getErNyoppstartet()));
        }
        finnTypeVirksomhet(virksomhetDto, egenNaering);
        return egenNaering;
    }

    private static void finnTypeVirksomhet(VirksomhetDto virksomhetDto, EgenNaering egenNaering) {
        var typeVirksomhet = virksomhetDto.getTypeVirksomhet();
        var virksomhetstyper = egenNaering.getVirksomhetstype();
        if (!isNull(typeVirksomhet)) {
            if (typeVirksomhet.getAnnen()) {
                var virksomhetstype = new Virksomhetstyper();
                virksomhetstype.setKode(VirksomhetType.ANNEN.getKode());
                virksomhetstyper.add(virksomhetstype);
            }
            if (typeVirksomhet.getFiske()) {
                var virksomhetstype = new Virksomhetstyper();
                virksomhetstype.setKode(VirksomhetType.FISKE.getKode());
                virksomhetstyper.add(virksomhetstype);
            }
            if (typeVirksomhet.getDagmammaEllerFamiliebarnehage()) {
                var virksomhetstype = new Virksomhetstyper();
                virksomhetstype.setKode(VirksomhetType.DAGMAMMA.getKode());
                virksomhetstyper.add(virksomhetstype);
            }
            if (typeVirksomhet.getJordbrukEllerSkogbruk()) {
                var virksomhetstype = new Virksomhetstyper();
                virksomhetstype.setKode(VirksomhetType.JORDBRUK_SKOGBRUK.getKode());
                virksomhetstyper.add(virksomhetstype);
            }
        } else {
            var virksomhetstype = new Virksomhetstyper();
            virksomhetstype.setKode(VirksomhetType.UDEFINERT.getKode());
            virksomhetstyper.add(virksomhetstype);
        }
    }

    static List<AnnenOpptjening> mapAndreYtelser(List<AndreYtelserDto> andreYtelser) {
        if (isNull(andreYtelser)) {
            return new ArrayList<>();
        }
        return andreYtelser.stream().map(SøknadMapperFelles::opprettAnnenOpptjening).toList();
    }

    private static AnnenOpptjening opprettAnnenOpptjening(AndreYtelserDto opptjening) {
        var annenOpptjening = new AnnenOpptjening();
        var typer = new AnnenOpptjeningTyper();
        typer.setKode(opptjening.getYtelseType().getKode());
        annenOpptjening.setType(typer);

        var periode = new Periode();
        periode.setFom(opptjening.getPeriodeFom());
        periode.setTom(opptjening.getPeriodeTom());
        annenOpptjening.setPeriode(periode);
        return annenOpptjening;

    }

}
