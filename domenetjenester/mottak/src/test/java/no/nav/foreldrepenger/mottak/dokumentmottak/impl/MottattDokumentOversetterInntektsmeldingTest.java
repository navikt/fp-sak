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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.v1.MottattDokumentOversetterInntektsmelding;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.inntektsmelding.v1.MottattDokumentWrapperInntektsmelding;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.xml.MottattDokumentXmlParser;

public class MottattDokumentOversetterInntektsmeldingTest extends EntityManagerAwareTest {

    private final VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);
    private final FileToStringUtil fileToStringUtil = new FileToStringUtil();
    private BehandlingRepositoryProvider repositoryProvider;

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private MottatteDokumentRepository mottatteDokumentRepository;
    private MottattDokumentOversetterInntektsmelding oversetter;

    @BeforeEach
    public void setUp() {
        when(virksomhetTjeneste.finnOrganisasjon(any())).thenReturn(Optional.of(Virksomhet.getBuilder()
            .medOrgnr(KUNSTIG_ORG)
            .medNavn("Ukjent Firma")
            .medRegistrert(LocalDate.now().minusDays(1))
            .build()));
        iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        var inntektsmeldingTjeneste = new InntektsmeldingTjeneste(iayTjeneste);
        oversetter = new MottattDokumentOversetterInntektsmelding(inntektsmeldingTjeneste, virksomhetTjeneste);
        repositoryProvider = new BehandlingRepositoryProvider(getEntityManager());
        mottatteDokumentRepository = new MottatteDokumentRepository(getEntityManager());
    }

    @Test
    public void mappe_inntektsmelding_til_domene() throws IOException, URISyntaxException {
        final Behandling behandling = opprettScenarioOgLagreInntektsmelding("inntektsmelding.xml");

        final InntektArbeidYtelseGrunnlag grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        assertThat(grunnlag).isNotNull();

        //Hent ut alle endringsrefusjoner fra alle inntektsmeldingene.
        List<Refusjon> endringerIRefusjon = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .map(i -> i.stream().flatMap(im -> im.getEndringerRefusjon().stream()).collect(Collectors.toList()))
            .orElse(Collections.emptyList());

        assertThat(endringerIRefusjon.size()).as(
            "Forventer at vi har en endring i refusjon lagret fra inntektsmeldingen.").isEqualTo(1);
    }

    @Test
    public void skalVedMappingLeseBeløpPerMndForNaturalytelseForGjenopptakelseFraOpphørListe() throws IOException, URISyntaxException {
        final Behandling behandling = opprettScenarioOgLagreInntektsmelding(
            "inntektsmelding_naturalytelse_gjenopptak_ignorer_belop.xml");

        final InntektArbeidYtelseGrunnlag grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        // Hent opp alle naturalytelser
        List<NaturalYtelse> naturalYtelser = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .map(e -> e.stream().flatMap(im -> im.getNaturalYtelser().stream()).collect(Collectors.toList()))
            .orElse(Collections.emptyList());

        assertThat(naturalYtelser.size()).as("Forventet fire naturalytelser, to opphørt og to gjenopptatt.")
            .isEqualTo(4);

        assertThat(naturalYtelser.stream().map(e -> e.getType()).collect(Collectors.toList())).containsOnly(
            AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS, ELEKTRISK_KOMMUNIKASJON);
        assertThat(naturalYtelser.stream().map(e -> e.getBeloepPerMnd())).containsOnly(new Beløp(100));
    }

    @Test
    public void skalMappeOgPersistereKorrektInnsendingsdato() throws IOException, URISyntaxException {
        // Arrange
        final Behandling behandling = opprettBehandling();
        MottattDokument mottattDokument = opprettDokument(behandling, "inntektsmelding.xml");

        final MottattDokumentWrapperInntektsmelding wrapper = (MottattDokumentWrapperInntektsmelding) MottattDokumentXmlParser
            .unmarshallXml(mottattDokument.getPayloadXml());

        // Act
        oversetter.trekkUtDataOgPersister(wrapper, mottattDokument, behandling, Optional.empty());

        // Assert
        final InntektArbeidYtelseGrunnlag grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        Optional<LocalDateTime> innsendingstidspunkt = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .stream()
            .flatMap(e -> e.stream().map(it -> it.getInnsendingstidspunkt()))
            .collect(Collectors.toList())
            .stream()
            .findFirst();

        assertThat(innsendingstidspunkt).isPresent();
        assertThat(innsendingstidspunkt).hasValue(wrapper.getInnsendingstidspunkt().get());

    }

    @Test
    public void skalVedMottakAvNyInntektsmeldingPåSammeArbeidsforholdIkkeOverskriveHvisPersistertErNyereEnnMottatt() throws IOException, URISyntaxException {
        // Arrange
        final Behandling behandling = opprettBehandling();
        MottattDokument mottattDokument = opprettDokument(behandling, "inntektsmelding.xml");
        MottattDokumentWrapperInntektsmelding wrapper = (MottattDokumentWrapperInntektsmelding) MottattDokumentXmlParser
            .unmarshallXml(mottattDokument.getPayloadXml());

        MottattDokumentWrapperInntektsmelding wrapperSpied = Mockito.spy(wrapper);

        LocalDateTime nyereDato = LocalDateTime.now();
        LocalDateTime eldreDato = nyereDato.minusMinutes(1);

        // Act
        // Motta nyere inntektsmelding først
        Mockito.doReturn(Optional.of(nyereDato)).when(wrapperSpied).getInnsendingstidspunkt();
        oversetter.trekkUtDataOgPersister(wrapperSpied, mottattDokument, behandling, Optional.empty());

        // Så motta eldre inntektsmelding
        Mockito.doReturn(Optional.of(eldreDato)).when(wrapperSpied).getInnsendingstidspunkt();
        oversetter.trekkUtDataOgPersister(wrapperSpied, mottattDokument, behandling, Optional.empty());

        // Assert
        final InntektArbeidYtelseGrunnlag grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        Optional<LocalDateTime> innsendingstidspunkt = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .stream()
            .flatMap(e -> e.stream().map(it -> it.getInnsendingstidspunkt()))
            .collect(Collectors.toList())
            .stream()
            .findFirst();

        assertThat(innsendingstidspunkt).isPresent();
        assertThat(innsendingstidspunkt).hasValue(nyereDato);
        assertThat(grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .get()).hasSize(1);

    }

    @Test
    public void skalVedMottakAvNyInntektsmeldingPåSammeArbeidsforholdOverskriveHvisPersistertErEldreEnnMottatt() throws IOException, URISyntaxException {
        // Arrange
        final Behandling behandling = opprettBehandling();
        MottattDokument mottattDokument = opprettDokument(behandling, "inntektsmelding.xml");
        MottattDokumentWrapperInntektsmelding wrapper = (MottattDokumentWrapperInntektsmelding) MottattDokumentXmlParser
            .unmarshallXml(mottattDokument.getPayloadXml());

        MottattDokumentWrapperInntektsmelding wrapperSpied = Mockito.spy(wrapper);

        LocalDateTime nyereDato = LocalDateTime.now();
        LocalDateTime eldreDato = nyereDato.minusMinutes(1);

        // Act
        // Motta eldre inntektsmelding først
        Mockito.doReturn(Optional.of(eldreDato)).when(wrapperSpied).getInnsendingstidspunkt();
        oversetter.trekkUtDataOgPersister(wrapperSpied, mottattDokument, behandling, Optional.empty());

        // Så motta nyere inntektsmelding
        Mockito.doReturn(Optional.of(nyereDato)).when(wrapperSpied).getInnsendingstidspunkt();
        oversetter.trekkUtDataOgPersister(wrapperSpied, mottattDokument, behandling, Optional.empty());

        // Assert
        final InntektArbeidYtelseGrunnlag grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        Optional<LocalDateTime> innsendingstidspunkt = grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .stream()
            .flatMap(e -> e.stream().map(it -> it.getInnsendingstidspunkt()))
            .collect(Collectors.toList())
            .stream()
            .findFirst();

        assertThat(innsendingstidspunkt).isPresent();
        assertThat(innsendingstidspunkt).hasValue(nyereDato);
        assertThat(grunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .get()).hasSize(1);
    }

    private Behandling opprettScenarioOgLagreInntektsmelding(String inntektsmeldingFilnavn) throws URISyntaxException, IOException {
        final Behandling behandling = opprettBehandling();
        MottattDokument mottattDokument = opprettDokument(behandling, inntektsmeldingFilnavn);

        final MottattDokumentWrapperInntektsmelding wrapper = (MottattDokumentWrapperInntektsmelding) MottattDokumentXmlParser
            .unmarshallXml(mottattDokument.getPayloadXml());

        oversetter.trekkUtDataOgPersister(wrapper, mottattDokument, behandling, Optional.empty());
        return behandling;
    }

    private Behandling opprettBehandling() {
        final ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        return scenario.lagre(repositoryProvider);
    }

    private MottattDokument opprettDokument(Behandling behandling,
                                            String inntektsmeldingFilnavn) throws IOException, URISyntaxException {
        final InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(
            behandling.getId());
        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);
        final String xml = fileToStringUtil.readFile(inntektsmeldingFilnavn);
        final MottattDokument.Builder builder = new MottattDokument.Builder();

        MottattDokument mottattDokument = builder.medDokumentType(DokumentTypeId.INNTEKTSMELDING)
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
