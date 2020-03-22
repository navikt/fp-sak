package no.nav.foreldrepenger.domene.person.tps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.FødtBarnInfo;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingsgrunnlagKodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.PoststedKodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Doedsdato;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjon;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Familierelasjoner;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Foedselsdato;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gateadresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Gyldighetsperiode;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoenn;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Kjoennstyper;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Landkoder;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Matrikkeladresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Matrikkelnummer;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.MidlertidigPostadresseNorge;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.MidlertidigPostadresseUtland;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.NorskIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Periode;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonIdent;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personnavn;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatus;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PersonstatusPeriode;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Personstatuser;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Postadresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Postadressetyper;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.PostboksadresseNorsk;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Postnummer;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Spraak;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Statsborgerskap;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.StatsborgerskapPeriode;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.UstrukturertAdresse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkResponse;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.konfig.Tid;

public class TpsOversetterTest {
    private static final DatatypeFactory DATATYPE_FACTORY;
    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final String POSTNUMMER = "1234";

    private static final String GATEADRESSE1 = "Gaten 13 B";

    private static final String USTRUKTURERT_GATEADRESSE1 = "Ustrukturert adresselinje 1";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingsgrunnlagKodeverkRepository bgKodeverkRepository = new BehandlingsgrunnlagKodeverkRepository(repoRule.getEntityManager());
    private PoststedKodeverkRepository poststedKodeverkRepository = new PoststedKodeverkRepository(repoRule.getEntityManager());

    @Mock
    private Bruker bruker;
    @Mock
    private MidlertidigPostadresseNorge midlertidigPostadresseNorge;
    @Mock
    private MidlertidigPostadresseUtland midlertidigPostadresseUtland;
    @Mock
    private Matrikkeladresse matrikkeladresse;
    @Mock
    private Gateadresse gateadresse;
    @Mock
    private Postadresse postadresse;
    @Mock
    private UstrukturertAdresse ustrukturertAdresse;
    @Mock
    private Bostedsadresse bostedsadresse;
    @Mock
    private PostboksadresseNorsk postboksAdresse;

    private TpsOversetter tpsOversetter;
    private TpsAdresseOversetter tpsAdresseOversetter;

    @Before
    public void oppsett() throws DatatypeConfigurationException {

        Landkoder landkodeNorge = new Landkoder();
        landkodeNorge.setValue("NOR");
        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setLand(landkodeNorge);

        NorskIdent ident = new NorskIdent();
        ident.setIdent("123");
        PersonIdent pi = new PersonIdent();
        pi.setIdent(ident);

        when(bruker.getAktoer()).thenReturn(pi);
        when(bruker.getStatsborgerskap()).thenReturn(statsborgerskap);
        tpsAdresseOversetter = new TpsAdresseOversetter(
            poststedKodeverkRepository);
        tpsOversetter = new TpsOversetter(
            bgKodeverkRepository, tpsAdresseOversetter);
        Matrikkelnummer matrikkelnummer = new Matrikkelnummer();
        matrikkelnummer.setBruksnummer("bnr");
        matrikkelnummer.setFestenummer("fnr");
        matrikkelnummer.setGaardsnummer("gnr");
        matrikkelnummer.setSeksjonsnummer("snr");
        matrikkelnummer.setUndernummer("unr");
        when(matrikkeladresse.getMatrikkelnummer()).thenReturn(matrikkelnummer);
        Postnummer poststed = new Postnummer();
        poststed.setKodeRef(POSTNUMMER);
        poststed.setValue(POSTNUMMER);
        when(matrikkeladresse.getPoststed()).thenReturn(poststed);

        when(postboksAdresse.getLandkode()).thenReturn(landkodeNorge);
        when(postboksAdresse.getPostboksnummer()).thenReturn("47");
        when(postboksAdresse.getPoststed()).thenReturn(poststed);

        when(gateadresse.getGatenavn()).thenReturn("Gaten");
        when(gateadresse.getHusnummer()).thenReturn(13);
        when(gateadresse.getHusbokstav()).thenReturn("B");
        when(gateadresse.getPoststed()).thenReturn(poststed);

        Gyldighetsperiode gyldighetsperiode = new Gyldighetsperiode();
        LocalDate fom = LocalDate.now().minusDays(1);
        LocalDate tom = LocalDate.now().plusDays(1);
        gyldighetsperiode.setFom(DATATYPE_FACTORY.newXMLGregorianCalendar(GregorianCalendar.from(fom.atStartOfDay(ZoneId.systemDefault()))));
        gyldighetsperiode.setTom(DATATYPE_FACTORY.newXMLGregorianCalendar(GregorianCalendar.from(tom.atStartOfDay(ZoneId.systemDefault()))));
        when(midlertidigPostadresseNorge.getPostleveringsPeriode()).thenReturn(gyldighetsperiode);
        Personnavn personnavn = new Personnavn();
        personnavn.setSammensattNavn("Ole Olsen");
        when(bruker.getPersonnavn()).thenReturn(personnavn);

        UstrukturertAdresse adresse = new UstrukturertAdresse();
        adresse.setAdresselinje1("Test utlandsadresse");
        when(midlertidigPostadresseUtland.getUstrukturertAdresse()).thenReturn(adresse);

        when(ustrukturertAdresse.getAdresselinje1()).thenReturn(USTRUKTURERT_GATEADRESSE1);
        when(postadresse.getUstrukturertAdresse()).thenReturn(ustrukturertAdresse);
        leggPåAndrePåkrevdeFelter();
    }

    @Test
    public void testPostnummerAdresse() {
        initMockBostedsadresseMedPostboksAdresseForBruker();

        Adresseinfo adresseinfo = tpsOversetter.tilAdresseInfo(bruker);

        assertThat(adresseinfo).isNotNull();
        assertThat(adresseinfo.getAdresselinje1()).isEqualTo("Postboks 47");
        assertThat(adresseinfo.getAdresselinje2()).isNull();
        assertThat(adresseinfo.getAdresselinje3()).isNull();
        assertThat(adresseinfo.getAdresselinje4()).isNull();
        assertThat(adresseinfo.getLand()).isEqualTo("NOR");
        assertThat(adresseinfo.getPostNr()).isEqualTo("1234");
        assertThat(adresseinfo.getPoststed()).isEqualTo("UKJENT");
    }

    @Test(expected = VLException.class)
    public void testPostnummerAdresseEksistererIkke() {
        when(bruker.getGjeldendePostadressetype()).thenReturn(new Postadressetyper());
        tpsOversetter.tilAdresseInfo(bruker);
    }

    @Test
    public void testUtlandsadresse() {
        when(bruker.getMidlertidigPostadresse()).thenReturn(midlertidigPostadresseUtland);
        String utlandsadresse = tpsAdresseOversetter.finnUtlandsadresseFor(bruker);
        assertThat(utlandsadresse).isEqualTo("Test utlandsadresse");
    }

    @Test
    public void skal_ikke_feile_når_bruker_ikke_har_utlandsadresse() {
        when(bruker.getMidlertidigPostadresse()).thenReturn(null);
        String utlandsadresse = tpsAdresseOversetter.finnUtlandsadresseFor(bruker);
        assertThat(utlandsadresse).isNull();
    }

    @Test
    public void testPostnummerAdresseMedPostboksanlegg() {
        initMockBostedsadresseMedPostboksAdresseForBruker();
        when(postboksAdresse.getPostboksanlegg()).thenReturn("Etterstad");

        Adresseinfo adresseinfo = tpsOversetter.tilAdresseInfo(bruker);

        assertThat(adresseinfo).isNotNull();
        assertThat(adresseinfo.getAdresselinje1()).isEqualTo("Postboks 47 Etterstad");
        assertThat(adresseinfo.getAdresselinje2()).isNull();
        assertThat(adresseinfo.getAdresselinje3()).isNull();
        assertThat(adresseinfo.getAdresselinje4()).isNull();
        assertThat(adresseinfo.getLand()).isEqualTo("NOR");
        assertThat(adresseinfo.getPostNr()).isEqualTo("1234");
        assertThat(adresseinfo.getPoststed()).isEqualTo("UKJENT");
    }

    @Test
    public void testPostnummerAdresseMedTilleggsadresse() {
        initMockBostedsadresseMedPostboksAdresseForBruker();
        when(postboksAdresse.getTilleggsadresse()).thenReturn("Tilleggsadresse");
        when(postboksAdresse.getTilleggsadresseType()).thenReturn("TilleggsadresseType");

        Adresseinfo adresseinfo = tpsOversetter.tilAdresseInfo(bruker);

        assertThat(adresseinfo).isNotNull();
        assertThat(adresseinfo.getAdresselinje1()).isEqualTo("Tilleggsadresse");
        assertThat(adresseinfo.getAdresselinje2()).isEqualTo("Postboks 47");
        assertThat(adresseinfo.getAdresselinje3()).isNull();
        assertThat(adresseinfo.getAdresselinje4()).isNull();
        assertThat(adresseinfo.getLand()).isEqualTo("NOR");
        assertThat(adresseinfo.getPostNr()).isEqualTo("1234");
        assertThat(adresseinfo.getPoststed()).isEqualTo("UKJENT");
    }

    @Test
    public void testMidlertidigMatrikkelAdresseNorge() {
        when(bruker.getGjeldendePostadressetype()).thenReturn(tilPostadressetyper("MIDLERTIDIG_POSTADRESSE_NORGE"));
        when(midlertidigPostadresseNorge.getStrukturertAdresse()).thenReturn(matrikkeladresse);
        when(bruker.getMidlertidigPostadresse()).thenReturn(midlertidigPostadresseNorge);
        Adresseinfo adresseinfo = tpsOversetter.tilAdresseInfo(bruker);
        assertThat(adresseinfo).isNotNull();
        assertThat(adresseinfo.getPostNr()).isEqualTo(POSTNUMMER);
    }

    @Test
    public void testMidlertidigGateAdresseNorge() {
        when(bruker.getGjeldendePostadressetype()).thenReturn(tilPostadressetyper("MIDLERTIDIG_POSTADRESSE_NORGE"));
        when(midlertidigPostadresseNorge.getStrukturertAdresse()).thenReturn(gateadresse);
        when(bruker.getMidlertidigPostadresse()).thenReturn(midlertidigPostadresseNorge);

        Adresseinfo adresseinfo = tpsOversetter.tilAdresseInfo(bruker);

        assertThat(adresseinfo).isNotNull();
        assertThat(adresseinfo.getAdresselinje1()).isEqualTo(GATEADRESSE1);
        assertThat(adresseinfo.getPostNr()).isEqualTo(POSTNUMMER);
    }

    @Test
    public void testUstrukturertAdresse() {
        when(bruker.getGjeldendePostadressetype()).thenReturn(tilPostadressetyper("POSTADRESSE"));
        when(bruker.getPostadresse()).thenReturn(postadresse);

        Adresseinfo adresseinfo = tpsOversetter.tilAdresseInfo(bruker);

        assertThat(adresseinfo).isNotNull();
        assertThat(adresseinfo.getAdresselinje1()).isEqualTo(USTRUKTURERT_GATEADRESSE1);
    }

    @Test
    public void skal_ha_med_foretrukket_språk_når_finnes() throws Exception {
        tpsOversetter = new TpsOversetter(bgKodeverkRepository, tpsAdresseOversetter);

        Spraak språk = new Spraak();
        språk.setValue("NN");
        when(bruker.getMaalform()).thenReturn(språk);
        Personinfo personinfo = tpsOversetter.tilBrukerInfo(AktørId.dummy(), bruker);
        assertThat(personinfo.getForetrukketSpråk()).isEqualTo(Språkkode.nn);
    }

    @Test
    public void skal_default_til_bokmål_om_foretrukket_språk_ikke_er_satt() throws Exception {

        tpsOversetter = new TpsOversetter(bgKodeverkRepository, tpsAdresseOversetter);

        when(bruker.getMaalform()).thenReturn(null);
        Personinfo personinfo = tpsOversetter.tilBrukerInfo(AktørId.dummy(), bruker);
        assertThat(personinfo.getForetrukketSpråk()).isEqualTo(Språkkode.nb);
    }

    @Test
    public void skal_defaulte_til_bokmål_om_foretrukket_språk_ikke_er_støttet() throws Exception {

        tpsOversetter = new TpsOversetter(bgKodeverkRepository, tpsAdresseOversetter);

        Spraak språk = new Spraak();
        språk.setValue("SVORSK");
        when(bruker.getMaalform()).thenReturn(språk);
        Personinfo personinfo = tpsOversetter.tilBrukerInfo(AktørId.dummy(), bruker);
        assertThat(personinfo.getForetrukketSpråk()).isEqualTo(Språkkode.nb);
    }

    @Test
    public void skal_defaulte_til_bokmål_om_foretrukket_språk_er_NO() throws Exception {
        BehandlingsgrunnlagKodeverkRepository grunnlagRepo = Mockito.mock(BehandlingsgrunnlagKodeverkRepository.class);
        when(grunnlagRepo.finnHøyestRangertRegion(Collections.singletonList(ArgumentMatchers.anyString()))).thenReturn(Region.UDEFINERT);

        tpsOversetter = new TpsOversetter(bgKodeverkRepository, tpsAdresseOversetter);

        Spraak språk = new Spraak();
        språk.setValue("NO");
        when(bruker.getMaalform()).thenReturn(språk);
        Personinfo personinfo = tpsOversetter.tilBrukerInfo(AktørId.dummy(), bruker);
        assertThat(personinfo.getForetrukketSpråk()).isEqualTo(Språkkode.nb);
    }

    @Test
    public void skal_oversette_statsborgerskap() throws Exception {
        // Arrange
        BehandlingsgrunnlagKodeverkRepository grunnlagRepo = Mockito.mock(BehandlingsgrunnlagKodeverkRepository.class);
        no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder norge = no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder.NOR;
        when(grunnlagRepo.finnLandkode(norge.getKode())).thenReturn(norge);
        when(grunnlagRepo.finnHøyestRangertRegion(any())).thenReturn(Region.NORDEN);
        tpsOversetter = new TpsOversetter(grunnlagRepo, tpsAdresseOversetter);

        // Act
        Personinfo personinfo = tpsOversetter.tilBrukerInfo(AktørId.dummy(), bruker);

        // Assert
        assertThat(personinfo.getRegion()).isEqualTo(Region.NORDEN);
        assertThat(personinfo.getLandkode()).isEqualTo(norge);
        assertThat(personinfo.getStatsborgerskap().getLandkode()).isEqualTo(norge.getKode());
    }

    @Test
    public void skal_returnere_personhistorikkinfo_med_statborgerskapsliste() {
        BehandlingsgrunnlagKodeverkRepository grunnlagRepo = Mockito.mock(BehandlingsgrunnlagKodeverkRepository.class);
        tpsOversetter = new TpsOversetter(grunnlagRepo, tpsAdresseOversetter);
        no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder norge = no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder.NOR;
        when(grunnlagRepo.finnLandkode(norge.getKode())).thenReturn(norge);

        HentPersonhistorikkResponse response = new HentPersonhistorikkResponse();
        String aktørIdString = "12345679";
        AktoerId aktør = new AktoerId();
        aktør.setAktoerId(aktørIdString);
        response.setAktoer(aktør);
        StatsborgerskapPeriode statsborgerskapPeriode = new StatsborgerskapPeriode();
        Statsborgerskap statsborgerskap = new Statsborgerskap();
        Landkoder landkoder = new Landkoder();
        landkoder.setValue("NOR");
        statsborgerskap.setLand(landkoder);
        statsborgerskapPeriode.setStatsborgerskap(statsborgerskap);
        Periode periode = new Periode();
        periode.setFom(null);
        periode.setTom(null);
        statsborgerskapPeriode.setPeriode(periode);
        Collection<StatsborgerskapPeriode> statsborgerskapListe = Collections.singletonList(statsborgerskapPeriode);
        response.withStatsborgerskapListe(statsborgerskapListe);

        Personhistorikkinfo personhistorikkinfo = tpsOversetter.tilPersonhistorikkInfo(aktørIdString, response);

        assertThat(personhistorikkinfo.getAktørId()).isEqualTo(aktør.getAktoerId());
        List<no.nav.foreldrepenger.behandlingslager.aktør.historikk.StatsborgerskapPeriode> statsborgerskaphistorikk = personhistorikkinfo
            .getStatsborgerskaphistorikk();
        assertThat(statsborgerskaphistorikk.get(0).getGyldighetsperiode().getFom()).isEqualTo(Tid.TIDENES_BEGYNNELSE);
        assertThat(statsborgerskaphistorikk.get(0).getGyldighetsperiode().getTom()).isEqualTo(Tid.TIDENES_ENDE);
        assertThat(statsborgerskaphistorikk.get(0).getStatsborgerskap().getLandkode()).isEqualTo("NOR");
    }

    @Test
    public void skal_returnere_personhistorikkinfo_med_personstatusliste() throws DatatypeConfigurationException {
        BehandlingsgrunnlagKodeverkRepository grunnlagRepo = Mockito.mock(BehandlingsgrunnlagKodeverkRepository.class);
        tpsOversetter = new TpsOversetter(grunnlagRepo, tpsAdresseOversetter);

        HentPersonhistorikkResponse response = new HentPersonhistorikkResponse();
        String aktørIdString = "67856789423";
        AktoerId aktør = new AktoerId();
        aktør.setAktoerId(aktørIdString);
        response.setAktoer(aktør);

        Periode periode1 = new Periode();
        LocalDate tomPeriode1 = LocalDate.of(2015, 4, 1);
        periode1.setFom(null);
        periode1.setTom(DateUtil.convertToXMLGregorianCalendar(tomPeriode1));
        PersonstatusPeriode personstatusPeriode1 = new PersonstatusPeriode();
        Personstatuser status1 = new Personstatuser();
        status1.withValue("FØDR");
        personstatusPeriode1.setPersonstatus(status1);
        personstatusPeriode1.setPeriode(periode1);

        Periode periode2 = new Periode();
        periode2.setFom(DateUtil.convertToXMLGregorianCalendar(tomPeriode1));
        periode2.setTom(null);
        PersonstatusPeriode personstatusPeriode2 = new PersonstatusPeriode();
        Personstatuser status2 = new Personstatuser();
        status2.withValue("UTVA");
        personstatusPeriode2.setPersonstatus(status2);
        personstatusPeriode2.setPeriode(periode2);

        Collection<PersonstatusPeriode> personstatusListe = new LinkedList<PersonstatusPeriode>();
        personstatusListe.add(personstatusPeriode1);
        personstatusListe.add(personstatusPeriode2);
        response.withPersonstatusListe(personstatusListe);

        Personhistorikkinfo personhistorikkinfo = tpsOversetter.tilPersonhistorikkInfo(aktørIdString, response);

        assertThat(personhistorikkinfo.getAktørId()).isEqualTo(aktør.getAktoerId());
        List<no.nav.foreldrepenger.behandlingslager.aktør.historikk.PersonstatusPeriode> personstatushistorikk = personhistorikkinfo.getPersonstatushistorikk();
        assertThat(personstatushistorikk.get(0).getGyldighetsperiode().getFom()).isEqualTo(Tid.TIDENES_BEGYNNELSE);
        assertThat(personstatushistorikk.get(0).getGyldighetsperiode().getTom()).isEqualTo(tomPeriode1);
        assertThat(personstatushistorikk.get(0).getPersonstatus().getKode()).isEqualTo(PersonstatusType.FØDR.getKode());
        assertThat(personstatushistorikk.get(1).getGyldighetsperiode().getFom()).isEqualTo(tomPeriode1);
        assertThat(personstatushistorikk.get(1).getGyldighetsperiode().getTom()).isEqualTo(Tid.TIDENES_ENDE);
        assertThat(personstatushistorikk.get(1).getPersonstatus().getKode()).isEqualTo(PersonstatusType.UTVA.getKode());
    }

    @Test
    public void skal_returnere_fødte_barn_liste() throws DatatypeConfigurationException {
        LocalDate førsteJanuarNitten = LocalDate.of(2019, 01, 01);
        LocalDate andreFebruarNitten = LocalDate.of(2019, 02, 02);

        BehandlingsgrunnlagKodeverkRepository grunnlagRepo = Mockito.mock(BehandlingsgrunnlagKodeverkRepository.class);
        tpsOversetter = new TpsOversetter(grunnlagRepo, tpsAdresseOversetter);

        no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker bruker = new no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker();

        no.nav.tjeneste.virksomhet.person.v3.informasjon.Person barnFnr = fødBarn("01011999928", false);
        barnFnr.setFoedselsdato(new Foedselsdato().withFoedselsdato(DateUtil.convertToXMLGregorianCalendar(førsteJanuarNitten)));

        no.nav.tjeneste.virksomhet.person.v3.informasjon.Person barnDnr = fødBarn("41011999847", true);
        barnDnr.setFoedselsdato(new Foedselsdato().withFoedselsdato(DateUtil.convertToXMLGregorianCalendar(førsteJanuarNitten)));
        barnDnr.setDoedsdato(new Doedsdato().withDoedsdato(DateUtil.convertToXMLGregorianCalendar(andreFebruarNitten)));

        no.nav.tjeneste.virksomhet.person.v3.informasjon.Person barnFdat = fødBarn("01011900001", true);

        Familierelasjoner familierelasjoner = new Familierelasjoner().withValue(RelasjonsRolleType.BARN.getKode());
        Familierelasjon familierelasjonBarnFnr = new Familierelasjon().withTilRolle(familierelasjoner).withTilPerson(barnFnr);
        Familierelasjon familierelasjonBarnDnr = new Familierelasjon().withTilRolle(familierelasjoner).withTilPerson(barnDnr);
        Familierelasjon familierelasjonBarnFdat = new Familierelasjon().withTilRolle(familierelasjoner).withTilPerson(barnFdat);

        List<FødtBarnInfo> fødteBarn = tpsOversetter.tilFødteBarn(bruker
            .withHarFraRolleI(familierelasjonBarnFnr).withHarFraRolleI(familierelasjonBarnDnr).withHarFraRolleI(familierelasjonBarnFdat));

        assertThat(fødteBarn).hasSize(3);
        assertThat(fødteBarn.get(0).getFødselsdato()).isEqualTo(førsteJanuarNitten);
        assertThat(fødteBarn.get(1).getFødselsdato()).isEqualTo(førsteJanuarNitten);
        assertThat(fødteBarn.get(1).getDødsdato().get()).isEqualTo(andreFebruarNitten);
        assertThat(fødteBarn.get(1).getIdent().erDnr()).isTrue();
        assertThat(fødteBarn.get(2).getFødselsdato()).isEqualTo(førsteJanuarNitten);
        assertThat(fødteBarn.get(2).getDødsdato().get()).isEqualTo(førsteJanuarNitten);
        assertThat(fødteBarn.get(2).getIdent().erFdatNummer()).isTrue();
    }

    private no.nav.tjeneste.virksomhet.person.v3.informasjon.Person fødBarn(String ident, boolean erJente) {
        Kjoenn kjønn = new Kjoenn();
        Kjoennstyper kjønnstype = new Kjoennstyper();
        kjønnstype.setValue(erJente ? "K" : "M");
        kjønn.setKjoenn(kjønnstype);

        no.nav.tjeneste.virksomhet.person.v3.informasjon.Person barn = new no.nav.tjeneste.virksomhet.person.v3.informasjon.Person();
        barn.setKjoenn(kjønn);
        barn.setAktoer(new PersonIdent().withIdent(new NorskIdent().withIdent(ident)));
        barn.setPersonnavn(new Personnavn().withSammensattNavn("Navi Navesen"));

        return barn;
    }

    private void initMockBrukerPersonstatus(PersonstatusType personstatusType) {
        Personstatus tpsPersonstatus = new Personstatus();
        Personstatuser tpsPersonstatuser = new Personstatuser();
        tpsPersonstatuser.setValue(personstatusType.getKode());
        tpsPersonstatus.setPersonstatus(tpsPersonstatuser);
        when(bruker.getPersonstatus()).thenReturn(tpsPersonstatus);
    }

    private void initMockBostedsadresseMedPostboksAdresseForBruker() {
        when(bruker.getGjeldendePostadressetype()).thenReturn(tilPostadressetyper("BOSTEDSADRESSE"));
        when(bruker.getBostedsadresse()).thenReturn(bostedsadresse);
        when(bostedsadresse.getStrukturertAdresse()).thenReturn(postboksAdresse);
    }

    private void leggPåAndrePåkrevdeFelter() throws DatatypeConfigurationException {
        Kjoenn kjønn = new Kjoenn();
        Kjoennstyper kjønnstype = new Kjoennstyper();
        kjønnstype.setValue("K");
        kjønn.setKjoenn(kjønnstype);
        when(bruker.getKjoenn()).thenReturn(kjønn);

        initMockBrukerPersonstatus(PersonstatusType.BOSA);

        Statsborgerskap statsborgerskap = new Statsborgerskap();
        Landkoder land = new Landkoder();
        land.setValue("NOR");
        statsborgerskap.setLand(land);
        when(bruker.getStatsborgerskap()).thenReturn(statsborgerskap);

        initMockBostedsadresseMedPostboksAdresseForBruker();

        Foedselsdato foedselsdato = new Foedselsdato();
        foedselsdato.setFoedselsdato(DateUtil.convertToXMLGregorianCalendar(LocalDate.now()));
        when(bruker.getFoedselsdato()).thenReturn(foedselsdato);
    }

    private Postadressetyper tilPostadressetyper(String type) {
        Postadressetyper postadresseType = new Postadressetyper();
        postadresseType.setValue(type);
        return postadresseType;
    }

}
