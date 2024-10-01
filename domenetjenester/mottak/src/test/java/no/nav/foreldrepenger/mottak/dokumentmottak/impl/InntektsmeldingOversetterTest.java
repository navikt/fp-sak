package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS;
import static no.nav.foreldrepenger.domene.iay.modell.kodeverk.NaturalYtelseType.ELEKTRISK_KOMMUNIKASJON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.fpinntektsmelding.FpInntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.v1.InntektsmeldingOversetter;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.v1.InntektsmeldingWrapper;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.xml.MottattDokumentXmlParser;

class InntektsmeldingOversetterTest extends EntityManagerAwareTest {

    private final VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);
    private final FileToStringUtil fileToStringUtil = new FileToStringUtil();
    private BehandlingRepositoryProvider repositoryProvider;

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private InntektsmeldingOversetter oversetter;

    @BeforeEach
    public void setUp() {
        when(virksomhetTjeneste.finnOrganisasjon(any())).thenReturn(Optional.of(Virksomhet.getBuilder()
            .medOrgnr(KUNSTIG_ORG)
            .medNavn("Ukjent Firma")
            .medRegistrert(LocalDate.now().minusDays(1))
            .build()));
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste, new FpInntektsmeldingTjeneste());
        oversetter = new InntektsmeldingOversetter(inntektsmeldingTjeneste, virksomhetTjeneste);
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        mottatteDokumentRepository = new MottatteDokumentRepository(getEntityManager());
    }

    @Test
    void mappe_inntektsmelding_til_domene() throws IOException, URISyntaxException {
        var behandling = opprettScenarioOgLagreInntektsmelding("inntektsmelding.xml");

        var grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        assertThat(grunnlag).isNotNull();

        //Hent ut alle endringsrefusjoner fra alle inntektsmeldingene.
        var endringerIRefusjon = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .map(i -> i.stream().flatMap(im -> im.getEndringerRefusjon().stream()).toList())
            .orElse(Collections.emptyList());

        assertThat(endringerIRefusjon.size()).as(
            "Forventer at vi har en endring i refusjon lagret fra inntektsmeldingen.").isEqualTo(1);
    }

    @Test
    void skalVedMappingLeseBeløpPerMndForNaturalytelseForGjenopptakelseFraOpphørListe() throws IOException, URISyntaxException {
        var behandling = opprettScenarioOgLagreInntektsmelding("inntektsmelding_naturalytelse_gjenopptak_ignorer_belop.xml");

        var grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        // Hent opp alle naturalytelser
        var naturalYtelser = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .map(e -> e.stream().flatMap(im -> im.getNaturalYtelser().stream()).toList())
            .orElse(Collections.emptyList());

        assertThat(naturalYtelser.size()).as("Forventet fire naturalytelser, to opphørt og to gjenopptatt.")
            .isEqualTo(4);

        assertThat(naturalYtelser.stream().map(NaturalYtelse::getType).toList()).containsOnly(
            AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS, ELEKTRISK_KOMMUNIKASJON);
        assertThat(naturalYtelser.stream().map(NaturalYtelse::getBeloepPerMnd)).containsOnly(new Beløp(100));
    }

    @Test
    void skalMappeOgPersistereKorrektInnsendingsdato() throws IOException, URISyntaxException {
        // Arrange
        var behandling = opprettBehandling();
        var mottattDokument = opprettDokument(behandling, "inntektsmelding.xml");

        var wrapper = (InntektsmeldingWrapper) MottattDokumentXmlParser.unmarshallXml(mottattDokument.getPayloadXml());

        // Act
        oversetter.trekkUtDataOgPersister(wrapper, mottattDokument, behandling, Optional.empty());

        // Assert
        var grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        var innsendingstidspunkt = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .stream()
            .flatMap(e -> e.stream().map(Inntektsmelding::getInnsendingstidspunkt))
            .toList()
            .stream()
            .findFirst();

        assertThat(innsendingstidspunkt).isPresent();
        assertThat(innsendingstidspunkt).hasValue(wrapper.getInnsendingstidspunkt().get());

    }

    @Test
    void skalVedMottakAvNyInntektsmeldingPåSammeArbeidsforholdIkkeOverskriveHvisPersistertErNyereEnnMottatt() throws IOException, URISyntaxException {
        // Arrange
        var behandling = opprettBehandling();
        var mottattDokument = opprettDokument(behandling, "inntektsmelding.xml");
        var wrapper = (InntektsmeldingWrapper) MottattDokumentXmlParser
            .unmarshallXml(mottattDokument.getPayloadXml());

        var wrapperSpied = Mockito.spy(wrapper);

        var nyereDato = LocalDateTime.now();
        var eldreDato = nyereDato.minusMinutes(1);

        // Act
        // Motta nyere inntektsmelding først
        Mockito.doReturn(Optional.of(nyereDato)).when(wrapperSpied).getInnsendingstidspunkt();
        oversetter.trekkUtDataOgPersister(wrapperSpied, mottattDokument, behandling, Optional.empty());

        // Så motta eldre inntektsmelding
        Mockito.doReturn(Optional.of(eldreDato)).when(wrapperSpied).getInnsendingstidspunkt();
        oversetter.trekkUtDataOgPersister(wrapperSpied, mottattDokument, behandling, Optional.empty());

        // Assert
        var grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        var innsendingstidspunkt = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .stream()
            .flatMap(e -> e.stream().map(Inntektsmelding::getInnsendingstidspunkt))
            .toList()
            .stream()
            .findFirst();

        assertThat(innsendingstidspunkt).isPresent();
        assertThat(innsendingstidspunkt).hasValue(nyereDato);
        assertThat(grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .get()).hasSize(1);

    }

    @Test
    void skalVedMottakAvNyInntektsmeldingPåSammeArbeidsforholdOverskriveHvisPersistertErEldreEnnMottatt() throws IOException, URISyntaxException {
        // Arrange
        var behandling = opprettBehandling();
        var mottattDokument = opprettDokument(behandling, "inntektsmelding.xml");
        var wrapper = (InntektsmeldingWrapper) MottattDokumentXmlParser
            .unmarshallXml(mottattDokument.getPayloadXml());

        var wrapperSpied = Mockito.spy(wrapper);

        var nyereDato = LocalDateTime.now();
        var eldreDato = nyereDato.minusMinutes(1);

        // Act
        // Motta eldre inntektsmelding først
        Mockito.doReturn(Optional.of(eldreDato)).when(wrapperSpied).getInnsendingstidspunkt();
        oversetter.trekkUtDataOgPersister(wrapperSpied, mottattDokument, behandling, Optional.empty());

        // Så motta nyere inntektsmelding
        Mockito.doReturn(Optional.of(nyereDato)).when(wrapperSpied).getInnsendingstidspunkt();
        oversetter.trekkUtDataOgPersister(wrapperSpied, mottattDokument, behandling, Optional.empty());

        // Assert
        var grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        var innsendingstidspunkt = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .stream()
            .flatMap(e -> e.stream().map(Inntektsmelding::getInnsendingstidspunkt))
            .toList()
            .stream()
            .findFirst();

        assertThat(innsendingstidspunkt).isPresent();
        assertThat(innsendingstidspunkt).hasValue(nyereDato);
        assertThat(grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .get()).hasSize(1);
    }

    private Behandling opprettScenarioOgLagreInntektsmelding(String inntektsmeldingFilnavn) throws URISyntaxException, IOException {
        var behandling = opprettBehandling();
        var mottattDokument = opprettDokument(behandling, inntektsmeldingFilnavn);

        var wrapper = (InntektsmeldingWrapper) MottattDokumentXmlParser.unmarshallXml(mottattDokument.getPayloadXml());

        oversetter.trekkUtDataOgPersister(wrapper, mottattDokument, behandling, Optional.empty());
        return behandling;
    }

    private Behandling opprettBehandling() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        return scenario.lagre(repositoryProvider);
    }

    private MottattDokument opprettDokument(Behandling behandling,
                                            String inntektsmeldingFilnavn) throws IOException, URISyntaxException {
        var inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);
        var xml = fileToStringUtil.readFile(inntektsmeldingFilnavn);
        var builder = new MottattDokument.Builder();

        var mottattDokument = builder.medDokumentType(DokumentTypeId.INNTEKTSMELDING)
            .medFagsakId(behandling.getFagsakId())
            .medMottattDato(LocalDate.now())
            .medBehandlingId(behandling.getId())
            .medElektroniskRegistrert(true)
            .medJournalPostId(new JournalpostId("123123123"))
            .medXmlPayload(xml)
            .build();

        mottatteDokumentRepository.lagre(mottattDokument);
        return mottattDokument;
    }
}
