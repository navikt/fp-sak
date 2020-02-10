package no.nav.foreldrepenger.behandlingslager.fagsak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.VLException;

public class FagsakRelasjonRepositoryImplTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider provider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private FagsakRepository fagsakRepository = provider.getFagsakRepository();
    private FagsakRelasjonRepository relasjonRepository = provider.getFagsakRelasjonRepository();


    @Test(expected = VLException.class)
    public void skal_ikke_kunne_kobles_med_seg_selv() {
        final Personinfo personinfo = opprettPerson(AktørId.dummy());
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo));
        fagsakRepository.opprettNy(fagsak);
        relasjonRepository.opprettRelasjon(fagsak, Dekningsgrad._100);
        relasjonRepository.kobleFagsaker(fagsak, fagsak, null);
    }

    @Test(expected = VLException.class)
    public void skal_ikke_kunne_kobles_med_fagsak_med_identisk_aktørid() {
        final Personinfo personinfo = opprettPerson(AktørId.dummy());
        final NavBruker bruker = NavBruker.opprettNy(personinfo);
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, bruker);
        fagsakRepository.opprettNy(fagsak);
        final Fagsak fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, bruker);
        fagsakRepository.opprettNy(fagsak2);

        relasjonRepository.opprettRelasjon(fagsak, Dekningsgrad._100);
        relasjonRepository.kobleFagsaker(fagsak, fagsak2, null);
    }

    @Test(expected = VLException.class)
    public void skal_ikke_kunne_kobles_med_fagsak_med_ulik_ytelse() {
        final Personinfo personinfo = opprettPerson(AktørId.dummy());

        final Personinfo personinfo2 = opprettPerson(AktørId.dummy());
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo));
        fagsakRepository.opprettNy(fagsak);
        final Fagsak fagsak2 = Fagsak.opprettNy(FagsakYtelseType.ENGANGSTØNAD, NavBruker.opprettNy(personinfo2));
        fagsakRepository.opprettNy(fagsak2);

        relasjonRepository.opprettRelasjon(fagsak, Dekningsgrad._100);
        relasjonRepository.kobleFagsaker(fagsak, fagsak2, null);
    }

    @Test
    public void skal_koble_sammen_fagsak_med_lik_ytelse_type_og_ulik_aktør() {
        final Personinfo personinfo = opprettPerson(AktørId.dummy());

        final Personinfo personinfo2 = opprettPerson(AktørId.dummy());

        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo));
        fagsakRepository.opprettNy(fagsak);
        final Fagsak fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo2));
        fagsakRepository.opprettNy(fagsak2);

        relasjonRepository.opprettRelasjon(fagsak, Dekningsgrad._100);
        relasjonRepository.kobleFagsaker(fagsak, fagsak2, null);
        final FagsakRelasjon fagsakRelasjon = relasjonRepository.finnRelasjonFor(fagsak);
        final FagsakRelasjon fagsakRelasjon1 = relasjonRepository.finnRelasjonFor(fagsak2);

        assertThat(fagsakRelasjon).isEqualTo(fagsakRelasjon1);
        assertThat(fagsakRelasjon.getFagsakNrEn()).isEqualTo(fagsak);
        assertThat(fagsakRelasjon.getFagsakNrTo()).hasValueSatisfying(fag -> assertThat(fag).isEqualTo(fagsak2));
    }

    @Test
    public void skal_lage_relasjon_når_mangler() {
        final Personinfo personinfo = opprettPerson(AktørId.dummy());

        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo));
        fagsakRepository.opprettNy(fagsak);
        relasjonRepository.opprettEllerOppdaterRelasjon(fagsak, Optional.empty(), Dekningsgrad._100);
        final FagsakRelasjon fagsakRelasjon = relasjonRepository.finnRelasjonFor(fagsak);

        assertThat(fagsakRelasjon.getFagsakNrEn()).isEqualTo(fagsak);
        assertThat(fagsakRelasjon.getDekningsgrad()).isEqualTo(Dekningsgrad._100);
    }

    @Test
    public void skal_oppdatere_relasjon_når_1gang() {
        final Personinfo personinfo = opprettPerson(AktørId.dummy());

        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo));
        fagsakRepository.opprettNy(fagsak);
        Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();

        relasjonRepository.opprettEllerOppdaterRelasjon(fagsak, Optional.empty(), Dekningsgrad._80);

        FagsakRelasjon fagsakRelasjon = relasjonRepository.finnRelasjonFor(fagsak);
        assertThat(fagsakRelasjon.getFagsakNrEn()).isEqualTo(fagsak);
        assertThat(fagsakRelasjon.getDekningsgrad()).isEqualTo(Dekningsgrad._80);

        Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();

        relasjonRepository.opprettEllerOppdaterRelasjon(fagsak, Optional.of(fagsakRelasjon), Dekningsgrad._100);

        fagsakRelasjon = relasjonRepository.finnRelasjonFor(fagsak);
        assertThat(fagsakRelasjon.getFagsakNrEn()).isEqualTo(fagsak);
        assertThat(fagsakRelasjon.getDekningsgrad()).isEqualTo(Dekningsgrad._100);
    }

    @Test
    public void skal_overstyre_dekningsgrad_eier_av_relasjon(){
        // Arrange
        final Personinfo personinfo1 = opprettPerson(AktørId.dummy());
        final Personinfo personinfo2 = opprettPerson(AktørId.dummy());
        final Fagsak fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo1));
        final Fagsak fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo2));
        fagsakRepository.opprettNy(fagsak1);
        fagsakRepository.opprettNy(fagsak2);
        relasjonRepository.opprettRelasjon(fagsak1, Dekningsgrad._80);
        relasjonRepository.kobleFagsaker(fagsak1, fagsak2, null);
        // Act
        relasjonRepository.overstyrDekningsgrad(fagsak1, Dekningsgrad._100);
        final FagsakRelasjon fagsakRelasjon1 = relasjonRepository.finnRelasjonFor(fagsak1);
        final FagsakRelasjon fagsakRelasjon2 = relasjonRepository.finnRelasjonFor(fagsak2);
        // Assert
        assertThat(fagsakRelasjon1).isEqualTo(fagsakRelasjon2);
        assertThat(fagsakRelasjon1.getFagsakNrEn()).isEqualTo(fagsak1);
        assertThat(fagsakRelasjon1.getDekningsgrad().getVerdi()).isEqualTo(80);
        assertThat(fagsakRelasjon1.getOverstyrtDekningsgrad().get().getVerdi()).isEqualTo(100);
        assertThat(fagsakRelasjon1.getFagsakNrTo()).hasValueSatisfying(fagsakOpt -> assertThat(fagsakOpt).isEqualTo(fagsak2));
        assertThat(fagsakRelasjon2.getFagsakNrEn()).isEqualTo(fagsak1);
        assertThat(fagsakRelasjon2.getDekningsgrad().getVerdi()).isEqualTo(80);
        assertThat(fagsakRelasjon1.getOverstyrtDekningsgrad().get().getVerdi()).isEqualTo(100);
        assertThat(fagsakRelasjon2.getFagsakNrTo()).hasValueSatisfying(fagsakOpt -> assertThat(fagsakOpt).isEqualTo(fagsak2));
    }

    @Test
    public void skal_overstyre_dekningsgrad_ikke_eier_av_relasjon(){
        // Arrange
        final Personinfo personinfo1 = opprettPerson(AktørId.dummy());
        final Personinfo personinfo2 = opprettPerson(AktørId.dummy());
        final Fagsak fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo1));
        final Fagsak fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo2));
        fagsakRepository.opprettNy(fagsak1);
        fagsakRepository.opprettNy(fagsak2);
        relasjonRepository.opprettRelasjon(fagsak1, Dekningsgrad._80);
        relasjonRepository.kobleFagsaker(fagsak1, fagsak2, null);
        // Act
        relasjonRepository.overstyrDekningsgrad(fagsak2, Dekningsgrad._100);
        final FagsakRelasjon fagsakRelasjon1 = relasjonRepository.finnRelasjonFor(fagsak1);
        final FagsakRelasjon fagsakRelasjon2 = relasjonRepository.finnRelasjonFor(fagsak2);
        // Assert
        assertThat(fagsakRelasjon1).isEqualTo(fagsakRelasjon2);
        assertThat(fagsakRelasjon1.getFagsakNrEn()).isEqualTo(fagsak1);
        assertThat(fagsakRelasjon1.getDekningsgrad().getVerdi()).isEqualTo(80);
        assertThat(fagsakRelasjon1.getOverstyrtDekningsgrad().get().getVerdi()).isEqualTo(100);
        assertThat(fagsakRelasjon1.getFagsakNrTo()).hasValueSatisfying(fagsakOpt -> assertThat(fagsakOpt).isEqualTo(fagsak2));
        assertThat(fagsakRelasjon2.getFagsakNrEn()).isEqualTo(fagsak1);
        assertThat(fagsakRelasjon2.getDekningsgrad().getVerdi()).isEqualTo(80);
        assertThat(fagsakRelasjon1.getOverstyrtDekningsgrad().get().getVerdi()).isEqualTo(100);
        assertThat(fagsakRelasjon2.getFagsakNrTo()).hasValueSatisfying(fagsakOpt -> assertThat(fagsakOpt).isEqualTo(fagsak2));
    }

    @Test
    public void skal_finne_relasjon_med_saksnummer(){
        // Arrange
        final Personinfo personinfo = opprettPerson(AktørId.dummy());
        Saksnummer saksnummer = new Saksnummer("1337");
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo), RelasjonsRolleType.MORA, saksnummer);
        fagsakRepository.opprettNy(fagsak);
        relasjonRepository.opprettRelasjon(fagsak, Dekningsgrad._80);

        // Act
        final FagsakRelasjon fagsakRelasjon = relasjonRepository.finnRelasjonFor(saksnummer);

        // Assert
        assertThat(fagsakRelasjon.getDekningsgrad().getVerdi()).isEqualTo(80);
        assertThat(fagsakRelasjon.getFagsakNrEn()).isEqualTo(fagsak);
    }

    private Personinfo opprettPerson(AktørId aktørId) {
        return new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(aktørId)
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12345678901"))
            .medForetrukketSpråk(Språkkode.nb)
            .build();
    }

    @Test
    public void skal_oppdatere_med_avslutningsdato(){
        // Arrange
        final Personinfo personinfo1 = opprettPerson(AktørId.dummy());
        final Personinfo personinfo2 = opprettPerson(AktørId.dummy());
        final Fagsak fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo1));
        final Fagsak fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo2));
        fagsakRepository.opprettNy(fagsak1);
        fagsakRepository.opprettNy(fagsak2);
        relasjonRepository.opprettRelasjon(fagsak1, Dekningsgrad._100);
        relasjonRepository.kobleFagsaker(fagsak1, fagsak2, null);
        // Act
        var fagsakRealasjon = relasjonRepository.finnRelasjonFor(fagsak1);
        relasjonRepository.oppdaterMedAvsluttningsdato(fagsakRealasjon, LocalDate.now(), null, Optional.empty(), Optional.empty());
        final FagsakRelasjon fagsakRelasjon1 = relasjonRepository.finnRelasjonFor(fagsak1);
        final FagsakRelasjon fagsakRelasjon2 = relasjonRepository.finnRelasjonFor(fagsak2);
        // Assert
        assertThat(fagsakRelasjon1).isEqualTo(fagsakRelasjon2);
        assertThat(fagsakRelasjon1.getAvsluttningsdato()).isNotNull();
        assertThat(fagsakRelasjon2.getAvsluttningsdato()).isNotNull();
        assertThat(fagsakRelasjon1.getAvsluttningsdato()).isEqualTo(LocalDate.now());
        assertThat(fagsakRelasjon2.getAvsluttningsdato()).isEqualTo(LocalDate.now());
    }

    @Test
    public void skal_hente_relasjoner_med_avsluttningsdato_i_dag_eller_tidligere(){
        // Arrange
        final Personinfo personinfo1 = opprettPerson(AktørId.dummy());
        final Personinfo personinfo2 = opprettPerson(AktørId.dummy());
        final Personinfo personinfo3 = opprettPerson(AktørId.dummy());
        final Personinfo personinfo4 = opprettPerson(AktørId.dummy());
        final Personinfo personinfo5 = opprettPerson(AktørId.dummy());
        final Personinfo personinfo6 = opprettPerson(AktørId.dummy());
        final Fagsak fagsak1 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo1));
        final Fagsak fagsak2 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo2));
        final Fagsak fagsak3 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo3));
        final Fagsak fagsak4 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo4));
        final Fagsak fagsak5 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo5));
        final Fagsak fagsak6 = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(personinfo6));
        fagsakRepository.opprettNy(fagsak1);
        fagsakRepository.opprettNy(fagsak2);
        fagsakRepository.opprettNy(fagsak3);
        fagsakRepository.opprettNy(fagsak4);
        fagsakRepository.opprettNy(fagsak5);
        fagsakRepository.opprettNy(fagsak6);
        relasjonRepository.opprettRelasjon(fagsak1, Dekningsgrad._100);
        relasjonRepository.opprettRelasjon(fagsak3, Dekningsgrad._100);
        relasjonRepository.opprettRelasjon(fagsak5, Dekningsgrad._100);
        relasjonRepository.kobleFagsaker(fagsak1, fagsak2, null);
        relasjonRepository.kobleFagsaker(fagsak3, fagsak4, null);
        relasjonRepository.kobleFagsaker(fagsak5, fagsak6, null);
        relasjonRepository.oppdaterMedAvsluttningsdato(relasjonRepository.finnRelasjonFor(fagsak1), LocalDate.now(), null, Optional.empty(), Optional.empty());
        relasjonRepository.oppdaterMedAvsluttningsdato(relasjonRepository.finnRelasjonFor(fagsak3), LocalDate.now().minusDays(5), null, Optional.empty(), Optional.empty());
        relasjonRepository.oppdaterMedAvsluttningsdato(relasjonRepository.finnRelasjonFor(fagsak5), LocalDate.now().plusDays(5), null, Optional.empty(), Optional.empty());
        // Act
        List<FagsakRelasjon> fagsakRelasjoner = relasjonRepository.finnRelasjonerForAvsluttningAvFagsaker(null,0);
        // Assert
        assertThat(fagsakRelasjoner).hasSize(1);
        assertThat(fagsakRelasjoner.get(0).getAvsluttningsdato()).isBeforeOrEqualTo(LocalDate.now());
    }
}
