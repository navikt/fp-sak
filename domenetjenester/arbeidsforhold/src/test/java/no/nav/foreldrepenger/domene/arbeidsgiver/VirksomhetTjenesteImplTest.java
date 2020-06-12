package no.nav.foreldrepenger.domene.arbeidsgiver;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.vedtak.felles.integrasjon.organisasjon.OrganisasjonRestKlient;


public class VirksomhetTjenesteImplTest {
    private static final String ORGNR = KUNSTIG_ORG;
    private static final String NAVN = "Kunstig virksomhet";
    private static final LocalDate REGISTRERTDATO = LocalDate.of(1978, 01, 01);

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private IAYRepositoryProvider repositoryProvider = new IAYRepositoryProvider(repositoryRule.getEntityManager());

    @Test
    public void skal_kalle_consumer_og_oversette_response() throws Exception {
        // Arrange
        IAYScenarioBuilder  scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.ENGANGSTØNAD);
        scenario.lagre(repositoryProvider);

        var organisasjonConsumer = mock(OrganisasjonRestKlient.class);

        VirksomhetTjeneste organisasjonTjeneste = new VirksomhetTjeneste(organisasjonConsumer);

        // Act
        Virksomhet organisasjon = organisasjonTjeneste.hentOrganisasjon(ORGNR);

        // Assert
        assertThat(organisasjon.getOrgnr()).isEqualTo(ORGNR);
        assertThat(organisasjon.getNavn()).isEqualTo(NAVN);
        assertThat(organisasjon.getRegistrert()).isEqualTo(REGISTRERTDATO);

        organisasjon = organisasjonTjeneste.hentOrganisasjon(ORGNR);
        // Assert
        assertThat(organisasjon.getOrgnr()).isEqualTo(ORGNR);
        assertThat(organisasjon.getNavn()).isEqualTo(NAVN);
        assertThat(organisasjon.getRegistrert()).isEqualTo(REGISTRERTDATO);
    }

}
