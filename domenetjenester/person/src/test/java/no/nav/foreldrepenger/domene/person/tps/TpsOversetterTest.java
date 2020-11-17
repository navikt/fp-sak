package no.nav.foreldrepenger.domene.person.tps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.Adresseinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.aktør.historikk.Personhistorikkinfo;
import no.nav.foreldrepenger.behandlingslager.geografisk.PoststedKodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.AktoerId;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bostedsadresse;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Bruker;
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
import no.nav.tjeneste.virksomhet.person.v3.informasjon.Statsborgerskap;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.StatsborgerskapPeriode;
import no.nav.tjeneste.virksomhet.person.v3.informasjon.UstrukturertAdresse;
import no.nav.tjeneste.virksomhet.person.v3.meldinger.HentPersonhistorikkResponse;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
public class TpsOversetterTest extends EntityManagerAwareTest {
    private static DatatypeFactory DATATYPE_FACTORY;

    private static final String POSTNUMMER = "1234";

    private static final String GATEADRESSE1 = "Gaten 13 B";

    private static final String USTRUKTURERT_GATEADRESSE1 = "Ustrukturert adresselinje 1";

    private PoststedKodeverkRepository poststedKodeverkRepository;

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

    @BeforeAll
    public static void beforeall() throws Exception {
        DATATYPE_FACTORY = DatatypeFactory.newInstance();
    }

    @BeforeEach
    public void oppsett() throws DatatypeConfigurationException {
        poststedKodeverkRepository = new PoststedKodeverkRepository(getEntityManager());

        Landkoder landkodeNorge = new Landkoder();
        landkodeNorge.setValue("NOR");
        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setLand(landkodeNorge);

        NorskIdent ident = new NorskIdent();
        ident.setIdent("123");
        PersonIdent pi = new PersonIdent();
        pi.setIdent(ident);

        lenient().when(bruker.getAktoer()).thenReturn(pi);
        lenient().when(bruker.getStatsborgerskap()).thenReturn(statsborgerskap);
        tpsAdresseOversetter = new TpsAdresseOversetter(
                poststedKodeverkRepository);
        tpsOversetter = new TpsOversetter(
                tpsAdresseOversetter);
        Matrikkelnummer matrikkelnummer = new Matrikkelnummer();
        matrikkelnummer.setBruksnummer("bnr");
        matrikkelnummer.setFestenummer("fnr");
        matrikkelnummer.setGaardsnummer("gnr");
        matrikkelnummer.setSeksjonsnummer("snr");
        matrikkelnummer.setUndernummer("unr");
        lenient().when(matrikkeladresse.getMatrikkelnummer()).thenReturn(matrikkelnummer);
        Postnummer poststed = new Postnummer();
        poststed.setKodeRef(POSTNUMMER);
        poststed.setValue(POSTNUMMER);
        lenient().when(matrikkeladresse.getPoststed()).thenReturn(poststed);

        lenient().when(postboksAdresse.getLandkode()).thenReturn(landkodeNorge);
        lenient().when(postboksAdresse.getPostboksnummer()).thenReturn("47");
        lenient().when(postboksAdresse.getPoststed()).thenReturn(poststed);

        lenient().when(gateadresse.getGatenavn()).thenReturn("Gaten");
        lenient().when(gateadresse.getHusnummer()).thenReturn(13);
        lenient().when(gateadresse.getHusbokstav()).thenReturn("B");
        lenient().when(gateadresse.getPoststed()).thenReturn(poststed);

        Gyldighetsperiode gyldighetsperiode = new Gyldighetsperiode();
        LocalDate fom = LocalDate.now().minusDays(1);
        LocalDate tom = LocalDate.now().plusDays(1);

        gyldighetsperiode.setFom(DATATYPE_FACTORY.newXMLGregorianCalendar(GregorianCalendar.from(fom.atStartOfDay(ZoneId.systemDefault()))));
        gyldighetsperiode.setTom(DATATYPE_FACTORY.newXMLGregorianCalendar(GregorianCalendar.from(tom.atStartOfDay(ZoneId.systemDefault()))));
        lenient().when(midlertidigPostadresseNorge.getPostleveringsPeriode()).thenReturn(gyldighetsperiode);
        Personnavn personnavn = new Personnavn();
        personnavn.setSammensattNavn("Ole Olsen");
        lenient().when(bruker.getPersonnavn()).thenReturn(personnavn);

        UstrukturertAdresse adresse = new UstrukturertAdresse();
        adresse.setAdresselinje1("Test utlandsadresse");
        lenient().when(midlertidigPostadresseUtland.getUstrukturertAdresse()).thenReturn(adresse);

        lenient().when(ustrukturertAdresse.getAdresselinje1()).thenReturn(USTRUKTURERT_GATEADRESSE1);
        lenient().when(postadresse.getUstrukturertAdresse()).thenReturn(ustrukturertAdresse);
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

    @Test
    public void testPostnummerAdresseEksistererIkke() {
        when(bruker.getGjeldendePostadressetype()).thenReturn(new Postadressetyper());
        assertThrows(VLException.class, () -> tpsOversetter.tilAdresseInfo(bruker));
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
        lenient().when(postboksAdresse.getTilleggsadresseType()).thenReturn("TilleggsadresseType");

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
    public void skal_oversette_statsborgerskap() throws Exception {
        // Arrange
        no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder norge = no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder.NOR;
        tpsOversetter = new TpsOversetter(tpsAdresseOversetter);

        // Act
        Personinfo personinfo = tpsOversetter.tilBrukerInfo(AktørId.dummy(), bruker);

        // Assert
        assertThat(personinfo.getRegion()).isEqualTo(Region.NORDEN);
        assertThat(personinfo.getLandkode()).isEqualTo(norge);
    }

    @Test
    public void skal_returnere_personhistorikkinfo_med_statborgerskapsliste() {
        no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder norge = no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder.NOR;
        tpsOversetter = new TpsOversetter(tpsAdresseOversetter);

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
        tpsOversetter = new TpsOversetter(tpsAdresseOversetter);

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
        List<no.nav.foreldrepenger.behandlingslager.aktør.historikk.PersonstatusPeriode> personstatushistorikk = personhistorikkinfo
                .getPersonstatushistorikk();
        assertThat(personstatushistorikk.get(0).getGyldighetsperiode().getFom()).isEqualTo(Tid.TIDENES_BEGYNNELSE);
        assertThat(personstatushistorikk.get(0).getGyldighetsperiode().getTom()).isEqualTo(tomPeriode1);
        assertThat(personstatushistorikk.get(0).getPersonstatus().getKode()).isEqualTo(PersonstatusType.FØDR.getKode());
        assertThat(personstatushistorikk.get(1).getGyldighetsperiode().getFom()).isEqualTo(tomPeriode1);
        assertThat(personstatushistorikk.get(1).getGyldighetsperiode().getTom()).isEqualTo(Tid.TIDENES_ENDE);
        assertThat(personstatushistorikk.get(1).getPersonstatus().getKode()).isEqualTo(PersonstatusType.UTVA.getKode());
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
        lenient().when(bruker.getPersonstatus()).thenReturn(tpsPersonstatus);
    }

    private void initMockBostedsadresseMedPostboksAdresseForBruker() {
        lenient().when(bruker.getGjeldendePostadressetype()).thenReturn(tilPostadressetyper("BOSTEDSADRESSE"));
        lenient().when(bruker.getBostedsadresse()).thenReturn(bostedsadresse);
        lenient().when(bostedsadresse.getStrukturertAdresse()).thenReturn(postboksAdresse);
    }

    private void leggPåAndrePåkrevdeFelter() throws DatatypeConfigurationException {
        Kjoenn kjønn = new Kjoenn();
        Kjoennstyper kjønnstype = new Kjoennstyper();
        kjønnstype.setValue("K");
        kjønn.setKjoenn(kjønnstype);
        lenient().when(bruker.getKjoenn()).thenReturn(kjønn);

        initMockBrukerPersonstatus(PersonstatusType.BOSA);

        Statsborgerskap statsborgerskap = new Statsborgerskap();
        Landkoder land = new Landkoder();
        land.setValue("NOR");
        statsborgerskap.setLand(land);
        lenient().when(bruker.getStatsborgerskap()).thenReturn(statsborgerskap);

        initMockBostedsadresseMedPostboksAdresseForBruker();

        Foedselsdato foedselsdato = new Foedselsdato();
        foedselsdato.setFoedselsdato(DateUtil.convertToXMLGregorianCalendar(LocalDate.now()));
        lenient().when(bruker.getFoedselsdato()).thenReturn(foedselsdato);
    }

    private Postadressetyper tilPostadressetyper(String type) {
        Postadressetyper postadresseType = new Postadressetyper();
        postadresseType.setValue(type);
        return postadresseType;
    }

}
