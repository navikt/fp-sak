package no.nav.foreldrepenger.domene.arbeidsgiver;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.JpaExtension;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling.IAYScenarioBuilder;
import no.nav.vedtak.felles.integrasjon.organisasjon.OrgInfo;

@ExtendWith(JpaExtension.class)
class VirksomhetTjenesteTest {
    private static final String ORGNR = KUNSTIG_ORG;
    private static final String NAVN = "Kunstig virksomhet";
    private static final LocalDate REGISTRERTDATO = LocalDate.of(1978, 1, 1);

    @Test
    void skal_kalle_consumer_og_oversette_response(EntityManager entityManager) {
        // Arrange
        var scenario = IAYScenarioBuilder.morSøker(FagsakYtelseType.ENGANGSTØNAD);
        scenario.lagre(new IAYRepositoryProvider(entityManager));

        var organisasjonConsumer = mock(OrgInfo.class);

        var organisasjonTjeneste = new VirksomhetTjeneste(organisasjonConsumer);

        // Act
        var organisasjon = organisasjonTjeneste.hentOrganisasjon(ORGNR);

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
