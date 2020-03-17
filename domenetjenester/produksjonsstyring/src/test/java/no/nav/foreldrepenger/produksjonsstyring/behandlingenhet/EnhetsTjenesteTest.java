package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import no.nav.foreldrepenger.behandlingslager.aktør.Familierelasjon;
import no.nav.foreldrepenger.behandlingslager.aktør.GeografiskTilknytning;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.FiktiveFnr;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingRequest;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingResponse;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingRestKlient;

public class EnhetsTjenesteTest {


    private static AktørId MOR_AKTØR_ID = AktørId.dummy();
    private static PersonIdent MOR_IDENT = new PersonIdent(new FiktiveFnr().nesteKvinneFnr());
    private static Personinfo MOR_PINFO;

    private static AktørId FAR_AKTØR_ID = AktørId.dummy();
    private static PersonIdent FAR_IDENT = new PersonIdent(new FiktiveFnr().nesteMannFnr());
    private static Personinfo FAR_PINFO;

    private static AktørId BARN_AKTØR_ID = AktørId.dummy();
    private static PersonIdent BARN_IDENT = new PersonIdent(new FiktiveFnr().nesteBarnFnr());
    private static Personinfo BARN_PINFO;
    private static LocalDate BARN_FØDT = LocalDate.of(2018,3,3);

    private static final Set<AktørId> FAMILIE = Set.of(MOR_AKTØR_ID, FAR_AKTØR_ID, BARN_AKTØR_ID);

    private static Familierelasjon relasjontilBarn = new Familierelasjon(BARN_IDENT, RelasjonsRolleType.BARN, BARN_FØDT, "Vei", true);
    private static Familierelasjon relasjonEkteFar = new Familierelasjon(FAR_IDENT, RelasjonsRolleType.EKTE, LocalDate.of(1991,11,11), "Vei", true);

    private static OrganisasjonsEnhet enhetNormal = new OrganisasjonsEnhet("4802", "NAV Bærum");
    private static OrganisasjonsEnhet enhetKode6 = new OrganisasjonsEnhet("2103", "NAV Viken");
    private static ArbeidsfordelingResponse respNormal = new ArbeidsfordelingResponse("4802", "NAV Bærum", "Aktiv", "FPY");
    private static ArbeidsfordelingResponse respKode6 = new ArbeidsfordelingResponse("2103", "NAV Viken", "Aktiv", "KO");

    private static GeografiskTilknytning tilknytningNormal = new GeografiskTilknytning("0219", null);
    private static GeografiskTilknytning tilknytningKode6 = new GeografiskTilknytning("0219", "SPSF");
    private static GeografiskTilknytning relatertKode6 = new GeografiskTilknytning(null, "SPSF");

    private TpsTjeneste tpsTjeneste;
    private ArbeidsfordelingRestKlient arbeidsfordelingTjeneste;
    private EnhetsTjeneste enhetsTjeneste;


    @Before
    public void oppsett() {
        tpsTjeneste = mock(TpsTjeneste.class);
        arbeidsfordelingTjeneste = mock(ArbeidsfordelingRestKlient.class);
        enhetsTjeneste = new EnhetsTjeneste(tpsTjeneste, arbeidsfordelingTjeneste);
    }

    @Test
    public void finn_enhet_utvidet_normal_fordeling() {
        // Oppsett
        settOppTpsStrukturer(false, false, false, true);

        OrganisasjonsEnhet enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID, BehandlingTema.ENGANGSSTØNAD);

        assertThat(enhet).isNotNull();
        assertThat(enhet).isEqualTo(enhetNormal);
    }

    @Test
    public void finn_enhet_utvidet_bruker_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(true, false, false, true);

        OrganisasjonsEnhet enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID, BehandlingTema.ENGANGSSTØNAD);

        assertThat(enhet).isNotNull();
        assertThat(enhet).isEqualTo(enhetKode6);
    }

    @Test
    public void finn_enhet_utvidet_barn_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(false, true, false, true);

        OrganisasjonsEnhet enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID, BehandlingTema.ENGANGSSTØNAD);
        OrganisasjonsEnhet enhet1 = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhet.getEnhetId(), BehandlingTema.ENGANGSSTØNAD, MOR_AKTØR_ID, FAMILIE).orElse(enhet);

        assertThat(enhet).isNotNull();
        assertThat(enhet1).isEqualTo(enhetKode6);
    }

    @Test
    public void finn_enhet_utvidet_ektefelle_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(false, false, true, true);

        OrganisasjonsEnhet enhet = enhetsTjeneste.hentEnhetSjekkKunAktør(MOR_AKTØR_ID, BehandlingTema.ENGANGSSTØNAD);
        OrganisasjonsEnhet enhet1 = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhet.getEnhetId(), BehandlingTema.ENGANGSSTØNAD, MOR_AKTØR_ID, FAMILIE).orElse(enhet);

        assertThat(enhet).isNotNull();
        assertThat(enhet1).isEqualTo(enhetKode6);
    }

    @Test
    public void oppdater_enhet_annenpart_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(false, false, true, false);

        Optional<OrganisasjonsEnhet> enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetNormal.getEnhetId(), BehandlingTema.ENGANGSSTØNAD, MOR_AKTØR_ID, FAMILIE);

        assertThat(enhet).isPresent();
        assertThat(enhet).hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetKode6));
    }

    @Test
    public void presendens_enhet() {
        // Oppsett
        settOppTpsStrukturer(false, false, false, false);

        OrganisasjonsEnhet enhet = enhetsTjeneste.enhetsPresedens(enhetNormal, enhetKode6);

        assertThat(enhet).isEqualTo(enhetKode6);
    }

    @Test
    public void oppdater_enhet_mor_annenpart_kode_fordeling() {
        // Oppsett
        settOppTpsStrukturer(true, false, true, false);

        Optional<OrganisasjonsEnhet> enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetKode6.getEnhetId(), BehandlingTema.ENGANGSSTØNAD, MOR_AKTØR_ID, FAMILIE);

        assertThat(enhet).isNotPresent();
    }

    @Test
    public void oppdater_etter_vent_barn_fått_kode6() {
        // Oppsett
        settOppTpsStrukturer(false, true, false, true);

        Optional<OrganisasjonsEnhet> enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetNormal.getEnhetId(), BehandlingTema.ENGANGSSTØNAD, MOR_AKTØR_ID, FAMILIE);

        assertThat(enhet).isPresent();
        assertThat(enhet).hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetKode6));
    }

    @Test
    public void oppdater_etter_vent_far_fått_kode6() {
        // Oppsett
        settOppTpsStrukturer(false, false, true, false);

        Optional<OrganisasjonsEnhet> enhet = enhetsTjeneste.oppdaterEnhetSjekkOppgittePersoner(enhetNormal.getEnhetId(), BehandlingTema.ENGANGSSTØNAD, MOR_AKTØR_ID, FAMILIE);

        assertThat(enhet).isPresent();
        assertThat(enhet).hasValueSatisfying(enhetObj -> assertThat(enhetObj).isEqualTo(enhetKode6));
    }

    private void settOppTpsStrukturer(boolean morKode6, boolean barnKode6, boolean annenPartKode6, boolean foreldreRelatertTps) {
        HashSet<Familierelasjon> relasjoner = new HashSet<>(List.of(relasjontilBarn));
        if (foreldreRelatertTps) {
            relasjoner.add(relasjonEkteFar);
        }
        MOR_PINFO = new Personinfo.Builder().medAktørId(MOR_AKTØR_ID).medPersonIdent(MOR_IDENT).medNavn("Kari Dunk")
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE).medFødselsdato(LocalDate.of(1989,12,12)).medAdresse("Vei")
            .medFamilierelasjon(relasjoner).build();
        FAR_PINFO = new Personinfo.Builder().medAktørId(FAR_AKTØR_ID).medPersonIdent(FAR_IDENT).medNavn("Ola Dunk")
            .medNavBrukerKjønn(NavBrukerKjønn.MANN).medFødselsdato(LocalDate.of(1991,11,11)).medAdresse("Vei").build();
        BARN_PINFO = new Personinfo.Builder().medAktørId(BARN_AKTØR_ID).medPersonIdent(BARN_IDENT).medFødselsdato(BARN_FØDT)
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE).medNavn("Dunk junior d.y.").medAdresse("Vei").build();

        when(tpsTjeneste.hentFnrForAktør(MOR_AKTØR_ID)).thenReturn(MOR_IDENT);
        when(tpsTjeneste.hentFnrForAktør(FAR_AKTØR_ID)).thenReturn(FAR_IDENT);
        when(tpsTjeneste.hentFnrForAktør(BARN_AKTØR_ID)).thenReturn(BARN_IDENT);

        when(tpsTjeneste.hentBrukerForAktør(MOR_AKTØR_ID)).thenReturn(Optional.of(MOR_PINFO));
        when(tpsTjeneste.hentBrukerForAktør(FAR_AKTØR_ID)).thenReturn(Optional.of(FAR_PINFO));
        when(tpsTjeneste.hentBrukerForAktør(BARN_AKTØR_ID)).thenReturn(Optional.of(BARN_PINFO));

        when(tpsTjeneste.hentGeografiskTilknytning(MOR_IDENT)).thenReturn(morKode6 ? tilknytningKode6 : tilknytningNormal);
        when(tpsTjeneste.hentGeografiskTilknytning(FAR_IDENT)).thenReturn(annenPartKode6 ? tilknytningKode6 : tilknytningNormal);
        when(tpsTjeneste.hentGeografiskTilknytning(BARN_IDENT)).thenReturn(barnKode6 ? tilknytningKode6 : tilknytningNormal);

        doAnswer((Answer<List<ArbeidsfordelingResponse>>) invocation -> {
            ArbeidsfordelingRequest data = (ArbeidsfordelingRequest) invocation.getArguments()[0];
            return Objects.equals("SPSF", data.getDiskresjonskode()) ? List.of(respKode6) : List.of(respNormal);
        }).when(arbeidsfordelingTjeneste).finnEnhet(any(ArbeidsfordelingRequest.class));

        when(tpsTjeneste.hentDiskresjonskoderForFamilierelasjoner(MOR_IDENT))
            .thenReturn(barnKode6 || (annenPartKode6 && foreldreRelatertTps) ? Collections.singletonList(relatertKode6): Collections.emptyList());
        when(tpsTjeneste.hentDiskresjonskoderForFamilierelasjoner(FAR_IDENT)).thenReturn(annenPartKode6 ? Collections.singletonList(relatertKode6): Collections.emptyList());
    }
}
