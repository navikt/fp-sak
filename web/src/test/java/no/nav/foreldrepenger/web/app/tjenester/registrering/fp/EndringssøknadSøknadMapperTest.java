package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.oppdaterDtoForFødsel;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettBruker;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingRevurderingTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.SøknadDataFraTidligereVedtakTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.AnnenPartOversetter;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.SøknadOversetter;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.SøknadWrapper;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapper;

@ExtendWith(MockitoExtension.class)
@ExtendWith(JpaExtension.class)
class EndringssøknadSøknadMapperTest {

    @Mock
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private VirksomhetTjeneste virksomhetTjeneste;
    private final SøknadMapper ytelseSøknadMapper = new EndringssøknadSøknadMapper();
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRevurderingTjeneste behandlingRevurderingTjeneste;
    private BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider;

    @BeforeEach
    public void setUp(EntityManager entityManager) {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var fagsakRelasjonTjeneste = new FagsakRelasjonTjeneste(repositoryProvider);
        behandlingRevurderingTjeneste = new BehandlingRevurderingTjeneste(repositoryProvider, fagsakRelasjonTjeneste);
        grunnlagRepositoryProvider = new BehandlingGrunnlagRepositoryProvider(entityManager);
    }

    @Test
    void skal_treffe_guard_hvis_endringssøknad_sendes_inn_uten_at_det_er_reflektert_i_dokumenttypeid() {
        var navBruker = opprettBruker();
        var manuellRegistreringEndringsøknadDto = new ManuellRegistreringEndringsøknadDto();
        manuellRegistreringEndringsøknadDto.setAnnenForelderInformert(true);
        oppdaterDtoForFødsel(manuellRegistreringEndringsøknadDto, true, LocalDate.now(), 1);
        var soeknad = ytelseSøknadMapper.mapSøknad(manuellRegistreringEndringsøknadDto, navBruker);

        var oppgittPeriodeMottattDatoTjeneste = new SøknadDataFraTidligereVedtakTjeneste(
            new YtelseFordelingTjeneste(repositoryProvider.getYtelsesFordelingRepository()),
            new FpUttakRepository(repositoryProvider.getEntityManager()), repositoryProvider.getBehandlingRepository());
        var oversetter = new SøknadOversetter(repositoryProvider.getFagsakRepository(), behandlingRevurderingTjeneste, grunnlagRepositoryProvider, virksomhetTjeneste, iayTjeneste, personinfoAdapter,
                oppgittPeriodeMottattDatoTjeneste, new AnnenPartOversetter(personinfoAdapter));

        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker);
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var mottattDokumentBuilder = new MottattDokument.Builder().medDokumentType(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL)
            .medMottattDato(LocalDate.now())
            .medFagsakId(fagsak.getId())
            .medElektroniskRegistrert(true);

        var wrapper = (SøknadWrapper) SøknadWrapper.tilXmlWrapper(soeknad);
        var mottattDokument = mottattDokumentBuilder.build();
        Optional<LocalDate> gjelderFra = Optional.empty();
        assertThrows(IllegalArgumentException.class, () -> oversetter.trekkUtDataOgPersister(wrapper, mottattDokument, behandling, gjelderFra));
    }

}
