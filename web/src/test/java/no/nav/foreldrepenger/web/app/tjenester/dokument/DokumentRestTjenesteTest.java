package no.nav.foreldrepenger.web.app.tjenester.dokument;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.aktør.NavPersoninfoBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.fagsak.FagsakBuilder;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dokumentarkiv.ArkivDokument;
import no.nav.foreldrepenger.dokumentarkiv.ArkivJournalPost;
import no.nav.foreldrepenger.dokumentarkiv.DokumentArkivTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.dokument.dto.DokumentDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.felles.testutilities.Whitebox;

@ExtendWith(MockitoExtension.class)
public class DokumentRestTjenesteTest {

    private static final String ORGNR = KUNSTIG_ORG;
    @Mock
    private DokumentArkivTjeneste dokumentArkivTjeneste;
    @Mock
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    @Mock
    private FagsakRepository fagsakRepository;
    @Mock
    private MottatteDokumentRepository mottatteDokumentRepository;
    @Mock
    private DokumentRestTjeneste tjeneste;
    @Mock
    private VirksomhetTjeneste virksomhetTjeneste;
    @Mock
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    public void setUp() throws Exception {
        tjeneste = new DokumentRestTjeneste(dokumentArkivTjeneste, inntektsmeldingTjeneste, fagsakRepository, mottatteDokumentRepository,
                virksomhetTjeneste, behandlingRepository);
    }

    @Test
    public void skal_gi_tom_liste_ved_ikkeeksisterende_saksnummer() throws Exception {
        when(fagsakRepository.hentSakGittSaksnummer(any())).thenReturn(Optional.empty());
        final Collection<DokumentDto> response = tjeneste.hentAlleDokumenterForSak(new SaksnummerDto("123456"));
        assertThat(response).isEmpty();
    }

    @Test
    public void skal_returnere_to_dokument() throws Exception {
        Long fagsakId = 5L;
        Long behandlingId = 150L;
        AktørId aktørId = AktørId.dummy();
        Personinfo personinfo = new NavPersoninfoBuilder().medAktørId(aktørId).medDiskresjonskode("6").medPersonstatusType(PersonstatusType.DØD)
                .build();
        NavBruker navBruker = new NavBrukerBuilder().medPersonInfo(personinfo).build();
        Fagsak fagsak = FagsakBuilder.nyForeldrepengerForMor()
                .medBruker(navBruker)
                .medSaksnummer(new Saksnummer("123456"))
                .build();
        Whitebox.setInternalState(fagsak, "id", fagsakId);
        when(fagsakRepository.hentSakGittSaksnummer(any())).thenReturn(Optional.of(fagsak));

        ArkivDokument søknad = new ArkivDokument();
        søknad.setTittel("Søknad");
        søknad.setDokumentId("456");
        søknad.setDokumentType(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL);
        ArkivJournalPost søknadJP = new ArkivJournalPost();
        søknadJP.setJournalpostId(new JournalpostId("123"));
        søknadJP.setTidspunkt(LocalDateTime.of(LocalDate.now().minusDays(6), LocalTime.of(10, 10)));
        søknadJP.setHovedDokument(søknad);

        ArkivDokument vedlegg = new ArkivDokument();
        søknad.setTittel("vedlegg");
        søknad.setDokumentId("123");
        søknad.setDokumentType(DokumentTypeId.DOKUMENTASJON_AV_TERMIN_ELLER_FØDSEL);
        ArkivJournalPost søknadV = new ArkivJournalPost();
        søknadV.setJournalpostId(new JournalpostId("125"));
        søknadV.setHovedDokument(vedlegg);

        ArkivDokument im = new ArkivDokument();
        im.setTittel("Inntektsmelding");
        im.setDokumentId("789");
        im.setDokumentType(DokumentTypeId.INNTEKTSMELDING);
        ArkivJournalPost imJP = new ArkivJournalPost();
        imJP.setJournalpostId(new JournalpostId("124"));
        imJP.setTidspunkt(LocalDateTime.of(LocalDate.now().minusDays(4), LocalTime.of(10, 10)));
        imJP.setHovedDokument(im);

        when(dokumentArkivTjeneste.hentAlleDokumenterForVisning(any())).thenReturn(List.of(søknadJP, søknadV, imJP));

        MottattDokument mds = new MottattDokument.Builder().medId(1001L).medJournalPostId(new JournalpostId("123"))
                .medDokumentType(DokumentTypeId.SØKNAD_ENGANGSSTØNAD_FØDSEL).medFagsakId(fagsakId).medBehandlingId(behandlingId).build();
        MottattDokument mdim = new MottattDokument.Builder().medId(1002L).medJournalPostId(new JournalpostId("124"))
                .medDokumentType(DokumentTypeId.INNTEKTSMELDING).medFagsakId(fagsakId).medBehandlingId(behandlingId).build();
        when(mottatteDokumentRepository.hentMottatteDokumentMedFagsakId(fagsakId)).thenReturn(List.of(mdim, mds));

        String vnavn = "Sinsen Septik og Snarmat";
        Virksomhet sinsen = new Virksomhet.Builder().medNavn(vnavn).medOrgnr(ORGNR).build();
        Inntektsmelding imelda = InntektsmeldingBuilder.builder().medArbeidsgiver(Arbeidsgiver.virksomhet(ORGNR))
                .medJournalpostId(mdim.getJournalpostId()).medInnsendingstidspunkt(LocalDateTime.now()).build();

        when(inntektsmeldingTjeneste.hentAlleInntektsmeldingerForAngitteBehandlinger(any())).thenReturn(Collections.singletonList(imelda));

        when(virksomhetTjeneste.finnOrganisasjon(any())).thenReturn(Optional.of(sinsen));

        final Collection<DokumentDto> response = tjeneste.hentAlleDokumenterForSak(new SaksnummerDto("123456"));
        assertThat(response).hasSize(3);

        assertThat(response.iterator().next().getTidspunkt()).isNull();
        Optional<DokumentDto> imdto = response.stream().filter(dto -> dto.getGjelderFor() != null).findAny();
        assertThat(imdto).isPresent();
        assertThat(imdto.get().getGjelderFor()).isEqualTo(vnavn);
        assertThat(imdto.get().getBehandlinger()).hasSize(1);
    }

}
