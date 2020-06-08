package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.oppdaterDtoForFødsel;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettBruker;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.KodeverkRepository;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.MottattDokumentOversetterSøknad;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.MottattDokumentWrapperSøknad;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapper;
import no.nav.vedtak.felles.xml.soeknad.v3.Soeknad;

public class EndringssøknadSøknadMapperTest {
    private static final DatatypeFactory DATATYPE_FACTORY;
    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final AktørId STD_KVINNE_AKTØR_ID = AktørId.dummy();

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private InntektArbeidYtelseTjeneste iayTjeneste = Mockito.mock(InntektArbeidYtelseTjeneste.class);
    private BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private TpsTjeneste tpsTjeneste;
    private final VirksomhetTjeneste virksomhetTjeneste = mock(VirksomhetTjeneste.class);
    private DatavarehusTjeneste datavarehusTjeneste = mock(DatavarehusTjeneste.class);
    private SvangerskapspengerRepository svangerskapspengerRepository = new SvangerskapspengerRepository(repositoryRule.getEntityManager());

    private SøknadMapper ytelseSøknadMapper = new EndringssøknadSøknadMapper();

    @Before
    public void setUp() throws Exception {
        tpsTjeneste = mock(TpsTjeneste.class);
        reset(tpsTjeneste);
        final Optional<AktørId> stdKvinneAktørId = Optional.of(STD_KVINNE_AKTØR_ID);
        when(tpsTjeneste.hentAktørForFnr(any())).thenReturn(stdKvinneAktørId);
        final Personinfo.Builder builder = new Personinfo.Builder()
            .medAktørId(STD_KVINNE_AKTØR_ID)
            .medNavBrukerKjønn(NavBrukerKjønn.KVINNE)
            .medNavn("Espen Utvikler")
            .medPersonIdent(PersonIdent.fra("12345678901"))
            .medFødselsdato(LocalDate.now().minusYears(20));
        final Optional<Personinfo> build = Optional.ofNullable(builder.build());
        when(tpsTjeneste.hentBrukerForAktør(any(AktørId.class))).thenReturn(build);
        when(virksomhetTjeneste.finnOrganisasjon(any()))
            .thenReturn(Optional.of(Virksomhet.getBuilder().medOrgnr(KUNSTIG_ORG).medNavn("Ukjent Firma").medRegistrert(LocalDate.now().minusDays(1)).build()));
        when(iayTjeneste.hentGrunnlag(any(Long.class))).thenReturn(Mockito.mock(InntektArbeidYtelseGrunnlag.class));
    }


    @Test(expected = IllegalArgumentException.class)
    public void skal_treffe_guard_hvis_endringssøknad_sendes_inn_uten_at_det_er_reflektert_i_dokumenttypeid() {
        // Arrange
        NavBruker navBruker = opprettBruker();
        ManuellRegistreringEndringsøknadDto manuellRegistreringEndringsøknadDto = new ManuellRegistreringEndringsøknadDto();
        oppdaterDtoForFødsel(manuellRegistreringEndringsøknadDto, FamilieHendelseType.FØDSEL, true, LocalDate.now(), 1);
        Soeknad soeknad = ytelseSøknadMapper.mapSøknad(manuellRegistreringEndringsøknadDto, navBruker);

        MottattDokumentOversetterSøknad oversetter = new MottattDokumentOversetterSøknad(repositoryProvider, Mockito.mock(KodeverkRepository.class), virksomhetTjeneste, iayTjeneste, tpsTjeneste, datavarehusTjeneste, svangerskapspengerRepository);

        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker);
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        MottattDokument.Builder mottattDokumentBuilder = new MottattDokument.Builder()
            .medDokumentType(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL)
            .medMottattDato(LocalDate.now())
            .medFagsakId(fagsak.getId())
            .medElektroniskRegistrert(true);

        // Act + Assert
        oversetter.trekkUtDataOgPersister((MottattDokumentWrapperSøknad) MottattDokumentWrapperSøknad.tilXmlWrapper(soeknad), mottattDokumentBuilder.build(), behandling, Optional.empty());
    }

}
