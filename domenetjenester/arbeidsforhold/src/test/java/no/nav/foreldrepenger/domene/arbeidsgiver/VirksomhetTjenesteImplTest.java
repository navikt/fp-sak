package no.nav.foreldrepenger.domene.arbeidsgiver;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.tjeneste.virksomhet.organisasjon.v4.binding.HentOrganisasjonUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v4.feil.UgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.OrganisasjonsDetaljer;
import no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.UstrukturertNavn;
import no.nav.tjeneste.virksomhet.organisasjon.v4.meldinger.HentOrganisasjonResponse;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil;
import no.nav.vedtak.felles.integrasjon.organisasjon.OrganisasjonConsumer;

public class VirksomhetTjenesteImplTest {
    private static final String ORGNR = KUNSTIG_ORG;
    private static final String NAVN = "Kunstig virksomhet";
    private static final LocalDate REGISTRERTDATO = LocalDate.of(1978, 01, 01);

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private VirksomhetRepository virksomhetRepository = new VirksomhetRepository();
    private IAYRepositoryProvider repositoryProvider = new IAYRepositoryProvider(repositoryRule.getEntityManager());

    @Test
    public void skal_kalle_consumer_og_oversette_response() throws Exception {
        // Arrange
        IAYScenarioBuilder  scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.ENGANGSTØNAD);
        scenario.lagre(repositoryProvider);

        HentOrganisasjonResponse response = opprettResponse();
        OrganisasjonConsumer organisasjonConsumer = mock(OrganisasjonConsumer.class);
        when(organisasjonConsumer.hentOrganisasjon(any())).thenReturn(response);

        VirksomhetTjeneste organisasjonTjeneste = new VirksomhetTjeneste(organisasjonConsumer, virksomhetRepository);

        // Act
        Virksomhet organisasjon = organisasjonTjeneste.hentOgLagreOrganisasjon(ORGNR);

        // Assert
        assertThat(organisasjon.getOrgnr()).isEqualTo(ORGNR);
        assertThat(organisasjon.getNavn()).isEqualTo(NAVN);
        assertThat(organisasjon.getRegistrert()).isEqualTo(REGISTRERTDATO);

        organisasjon = organisasjonTjeneste.hentOgLagreOrganisasjon(ORGNR);
        // Assert
        assertThat(organisasjon.getOrgnr()).isEqualTo(ORGNR);
        assertThat(organisasjon.getNavn()).isEqualTo(NAVN);
        assertThat(organisasjon.getRegistrert()).isEqualTo(REGISTRERTDATO);
    }

    @Test
    public void skal_håndtere_exceptions_fra_consumer() throws Exception {
        // Arrange
        IAYScenarioBuilder scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.ENGANGSTØNAD);
        scenario.lagre(repositoryProvider);

        HentOrganisasjonResponse response = opprettResponse();
        OrganisasjonConsumer organisasjonConsumer = mock(OrganisasjonConsumer.class);
        when(organisasjonConsumer.hentOrganisasjon(any())).thenReturn(response);

        doThrow(new HentOrganisasjonUgyldigInput("Feil", new UgyldigInput())).when(organisasjonConsumer).hentOrganisasjon(any());

        VirksomhetTjeneste organisasjonTjeneste = new VirksomhetTjeneste(organisasjonConsumer, virksomhetRepository);

        try {
            // Act
            organisasjonTjeneste.hentOgLagreOrganisasjon(ORGNR + "1");
            fail("Forventet VLException");
        } catch (VLException e) {
            // Assert
            assertThat(e.getCause()).isInstanceOf(HentOrganisasjonUgyldigInput.class);
        }
    }

    private HentOrganisasjonResponse opprettResponse() throws Exception {
        HentOrganisasjonResponse response = new HentOrganisasjonResponse();
        no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Organisasjon organisasjon = new no.nav.tjeneste.virksomhet.organisasjon.v4.informasjon.Organisasjon();
        UstrukturertNavn sammensattNavn = new UstrukturertNavn();
        sammensattNavn.getNavnelinje().add(NAVN);
        organisasjon.setNavn(sammensattNavn);
        organisasjon.setOrgnummer(ORGNR);
        OrganisasjonsDetaljer organisasjonsDetaljer = new OrganisasjonsDetaljer();
        organisasjonsDetaljer.setOrgnummer(ORGNR);
        organisasjonsDetaljer.setRegistreringsDato(DateUtil.convertToXMLGregorianCalendar(REGISTRERTDATO));
        organisasjon.setOrganisasjonDetaljer(organisasjonsDetaljer);

        response.setOrganisasjon(organisasjon);
        return response;
    }
}
