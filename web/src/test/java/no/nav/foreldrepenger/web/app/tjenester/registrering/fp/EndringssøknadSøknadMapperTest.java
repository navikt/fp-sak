package no.nav.foreldrepenger.web.app.tjenester.registrering.fp;

import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.oppdaterDtoForFødsel;
import static no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapperUtil.opprettBruker;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.datavarehus.tjeneste.DatavarehusTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.impl.OppgittPeriodeMottattDatoTjeneste;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.MottattDokumentOversetterSøknad;
import no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3.MottattDokumentWrapperSøknad;
import no.nav.foreldrepenger.web.RepositoryAwareTest;
import no.nav.foreldrepenger.web.app.tjenester.registrering.SøknadMapper;
import no.nav.vedtak.felles.xml.soeknad.v3.Soeknad;

@ExtendWith(MockitoExtension.class)
public class EndringssøknadSøknadMapperTest extends RepositoryAwareTest {

    @Mock
    private InntektArbeidYtelseTjeneste iayTjeneste;
    @Mock
    private PersoninfoAdapter personinfoAdapter;
    @Mock
    private VirksomhetTjeneste virksomhetTjeneste;
    @Mock
    private DatavarehusTjeneste datavarehusTjeneste;
    private OppgittPeriodeMottattDatoTjeneste oppgittPeriodeMottattDatoTjeneste;
    private SøknadMapper ytelseSøknadMapper = new EndringssøknadSøknadMapper();

    @BeforeEach
    public void setUp() {
        oppgittPeriodeMottattDatoTjeneste = new OppgittPeriodeMottattDatoTjeneste(
                new YtelseFordelingTjeneste(ytelsesfordelingRepository));
    }

    @Test
    public void skal_treffe_guard_hvis_endringssøknad_sendes_inn_uten_at_det_er_reflektert_i_dokumenttypeid() {
        NavBruker navBruker = opprettBruker();
        ManuellRegistreringEndringsøknadDto manuellRegistreringEndringsøknadDto = new ManuellRegistreringEndringsøknadDto();
        oppdaterDtoForFødsel(manuellRegistreringEndringsøknadDto, true, LocalDate.now(), 1);
        Soeknad soeknad = ytelseSøknadMapper.mapSøknad(manuellRegistreringEndringsøknadDto, navBruker);

        MottattDokumentOversetterSøknad oversetter = new MottattDokumentOversetterSøknad(repositoryProvider, virksomhetTjeneste,
                iayTjeneste, personinfoAdapter, datavarehusTjeneste, svangerskapspengerRepository, oppgittPeriodeMottattDatoTjeneste);

        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, navBruker);
        Behandling behandling = Behandling.forFørstegangssøknad(fagsak).build();
        fagsakRepository.opprettNy(fagsak);
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        MottattDokument.Builder mottattDokumentBuilder = new MottattDokument.Builder()
                .medDokumentType(DokumentTypeId.SØKNAD_FORELDREPENGER_FØDSEL)
                .medMottattDato(LocalDate.now())
                .medFagsakId(fagsak.getId())
                .medElektroniskRegistrert(true);

        var wrapper = (MottattDokumentWrapperSøknad) MottattDokumentWrapperSøknad.tilXmlWrapper(soeknad);
        assertThrows(IllegalArgumentException.class, () -> {
            oversetter.trekkUtDataOgPersister(wrapper, mottattDokumentBuilder.build(), behandling, Optional.empty());
        });
    }

}
