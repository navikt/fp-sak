package no.nav.foreldrepenger.mottak.hendelser.impl.oversetter;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.familiehendelse.dødsfall.DødfødselForretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.JsonMapper;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.DødfødselHendelse;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.ForretningshendelseDto;
import no.nav.foreldrepenger.mottak.hendelser.oversetter.DødfødselForretningshendelseOversetter;

public class DødfødselForretningshendelseOversetterTest {
    private static final List<String> AKTØR_ID_LISTE = singletonList(AktørId.dummy().getId());
    private static final LocalDate DØDFØDSELDATO = LocalDate.now();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private DødfødselForretningshendelseOversetter dødfødselForretningshendelseOversetter;

    @Before
    public void before() {
        dødfødselForretningshendelseOversetter = new DødfødselForretningshendelseOversetter();
    }

    @Test
    public void skal_oversette_fra_json_til_DødfødselForretningshendelse() {
        // Arrange
        DødfødselHendelse dødfødselHendelse = new DødfødselHendelse(AKTØR_ID_LISTE, DØDFØDSELDATO);
        String json = JsonMapper.toJson(dødfødselHendelse);
        ForretningshendelseDto forretningshendelse = new ForretningshendelseDto("DØDFØDSEL", json);

        // Act
        DødfødselForretningshendelse resultat = dødfødselForretningshendelseOversetter.oversett(forretningshendelse);

        // Assert
        assertThat(resultat.getForretningshendelseType()).isEqualTo(ForretningshendelseType.DØDFØDSEL);
        assertThat(resultat.getAktørIdListe().stream().map(AktørId::getId).collect(Collectors.toList())).isEqualTo(AKTØR_ID_LISTE);
        assertThat(resultat.getDødfødselsdato()).isEqualTo(DØDFØDSELDATO);
    }
}
